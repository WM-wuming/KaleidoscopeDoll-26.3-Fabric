package com.kaleidoscopedoll.client.custom;

import com.kaleidoscopedoll.KaleidoscopeDoll;
import com.kaleidoscopedoll.data.custom.CustomDollConfigPacks;
import com.kaleidoscopedoll.data.custom.FolderDollPack;
import com.kaleidoscopedoll.data.custom.TransferableDollPack;
import com.kaleidoscopedoll.network.DollPackPackets;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.FilePackResources;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.zip.ZipFile;

public final class CustomDollConfigPackSource implements RepositorySource {
    private static final PackSelectionConfig SELECTION = new PackSelectionConfig(true, Pack.Position.BOTTOM, true);
    private final Path directory;

    public CustomDollConfigPackSource(Path directory) {
        this.directory = directory;
    }

    @Override
    public void loadPacks(Consumer<Pack> output) {
        if (!Files.isDirectory(directory)) {
            return;
        }
        try (var paths = Files.list(directory)) {
            paths.sorted().forEach(path -> {
                if (FolderDollPack.isPack(path)) {
                    loadFolder(path, output);
                } else if (CustomDollConfigPacks.isArchive(path)) {
                    loadPack(path, output);
                }
            });
        } catch (IOException | UncheckedIOException e) {
            KaleidoscopeDoll.LOGGER.error("Failed to list custom doll config packs in {}", directory, e);
        }
    }

    private static void loadFolder(Path path, Consumer<Pack> output) {
        try {
            var bytes = FolderDollPack.read(path).archive(DollPackPackets.MAX_TOTAL_BYTES);
            String id = "kaleidoscope_doll/custom/" + path.getFileName();
            var pack = MemoryDollPack.localConfig(id, bytes);
            if (pack != null) output.accept(pack);
        } catch (IOException | UncheckedIOException e) {
            KaleidoscopeDoll.LOGGER.warn("Could not load custom doll folder {}: {}", path, e.getMessage());
        }
    }

    private static void loadPack(Path path, Consumer<Pack> output) {
        try (ZipFile zip = new ZipFile(path.toFile())) {
            if (!CustomDollConfigPacks.hasJavaModels(zip)) {
                return;
            }
            TransferableDollPack.validate(zip);
        } catch (IOException e) {
            KaleidoscopeDoll.LOGGER.error("Failed to read custom doll config pack {}", path, e);
            return;
        }

        String name = path.getFileName().toString();
        var info = new PackLocationInfo("kaleidoscope_doll/custom/" + name, Component.literal(name),
                PackSource.BUILT_IN, Optional.empty());
        Pack pack = Pack.readMetaAndCreate(info, new FilePackResources.FileResourcesSupplier(path), PackType.CLIENT_RESOURCES, SELECTION);
        if (pack != null && pack.getCompatibility().isCompatible()) {
            output.accept(pack);
        } else {
            KaleidoscopeDoll.LOGGER.warn("Incompatible custom doll config pack: {}", path);
        }
    }
}
