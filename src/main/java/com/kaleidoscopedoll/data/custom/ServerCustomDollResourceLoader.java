package com.kaleidoscopedoll.data.custom;

import com.google.common.collect.Sets;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;
import java.util.Set;

public class ServerCustomDollResourceLoader {
    private static final String MODEL_DIRECTORY = "custom_dolls/models";

    /**
     * 缓存数据
     */
    private static final Set<String> MODELS = Sets.newLinkedHashSet();

    public static void init(ResourceManager manager) throws IOException {
        MODELS.clear();

        for (var model : manager.listResources(MODEL_DIRECTORY, id -> id.getPath().endsWith(".json")).values()) {
            try (var stream = model.open()) {
                ServerCustomDollLoader.readBedrockModel(stream, MODELS);
            }
        }
    }

    public static void putAll(Set<String> target) {
        target.addAll(MODELS);
    }
}
