package com.kaleidoscopedoll.data.resources;

import com.kaleidoscopedoll.KaleidoscopeDoll;
import com.kaleidoscopedoll.data.custom.ServerCustomDollLoader;
import com.kaleidoscopedoll.data.custom.ServerCustomDollResourceLoader;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;

public class ServerCustomDollReloadListener implements SimpleSynchronousResourceReloadListener {
    @Override
    public Identifier getFabricId() {
        return Identifier.fromNamespaceAndPath(KaleidoscopeDoll.MOD_ID, "server_custom_doll");
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        try {
            ServerCustomDollResourceLoader.init(resourceManager);
            ServerCustomDollLoader.init();
        } catch (IOException e) {
            KaleidoscopeDoll.LOGGER.error("Failed to reload custom dolls", e);
        }
    }
}