package com.kaleidoscopedoll.data.custom;

import com.kaleidoscopedoll.KaleidoscopeDoll;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class CustomDollConfigPacks {
    public static final Path ROOT = FabricLoader.getInstance().getConfigDir().resolve(KaleidoscopeDoll.MOD_ID).resolve("custom");
    public static final String MODEL_PREFIX = "models/block/doll";
    private static final String ASSETS_PREFIX = "assets/";
    private static final String JAVA_PREFIX = "java:";

    private CustomDollConfigPacks() {
    }

    public static boolean isArchive(Path path) {
        return Files.isRegularFile(path) && path.getFileName().toString().endsWith(".zip");
    }

    public static boolean hasJavaModels(ZipFile zip) {
        return zip.stream().filter(entry -> !entry.isDirectory()).map(ZipEntry::getName)
                .anyMatch(name -> modelIdFromAsset(name) != null);
    }

    public static Identifier modelLocation(String modelId) {
        if (modelId == null || !modelId.startsWith(JAVA_PREFIX)) {
            return null;
        }
        String target = modelId.substring(JAVA_PREFIX.length());
        Identifier id = target.indexOf(':') > 0 ? Identifier.tryParse(target) : null;
        return id != null && !id.getPath().isEmpty()
                && !Set.of("minecraft", KaleidoscopeDoll.MOD_ID).contains(id.getNamespace())
                && java.util.Arrays.stream(id.getPath().split("/", -1))
                .noneMatch(part -> part.isEmpty() || part.equals(".") || part.equals("..")) ? id : null;
    }

    public static Identifier modelFile(Identifier id) {
        return Identifier.fromNamespaceAndPath(id.getNamespace(), MODEL_PREFIX + "/" + id.getPath() + ".json");
    }

    public static Identifier renderModel(Identifier id) {
        return Identifier.fromNamespaceAndPath(id.getNamespace(), "block/doll/" + id.getPath());
    }

    public static Identifier modelIdFromFile(Identifier file) {
        String path = modelPath(file.getPath());
        return path == null ? null : modelLocation(JAVA_PREFIX + file.getNamespace() + ":" + path);
    }

    public static String modelIdFromAsset(String name) {
        if (!name.startsWith(ASSETS_PREFIX)) return null;
        int end = name.indexOf('/', ASSETS_PREFIX.length());
        if (end < 0) return null;
        String path = modelPath(name.substring(end + 1));
        if (path == null) return null;
        String model = JAVA_PREFIX + name.substring(ASSETS_PREFIX.length(), end) + ":" + path;
        return modelLocation(model) == null ? null : model;
    }

    private static String modelPath(String file) {
        String prefix = MODEL_PREFIX + "/";
        return file.startsWith(prefix) && file.endsWith(".json")
                ? file.substring(prefix.length(), file.length() - ".json".length()) : null;
    }

    public static void readModelIds(ZipFile zip, Set<String> models) {
        zip.stream().filter(entry -> !entry.isDirectory()).map(ZipEntry::getName)
                .map(CustomDollConfigPacks::modelIdFromAsset).filter(java.util.Objects::nonNull).forEach(models::add);
    }
}
