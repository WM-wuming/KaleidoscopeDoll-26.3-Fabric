package com.github.ysbbbbbb.kaleidoscopedoll.data.custom;

import com.github.ysbbbbbb.kaleidoscopedoll.KaleidoscopeDoll;
import com.google.gson.JsonParseException;
import net.minecraft.util.GsonHelper;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class TransferableDollPack {
    private static final int MAX_ENTRIES = 8192;
    private static final long MAX_UNCOMPRESSED_BYTES = 64L * 1024 * 1024;
    private static final int MAX_JSON_BYTES = 2 * 1024 * 1024;
    private static final long MAX_TEXTURE_PIXELS = 16L * 1024 * 1024;
    private static final Pattern ASSET = Pattern.compile(
            "assets/([a-z0-9_.-]+)/(?:items/.+\\.json|models/.+\\.json|textures/.+\\.png(?:\\.mcmeta)?|custom_dolls/lang/[a-z0-9_]+\\.json)");
    private static final Set<String> ROOT_FILES = Set.of("pack.mcmeta", "pack.png", "custom_dolls.json");

    private TransferableDollPack() {
    }

    public static void validate(ZipFile zip) throws IOException {
        if (zip.size() > MAX_ENTRIES || !CustomDollConfigPacks.hasManifest(zip)) {
            throw new IOException("Missing doll manifest or too many ZIP entries");
        }
        long remaining = MAX_UNCOMPRESSED_BYTES;
        long remainingPixels = MAX_TEXTURE_PIXELS;
        byte[] buffer = new byte[8192];
        Set<String> seen = new HashSet<>();
        var entries = zip.entries();
        while (entries.hasMoreElements()) {
            var entry = entries.nextElement();
            String name = entry.getName();
            if (!seen.add(name) || !name.matches("[a-z0-9_./-]+") || name.startsWith("/")
                    || name.contains("//") || Set.of(".", "..").stream().anyMatch(part -> name.equals(part)
                    || name.startsWith(part + "/") || name.contains("/" + part + "/") || name.endsWith("/" + part))) {
                throw new IOException("Invalid ZIP entry: " + name);
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
            if (!name.endsWith(".png") && size > MAX_JSON_BYTES) {
                throw new IOException("Oversized doll JSON: " + name);
            }
            remainingPixels -= checkEntry(zip, entry, buffer);
            if (remainingPixels < 0) {
                throw new IOException("Doll ZIP exceeds its texture pixel limit");
            }
        }

        var metadata = zip.getEntry("pack.mcmeta");
        if (metadata == null || metadata.getSize() > 16 * 1024 || zip.getEntry("custom_dolls.json").getSize() > 256 * 1024) {
            throw new IOException("Missing or oversized pack metadata");
        }
        try (var reader = new InputStreamReader(zip.getInputStream(metadata), StandardCharsets.UTF_8)) {
            var fields = GsonHelper.parse(reader).keySet();
            if (!fields.equals(Set.of("pack"))) {
                throw new IOException("Transferred packs may not define filters or overlays");
            }
        } catch (JsonParseException e) {
            throw new IOException("Invalid pack metadata", e);
        }
        Set<String> ids = new HashSet<>();
        CustomDollConfigPacks.readModelIds(zip, ids);
        if (ids.isEmpty()) {
            throw new IOException("No Java doll models found");
        }
    }

    private static long checkEntry(ZipFile zip, ZipEntry entry, byte[] buffer) throws IOException {
        try (var stream = zip.getInputStream(entry)) {
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
}
