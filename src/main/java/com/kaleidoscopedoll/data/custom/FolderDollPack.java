package com.kaleidoscopedoll.data.custom;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.kaleidoscopedoll.KaleidoscopeDoll;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class FolderDollPack {
    private static final LinkOption NO_LINKS = LinkOption.NOFOLLOW_LINKS;
    private static final int MAX_ENTRIES = 8192;
    private static final int MAX_JSON_BYTES = 2 * 1024 * 1024;
    private static final long MAX_EXPANDED_BYTES = 64L * 1024 * 1024;
    private static final LocalDateTime ZIP_TIMESTAMP = LocalDateTime.of(2000, 1, 1, 0, 0);
    private static final String MODEL_PREFIX = "block/doll/";

    private final Map<String, Path> files = new TreeMap<>();
    private final Map<String, Drop> drops = new LinkedHashMap<>();
    private final Set<String> models = new LinkedHashSet<>();

    private FolderDollPack() {
    }

    public static boolean isPack(Path path) {
        return Files.isDirectory(path, NO_LINKS)
                && Files.isDirectory(path.resolve("custom_dolls"), NO_LINKS)
                && Files.isDirectory(path.resolve("custom_dolls/models"), NO_LINKS);
    }

    public static FolderDollPack read(Path path) throws IOException {
        var pack = new FolderDollPack();
        Path root = path.resolve("custom_dolls");
        var models = collectFiles(root.resolve("models"), name -> name.endsWith(".json"));
        var textures = collectFiles(root.resolve("textures"), name -> name.endsWith(".png") || name.endsWith(".png.mcmeta"));
        var loot = collectFiles(root.resolve("loot_table"), name -> name.endsWith(".json"));
        String namespace = namespace(path, models, textures);
        String assets = "assets/" + namespace + "/";
        pack.rootFile(path, "pack.mcmeta", 16 * 1024, true);
        pack.rootFile(path, "pack.png", MAX_EXPANDED_BYTES, false);
        textures.forEach((name, file) -> pack.files.put(assets + "textures/" + MODEL_PREFIX + name, file));
        pack.collect(root.resolve("lang"), assets + "custom_dolls/lang/", name -> name.matches("[a-z0-9_]+\\.json"));
        for (var entry : models.entrySet()) {
            String filename = entry.getKey();
            String name = filename.substring(0, filename.length() - ".json".length());
            String modelId = "java:" + namespace + ":" + name;
            if (CustomDollConfigPacks.modelLocation(modelId) == null) continue;
            pack.models.add(modelId);
            pack.files.put(assets + "models/" + MODEL_PREFIX + filename, entry.getValue());

            Path table = loot.get(filename);
            if (table != null) {
                try {
                    pack.drops.put(modelId, new Drop(table, readJson(table)));
                } catch (IOException e) {
                    KaleidoscopeDoll.LOGGER.warn("Ignoring invalid doll loot table {}: {}", table, e.getMessage());
                }
            }
        }
        if (pack.models.isEmpty() || pack.files.size() > MAX_ENTRIES) {
            throw new IOException("No usable Java dolls or too many entries in " + path);
        }
        return pack;
    }

    private static String namespace(Path path, Map<String, Path> models, Map<String, Path> textures) throws IOException {
        Set<String> namespaces = new LinkedHashSet<>();
        for (Path model : models.values()) {
            var definition = readJson(model);
            addNamespace(definition.get("parent"), ".json", models, namespaces);
            if (definition.get("textures") instanceof JsonObject references) {
                for (var reference : references.entrySet()) {
                    addNamespace(reference.getValue(), ".png", textures, namespaces);
                }
            }
        }
        if (namespaces.size() > 1) {
            throw new IOException("Doll folder references local files under several namespaces: " + namespaces);
        }
        if (!namespaces.isEmpty()) return namespaces.iterator().next();
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]+", "_");
        return CustomDollConfigPacks.modelLocation("java:" + name + ":model") == null ? "custom_" + name : name;
    }

    private static void addNamespace(JsonElement reference, String extension, Map<String, Path> resources, Set<String> result) {
        if (reference == null || !GsonHelper.isStringValue(reference)) return;
        String value = reference.getAsString();
        if (value.indexOf(':') < 0) return;
        Identifier id = CustomDollConfigPacks.modelLocation("java:" + value);
        if (id != null && id.getPath().startsWith(MODEL_PREFIX)
                && resources.containsKey(id.getPath().substring(MODEL_PREFIX.length()) + extension)) {
            result.add(id.getNamespace());
        }
    }

    Set<String> models() {
        return models;
    }

    Drop drop(String model) {
        return drops.get(model);
    }

    public byte[] archive(int limit) throws IOException {
        var result = new ByteArrayOutputStream();
        var bounded = new OutputStream() {
            @Override
            public void write(int value) throws IOException {
                if (result.size() >= limit) throw new IOException("Doll folder exceeds the compressed sync limit");
                result.write(value);
            }

            @Override
            public void write(byte[] bytes, int offset, int length) throws IOException {
                if (length > limit - result.size()) throw new IOException("Doll folder exceeds the compressed sync limit");
                result.write(bytes, offset, length);
            }
        };
        long remaining = MAX_EXPANDED_BYTES;
        byte[] buffer = new byte[8192];
        try (var zip = new ZipOutputStream(bounded)) {
            for (var file : files.entrySet()) {
                var size = Files.size(file.getValue());
                if (size > remaining) throw new IOException("Doll folder exceeds the uncompressed size limit");
                remaining -= size;
                putEntry(zip, file.getKey());
                try (var input = Files.newInputStream(file.getValue(), NO_LINKS)) {
                    for (long left = size; left > 0;) {
                        int read = input.read(buffer, 0, (int) Math.min(left, buffer.length));
                        if (read < 0) throw new IOException("Doll file changed during sync preparation");
                        zip.write(buffer, 0, read);
                        left -= read;
                    }
                    if (input.read() != -1) throw new IOException("Doll file changed during sync preparation");
                }
                zip.closeEntry();
            }
        }
        return result.toByteArray();
    }

    private static void putEntry(ZipOutputStream zip, String name) throws IOException {
        var entry = new ZipEntry(name);
        entry.setTimeLocal(ZIP_TIMESTAMP);
        zip.putNextEntry(entry);
    }

    private void rootFile(Path root, String name, long maxSize, boolean required) throws IOException {
        Path path = root.resolve(name);
        if (Files.isRegularFile(path, NO_LINKS)) {
            if (Files.size(path) > maxSize) throw new IOException("Oversized doll file: " + path);
            files.put(name, path);
        } else if (required) {
            throw new IOException("Missing required doll file: " + path);
        }
    }

    private void collect(Path root, String prefix, Predicate<String> allowed) throws IOException {
        for (var file : collectFiles(root, allowed).entrySet()) {
            files.put(prefix + file.getKey(), file.getValue());
        }
    }

    private static Map<String, Path> collectFiles(Path root, Predicate<String> allowed) throws IOException {
        var files = new TreeMap<String, Path>();
        if (Files.isDirectory(root, NO_LINKS)) {
            try (var paths = Files.walk(root)) {
                var entries = paths.limit(MAX_ENTRIES + 1L).toList();
                if (entries.size() > MAX_ENTRIES) throw new IOException("Too many doll files in " + root);
                for (Path path : entries) {
                    if (!Files.isRegularFile(path, NO_LINKS)) continue;
                    String name = root.relativize(path).toString().replace('\\', '/');
                    if (name.matches("[a-z0-9_.-]+(?:/[a-z0-9_.-]+)*") && allowed.test(name)) {
                        if (!name.endsWith(".png") && Files.size(path) > MAX_JSON_BYTES) {
                            throw new IOException("Oversized doll JSON: " + path);
                        }
                        files.put(name, path);
                    }
                }
            }
        }
        return files;
    }

    private static JsonObject readJson(Path path) throws IOException {
        try (var reader = new InputStreamReader(Files.newInputStream(path, NO_LINKS), StandardCharsets.UTF_8)) {
            return GsonHelper.parse(reader);
        } catch (JsonParseException e) {
            throw new IOException("Invalid doll JSON: " + path, e);
        }
    }

    record Drop(Path path, JsonObject table) {
    }
}
