package com.kaleidoscopedoll.client.custom;

import com.kaleidoscopedoll.KaleidoscopeDoll;
import com.kaleidoscopedoll.data.custom.CustomDollConfigPacks;
import com.kaleidoscopedoll.network.DollPackEncryption;
import com.kaleidoscopedoll.network.DollPackPackets;
import com.google.common.hash.Hashing;

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
import java.util.function.Predicate;

final class ServerDollPackCache {
    static final String SOURCE_PREFIX = "kaleidoscope_doll/server/";
    private static final Path ROOT = CustomDollConfigPacks.ROOT.resolveSibling("servers");

    private final Path directory;

    ServerDollPackCache(String address) {
        this(ROOT, address);
    }

    ServerDollPackCache(Path root, String address) {
        String key = Hashing.sha256().hashString(address.trim().toLowerCase(Locale.ROOT), StandardCharsets.UTF_8).toString();
        directory = root.resolve(key);
    }

    Path path(DollPackPackets.PackInfo info, byte[] key) {
        return directory.resolve(info.sha256() + (key.length == 0 ? ".zip" : ".zip.enc"));
    }

    MemoryDollPack open(DollPackPackets.PackInfo info, byte[] key) throws IOException {
        Path path = path(info, key);
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Missing cached server doll ZIP");
        }
        int size = info.size() + (key.length == 0 ? 0 : DollPackEncryption.OVERHEAD);
        try (var input = Files.newInputStream(path, LinkOption.NOFOLLOW_LINKS)) {
            byte[] data = input.readNBytes(size + 1);
            if (data.length != size) {
                throw new IOException("Incorrect cached server doll ZIP size");
            }
            return decode(info, data, key);
        }
    }

    boolean contains(DollPackPackets.PackInfo info, byte[] key) {
        try (var ignored = open(info, key)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    void store(DollPackPackets.PackInfo info, byte[] bytes, byte[] key) throws IOException {
        try (var ignored = decode(info, bytes, key)) {
            // Validate the decrypted archive before writing the original bytes to disk.
        }
        var path = path(info, key);
        Files.createDirectories(directory);
        Path temporary = Files.createTempFile(directory, "incoming-", ".tmp");
        try {
            Files.write(temporary, bytes);
            try {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private MemoryDollPack decode(DollPackPackets.PackInfo info, byte[] data, byte[] key) throws IOException {
        byte[] zip = key.length == 0 ? data : DollPackEncryption.open(data, key);
        if (!info.equals(DollPackPackets.PackInfo.of(zip))) {
            throw new IOException("Server doll ZIP does not match its advertised hash or size");
        }
        return MemoryDollPack.open(sourceId(path(info, key)), zip);
    }

    void prune(List<Path> keep) {
        removeMatching(path -> path.getFileName().toString().matches("[0-9a-f]{64}\\.zip(?:\\.enc)?") && !keep.contains(path));
    }

    void removePlaintext() {
        removeMatching(path -> path.getFileName().toString().matches("[0-9a-f]{64}\\.zip"));
    }

    private void removeMatching(Predicate<Path> shouldRemove) {
        if (!Files.isDirectory(directory)) {
            return;
        }
        try (var paths = Files.list(directory)) {
            for (Path path : paths.filter(shouldRemove).toList()) {
                if (Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
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
}
