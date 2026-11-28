package com.kaleidoscopedoll.data.custom;

import com.kaleidoscopedoll.KaleidoscopeDoll;
import com.google.gson.JsonParseException;
import net.minecraft.util.GsonHelper;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class TransferableDollPack {
    private static final int MAX_ENTRIES = 8192;
    private static final long MAX_UNCOMPRESSED_BYTES = 64L * 1024 * 1024;
    private static final int MAX_JSON_BYTES = 2 * 1024 * 1024;
    private static final long MAX_TEXTURE_PIXELS = 16L * 1024 * 1024;
    private static final Pattern ASSET = Pattern.compile(
            "assets/([a-z0-9_.-]+)/(?:models/.+\\.json|textures/.+\\.png(?:\\.mcmeta)?|custom_dolls/lang/[a-z0-9_]+\\.json)");
    private static final Set<String> ROOT_FILES = Set.of("pack.mcmeta", "pack.png");

    private TransferableDollPack() {
    }

    public static void validate(ZipFile zip) throws IOException {
        validate(zip.entries(), zip::getEntry, zip::getInputStream);
    }

    public static void validate(byte[] bytes) throws IOException {
        var staging = Files.createTempFile("kaleidoscope-doll-", ".zip");
        try {
            Files.write(staging, bytes);
            try (var zip = new ZipFile(staging.toFile())) {
                validate(zip);
            }
        } finally {
            Files.deleteIfExists(staging);
        }
    }

    public static <E extends ZipEntry> void validate(Enumeration<? extends E> entries,
                                                      Function<String, E> find, EntryInput<E> input) throws IOException {
        long remaining = MAX_UNCOMPRESSED_BYTES;
        long remainingPixels = MAX_TEXTURE_PIXELS;
        byte[] buffer = new byte[8192];
        Set<String> seen = new HashSet<>();
        boolean hasModels = false;
        while (entries.hasMoreElements()) {
            var entry = entries.nextElement();
            String name = entry.getName();
            if (!seen.add(name) || !name.matches("[a-z0-9_./-]+") || name.startsWith("/")
                    || name.contains("//") || Set.of(".", "..").stream().anyMatch(part -> name.equals(part)
                    || name.startsWith(part + "/") || name.contains("/" + part + "/") || name.endsWith("/" + part))) {
                throw new IOException("Invalid ZIP entry: " + name);
            }
            if (seen.size() > MAX_ENTRIES) {
                throw new IOException("Too many ZIP entries");
            }
            long size = entry.getSize();
            if (size < 0 || size > remaining) {
                throw new IOException("Doll ZIP exceeds its uncompressed size limit");
            }
            remaining -= size;
            if (entry.isDirectory()) {
                continue;
            }
            var asset = ASSET.matcher(name);
            if (!ROOT_FILES.contains(name)
                    && (!asset.matches() || Set.of("minecraft", KaleidoscopeDoll.MOD_ID).contains(asset.group(1)))) {
                throw new IOException("Non-doll resource in transferred ZIP: " + name);
            }
            hasModels |= CustomDollConfigPacks.modelIdFromAsset(name) != null;
            if (!name.endsWith(".png") && size > MAX_JSON_BYTES) {
                throw new IOException("Oversized doll JSON: " + name);
            }
            if (entry.getMethod() != ZipEntry.STORED && entry.getMethod() != ZipEntry.DEFLATED) {
                throw new IOException("Unsupported compression for doll ZIP entry: " + name);
            }
            remainingPixels -= checkEntry(input.open(entry), entry, buffer);
            if (remainingPixels < 0) {
                throw new IOException("Doll ZIP exceeds its texture pixel limit");
            }
        }

        var metadata = find.apply("pack.mcmeta");
        if (metadata == null || metadata.getSize() > 16 * 1024) {
            throw new IOException("Missing or oversized pack metadata");
        }
        try (var reader = new InputStreamReader(input.open(metadata), StandardCharsets.UTF_8)) {
            var fields = GsonHelper.parse(reader).keySet();
            if (!fields.equals(Set.of("pack"))) {
                throw new IOException("Transferred packs may not define filters or overlays");
            }
        } catch (JsonParseException e) {
            throw new IOException("Invalid pack metadata", e);
        }
        if (!hasModels) {
            throw new IOException("No Java doll models found");
        }
    }

    private static long checkEntry(InputStream stream, ZipEntry entry, byte[] buffer) throws IOException {
        try (stream) {
            long pixels = 0;
            long expanded = 0;
            if (entry.getName().endsWith(".png")) {
                expanded = stream.readNBytes(buffer, 0, 24);
                var header = ByteBuffer.wrap(buffer, 0, 24);
                if (expanded != 24 || header.getLong() != 0x89504e470d0a1a0aL
                        || header.getInt() != 13 || header.getInt() != 0x49484452) {
                    throw new IOException("Invalid PNG header: " + entry.getName());
                }
                int width = header.getInt();
                int height = header.getInt();
                if (width < 1 || height < 1 || width > 4096 || height > 4096) {
                    throw new IOException("Unsupported doll texture size: " + entry.getName());
                }
                pixels = (long) width * height;
            }
            int count;
            while ((count = stream.read(buffer)) != -1) {
                expanded += count;
                if (expanded > entry.getSize()) {
                    throw new IOException("Doll ZIP entry exceeds its declared size: " + entry.getName());
                }
            }
            if (expanded != entry.getSize()) {
                throw new IOException("Incorrect doll ZIP entry size: " + entry.getName());
            }
            return pixels;
        }
    }

    @FunctionalInterface
    public interface EntryInput<E extends ZipEntry> {
        InputStream open(E entry) throws IOException;
    }
}
