package com.github.ysbbbbbb.kaleidoscopedoll.data.custom;

import com.github.ysbbbbbb.kaleidoscopedoll.KaleidoscopeDoll;
import com.google.gson.JsonParseException;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.zip.ZipFile;

public final class CustomDollConfigPacks {
    public static final Path ROOT = FabricLoader.getInstance().getConfigDir().resolve(KaleidoscopeDoll.MOD_ID).resolve("custom");
    private static final String MANIFEST = "custom_dolls.json";
    private static final String JAVA_PREFIX = "java:";

    private CustomDollConfigPacks() {
    }

    public static boolean isArchive(Path path) {
        return Files.isRegularFile(path) && path.getFileName().toString().endsWith(".zip");
    }

    public static boolean hasManifest(ZipFile zip) {
        var entry = zip.getEntry(MANIFEST);
        return entry != null && !entry.isDirectory();
    }

    public static void readModelIds(ZipFile zip, Set<String> models) throws IOException {
        try (var stream = zip.getInputStream(zip.getEntry(MANIFEST));
             var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            for (var element : GsonHelper.parseArray(reader)) {
                if (!GsonHelper.isStringValue(element)) {
                    continue;
                }
                String modelId = element.getAsString();
                if (modelId.startsWith(JAVA_PREFIX)) {
                    String target = modelId.substring(JAVA_PREFIX.length());
                    Identifier id = target.indexOf(':') > 0 ? Identifier.tryParse(target) : null;
                    if (id != null && !id.getPath().isEmpty() && !KaleidoscopeDoll.MOD_ID.equals(id.getNamespace())
                            && zip.getEntry("assets/" + id.getNamespace() + "/items/" + id.getPath() + ".json") != null) {
                        models.add(modelId);
                    }
                }
            }
        } catch (JsonParseException e) {
            throw new IOException("Invalid " + MANIFEST + " in " + zip.getName(), e);
        }
    }
}
