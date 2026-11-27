package com.github.ysbbbbbb.kaleidoscopedoll.client.custom;

import com.github.ysbbbbbb.kaleidoscopedoll.KaleidoscopeDoll;
import com.github.ysbbbbbb.kaleidoscopedoll.client.bedrock.BedrockModel;
import com.github.ysbbbbbb.kaleidoscopedoll.client.custom.CustomDollAssets.Kind;
import com.github.ysbbbbbb.kaleidoscopedoll.data.custom.CustomDollConfigPacks;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.ZipFile;

public class CustomDollLoader {
    private static final Path ROOT = CustomDollConfigPacks.ROOT;
    private static final String RESOURCE_ROOT = "custom_dolls/";
    private static final Kind[] KINDS = Kind.values();

    private static CustomDollAssets resourceAssets = new CustomDollAssets();
    private static CustomDollAssets assets = new CustomDollAssets();

    public static void reloadResources(ResourceManager manager) throws IOException {
        CustomDollAssets loaded = new CustomDollAssets();
        for (Kind kind : KINDS) {
            String prefix = RESOURCE_ROOT + kind.directory;
            manager.listResources(prefix, id -> id.getPath().endsWith(kind.extension)).forEach((id, resource) -> {
                try (var stream = resource.open()) {
                    loaded.read(kind, kind.assetName(id.getPath(), prefix.length() + 1), stream);
                } catch (IOException | RuntimeException e) {
                    KaleidoscopeDoll.LOGGER.error("Failed to load custom doll resource: {}", id, e);
                }
            });
        }
        resourceAssets = loaded;
        init();
    }

    public static void init() throws IOException {
        if (!Files.isDirectory(ROOT)) {
            Files.createDirectories(ROOT);
        }
        CustomDollAssets loaded = new CustomDollAssets();
        try (var paths = Files.list(ROOT)) {
            paths.forEach(path -> {
                if (Files.isDirectory(path)) {
                    loadFromDirectory(loaded, path);
                } else if (CustomDollConfigPacks.isArchive(path)) {
                    loadFromZip(loaded, path);
                }
            });
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }

        loaded.putAll(resourceAssets);
        assets = loaded;
    }

    private static void loadFromDirectory(CustomDollAssets target, Path directory) {
        for (Kind kind : KINDS) {
            Path assetDirectory = directory.resolve(kind.directory);
            if (!Files.isDirectory(assetDirectory)) {
                continue;
            }
            try (var paths = Files.list(assetDirectory)) {
                paths.forEach(path -> {
                    String fileName = path.getFileName().toString();
                    if (!fileName.endsWith(kind.extension) || !Files.isRegularFile(path)) {
                        return;
                    }
                    try (var stream = Files.newInputStream(path)) {
                        target.read(kind, kind.assetName(fileName, 0), stream);
                    } catch (IOException | RuntimeException e) {
                        KaleidoscopeDoll.LOGGER.error("Failed to load custom doll file: {}", path, e);
                    }
                });
            } catch (IOException | UncheckedIOException e) {
                KaleidoscopeDoll.LOGGER.error("Failed to list custom doll directory: {}", assetDirectory, e);
            }
        }
    }

    private static void loadFromZip(CustomDollAssets target, Path file) {
        try (ZipFile zip = new ZipFile(file.toFile())) {
            if (CustomDollConfigPacks.hasManifest(zip)) {
                return;
            }
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }
                String path = entry.getName().replace('\\', '/');
                if (path.startsWith("/")) {
                    path = path.substring(1);
                }
                for (Kind kind : KINDS) {
                    if (!path.endsWith(kind.extension) || !kind.zipPath.matcher(path).matches()) {
                        continue;
                    }
                    try (var stream = zip.getInputStream(entry)) {
                        target.read(kind, kind.assetName(path, path.lastIndexOf('/') + 1), stream);
                    } catch (IOException | RuntimeException e) {
                        KaleidoscopeDoll.LOGGER.error("Failed to load custom doll entry {} from {}", path, file, e);
                    }
                    break;
                }
            }
        } catch (IOException e) {
            KaleidoscopeDoll.LOGGER.error("Failed to load custom dolls from zip: {}", file, e);
        }
    }

    public static @Nullable BedrockModel getModel(String id) {
        return assets.models.get(id);
    }

    public static @Nullable Identifier getTexture(String name) {
        return assets.textures.get(name);
    }

    public static String getLanguage(String locale, String key) {
        Map<String, String> translations = assets.languages.get(locale);
        if (translations != null) {
            String translation = translations.get(key);
            if (translation != null) {
                return translation;
            }
        }
        Map<String, String> english = assets.languages.get("en_us");
        return english == null ? key : english.getOrDefault(key, key);
    }
}
