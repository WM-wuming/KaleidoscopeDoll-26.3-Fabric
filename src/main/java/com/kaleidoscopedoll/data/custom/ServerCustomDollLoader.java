package com.kaleidoscopedoll.data.custom;

import com.kaleidoscopedoll.KaleidoscopeDoll;
import com.kaleidoscopedoll.network.ServerDollPackSync;
import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipFile;

public class ServerCustomDollLoader {
    private static final Path ROOT = CustomDollConfigPacks.ROOT;
    private static final String DIRECTORY_MODELS_NAME = "models";
    /**
     * zip 文件有两种情况，一直是直接下面就是 models 和 lang 文件夹
     * 另一种是有一个顶层文件夹，下面才是 models 和 lang 文件夹
     * 我们需要同时考虑这两种情况
     */
    private static final Pattern ZIP_MODELS_PATTERN = Pattern.compile("^(?:[^/]+/)?models/[^/]+\\.json$");

    private static final String JSON = ".json";

    /**
     * 缓存数据
     */
    private static final Set<String> MODELS = Sets.newLinkedHashSet();
    private static volatile Map<String, FolderDollPack.Drop> drops = Map.of();

    public static void init() throws IOException {
        if (!Files.isDirectory(ROOT)) {
            Files.createDirectories(ROOT);
        }

        MODELS.clear();
        var loadedDrops = new HashMap<String, FolderDollPack.Drop>();

        try (Stream<Path> pathStream = Files.walk(ROOT, 1)) {
            pathStream.filter(p -> !p.equals(ROOT)).sorted().forEach(path -> {
                // 文件夹
                if (Files.isDirectory(path)) {
                    if (FolderDollPack.isPack(path)) {
                        loadFolderPack(path, loadedDrops);
                    } else {
                        loadFromDirectory(path);
                    }
                    return;
                }

                // zip 文件
                if (CustomDollConfigPacks.isArchive(path)) {
                    ServerCustomDollLoader.loadFromZip(path);
                }
            });
        }

        // 最后把内部数据包读取的数据存入
        ServerCustomDollResourceLoader.putAll(MODELS);
        drops = Map.copyOf(loadedDrops);
        ServerDollPackSync.refresh(ROOT);
    }

    private static void loadFolderPack(Path path, Map<String, FolderDollPack.Drop> loadedDrops) {
        try {
            var pack = FolderDollPack.read(path);
            for (String id : pack.models()) {
                MODELS.add(id);
                var drop = pack.drop(id);
                if (drop != null) {
                    loadedDrops.put(id, drop);
                } else {
                    loadedDrops.remove(id);
                }
            }
        } catch (IOException e) {
            KaleidoscopeDoll.LOGGER.warn("Failed to read custom doll folder {}", path, e);
        }
    }

    public static Optional<List<ItemStack>> blockDrops(String modelId, BlockState state, LootParams.Builder params) {
        return rollDrops(modelId, params.getLevel(), () ->
                params.withParameter(LootContextParams.BLOCK_STATE, state).create(LootContextParamSets.BLOCK));
    }

    public static Optional<List<ItemStack>> entityDrops(String modelId, ServerLevel level, Entity entity, DamageSource source) {
        return rollDrops(modelId, level, () -> {
            var builder = new LootParams.Builder(level)
                    .withParameter(LootContextParams.THIS_ENTITY, entity)
                    .withParameter(LootContextParams.ORIGIN, entity.position())
                    .withParameter(LootContextParams.DAMAGE_SOURCE, source)
                    .withOptionalParameter(LootContextParams.ATTACKING_ENTITY, source.getEntity())
                    .withOptionalParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, source.getDirectEntity());
            if (source.getEntity() instanceof Player player) {
                builder.withParameter(LootContextParams.LAST_DAMAGE_PLAYER, player).withLuck(player.getLuck());
            }
            return builder.create(LootContextParamSets.ENTITY);
        });
    }

    private static Optional<List<ItemStack>> rollDrops(String modelId, ServerLevel level, Supplier<LootParams> params) {
        if (modelId == null) return Optional.empty();
        var drop = drops.get(modelId);
        if (drop == null) return Optional.empty();
        try {
            return readLootTable(level, drop).map(value -> value.getRandomItems(params.get()));
        } catch (RuntimeException e) {
            KaleidoscopeDoll.LOGGER.warn("Could not roll doll loot table {}: {}", drop.path(), e.getMessage());
            return Optional.empty();
        }
    }

    private static Optional<LootTable> readLootTable(ServerLevel level, FolderDollPack.Drop drop) {
        var parsed = LootTable.DIRECT_CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, level.getServer().reloadableRegistries().lookup()), drop.table());
        parsed.error().ifPresent(error -> KaleidoscopeDoll.LOGGER.warn("Invalid doll loot table {}: {}", drop.path(), error.message()));
        return parsed.result();
    }

    private static void loadFromDirectory(Path directory) {
        Path modelsDir = directory.resolve(DIRECTORY_MODELS_NAME);
        if (Files.isDirectory(modelsDir)) {
            try (Stream<Path> pathStream = Files.walk(modelsDir, 1)) {
                pathStream.filter(p -> !p.equals(modelsDir)).forEach(path -> {
                    if (Files.isRegularFile(path) && path.toString().endsWith(JSON)) {
                        try (InputStream stream = Files.newInputStream(path)) {
                            readBedrockModel(stream, MODELS);
                        } catch (IOException e) {
                            KaleidoscopeDoll.LOGGER.error("Failed to read model file: {}", path, e);
                        }
                    }
                });
            } catch (IOException e) {
                KaleidoscopeDoll.LOGGER.error("Failed to load models from directory: {}", directory, e);
            }
        }
    }

    private static void loadFromZip(Path zipFile) {
        try (ZipFile zip = new ZipFile(zipFile.toFile())) {
            if (CustomDollConfigPacks.hasJavaModels(zip)) {
                TransferableDollPack.validate(zip);
                CustomDollConfigPacks.readModelIds(zip, MODELS);
                return;
            }
            zip.stream().forEach(entry -> {
                String entryName = entry.getName().replace('\\', '/');
                if (entryName.startsWith("/")) {
                    entryName = entryName.substring(1);
                }

                if (ZIP_MODELS_PATTERN.matcher(entryName).matches()) {
                    try (InputStream stream = zip.getInputStream(entry)) {
                        readBedrockModel(stream, MODELS);
                    } catch (IOException e) {
                        KaleidoscopeDoll.LOGGER.error("Failed to read model file from zip: {} in {}", entryName, zipFile, e);
                    }
                }
            });
        } catch (IOException e) {
            KaleidoscopeDoll.LOGGER.error("Failed to load models from zip file: {}", zipFile, e);
        }
    }

    static void readBedrockModel(InputStream stream, Set<String> models) {
        try {
            InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
            JsonObject object = GsonHelper.parse(reader);
            JsonArray array = GsonHelper.getAsJsonArray(object, "minecraft:geometry");
            if (array.isEmpty() || !array.get(0).isJsonObject()) {
                KaleidoscopeDoll.LOGGER.error("Invalid model format: missing or invalid 'minecraft:geometry' array");
                return;
            }
            JsonObject geometry = array.get(0).getAsJsonObject();
            JsonObject description = GsonHelper.getAsJsonObject(geometry, "description");
            String identifier = GsonHelper.getAsString(description, "identifier");
            models.add(identifier);
        } catch (Exception e) {
            KaleidoscopeDoll.LOGGER.error("Failed to read model", e);
        }
    }

    public static Set<String> getModels() {
        return MODELS;
    }
}
