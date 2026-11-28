package com.kaleidoscopedoll.client.custom;

import com.kaleidoscopedoll.KaleidoscopeDoll;
import com.kaleidoscopedoll.client.bedrock.BedrockModel;
import com.kaleidoscopedoll.client.bedrock.BedrockModelUtil;
import com.kaleidoscopedoll.utils.Md5Utils;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

final class CustomDollAssets {
    private static final TypeToken<Map<String, String>> LANGUAGE_TYPE = new TypeToken<>() {
    };

    final Map<String, BedrockModel> models = new HashMap<>();
    final Map<String, Map<String, String>> languages = new HashMap<>();
    final Map<String, Identifier> textures = new HashMap<>();

    void read(Kind kind, String name, InputStream stream) throws IOException {
        switch (kind) {
            case MODEL -> {
                BedrockModel model = new BedrockModel(stream);
                if (model.getIdentifier() == null) {
                    throw new JsonParseException("Missing model identifier");
                }
                models.put(model.getIdentifier(), model);
            }
            case LANGUAGE -> {
                var reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
                Map<String, String> translations = BedrockModelUtil.GSON.fromJson(reader, LANGUAGE_TYPE);
                if (translations == null) {
                    throw new JsonParseException("Missing translation object");
                }
                translations.values().removeIf(value -> value == null);
                languages.computeIfAbsent(name, key -> new HashMap<>()).putAll(translations);
            }
            case TEXTURE -> {
                Identifier id = Identifier.fromNamespaceAndPath(KaleidoscopeDoll.MOD_ID, "custom/" + Md5Utils.md5Hex(name));
                NativeImage image = NativeImage.read(stream);
                DynamicTexture texture = new DynamicTexture(() -> "custom_doll_" + name, image);
                Minecraft.getInstance().getTextureManager().register(id, texture);
                textures.put(name, id);
            }
        }
    }

    void putAll(CustomDollAssets other) {
        models.putAll(other.models);
        other.languages.forEach((key, value) -> languages.computeIfAbsent(key, ignored -> new HashMap<>()).putAll(value));
        textures.putAll(other.textures);
    }

    enum Kind {
        MODEL("models", ".json", false),
        LANGUAGE("lang", ".json", false),
        TEXTURE("textures", ".png", true);

        final String directory;
        final String extension;
        final Pattern zipPath;

        Kind(String directory, String extension, boolean nestedZipEntries) {
            this.directory = directory;
            this.extension = extension;
            this.zipPath = Pattern.compile("^(?:[^/]+/)?" + directory + "/"
                    + (nestedZipEntries ? ".+" : "[^/]+") + Pattern.quote(extension) + "$");
        }

        String assetName(String path, int start) {
            return path.substring(start, path.length() - extension.length());
        }
    }
}
