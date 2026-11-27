package com.github.ysbbbbbb.kaleidoscopedoll.client.custom;

import com.github.ysbbbbbb.kaleidoscopedoll.KaleidoscopeDoll;
import com.github.ysbbbbbb.kaleidoscopedoll.data.custom.CustomDollConfigPacks;
import com.github.ysbbbbbb.kaleidoscopedoll.data.custom.TransferableDollPack;
import com.github.ysbbbbbb.kaleidoscopedoll.network.DollPackPackets;
import com.google.common.hash.Hashing;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.FilePackResources;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.zip.ZipFile;

final class ServerDollPackCache {
    static final String SOURCE_PREFIX = "kaleidoscope_doll/server/";
    private static final Path ROOT = CustomDollConfigPacks.ROOT.resolveSibling("servers");
    private static final PackSelectionConfig SELECTION = new PackSelectionConfig(false, Pack.Position.BOTTOM, false);

    private final Path directory;

    ServerDollPackCache(String address) {
        String key = Hashing.sha256().hashString(address.trim().toLowerCase(Locale.ROOT), StandardCharsets.UTF_8).toString();
        directory = ROOT.resolve(key);
    }

    Path path(DollPackPackets.PackInfo info) {
        return directory.resolve(info.sha256() + ".zip");
    }

    boolean contains(DollPackPackets.PackInfo info) {
        Path path = path(info);
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            return false;
        }
        try (var input = Files.newInputStream(path, LinkOption.NOFOLLOW_LINKS)) {
            if (!info.equals(DollPackPackets.PackInfo.of(input.readNBytes(info.size() + 1)))) {
                return false;
            }
            validate(path);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    void store(DollPackPackets.PackInfo info, byte[] bytes) throws IOException {
        if (!info.equals(DollPackPackets.PackInfo.of(bytes))) {
            throw new IOException("Server doll ZIP does not match its advertised hash or size");
        }
        Files.createDirectories(directory);
        Path temporary = Files.createTempFile(directory, "incoming-", ".tmp");
        try {
            Files.write(temporary, bytes);
            validate(temporary);
            try {
                Files.move(temporary, path(info), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, path(info), StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    void prune(List<Path> keep) {
        if (!Files.isDirectory(directory)) {
            return;
        }
        try (var paths = Files.list(directory)) {
            for (Path path : paths.filter(p -> p.getFileName().toString().matches("[0-9a-f]{64}\\.zip")).toList()) {
                if (!keep.contains(path) && Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
                    Files.deleteIfExists(path);
                }
            }
        } catch (IOException | UncheckedIOException e) {
            KaleidoscopeDoll.LOGGER.debug("Could not prune server doll config cache", e);
        }
    }

    static String sourceId(Path path) {
        return SOURCE_PREFIX + path.getParent().getFileName() + "/" + path.getFileName();
    }

    static Pack openConfig(Path path) {
        String id = sourceId(path);
        var location = new PackLocationInfo(id, Component.literal(id), PackSource.BUILT_IN, Optional.empty());
        return Pack.readMetaAndCreate(location, new FilePackResources.FileResourcesSupplier(path), PackType.CLIENT_RESOURCES, SELECTION);
    }

    private static void validate(Path path) throws IOException {
        try (var zip = new ZipFile(path.toFile())) {
            TransferableDollPack.validate(zip);
        }
        Pack pack = openConfig(path);
        if (pack == null || !pack.getCompatibility().isCompatible()) {
            throw new IOException("Incompatible server doll config format");
        }
    }
}
