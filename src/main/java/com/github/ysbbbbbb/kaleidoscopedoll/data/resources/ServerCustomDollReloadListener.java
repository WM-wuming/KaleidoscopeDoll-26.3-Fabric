package com.github.ysbbbbbb.kaleidoscopedoll.data.resources;

import com.github.ysbbbbbb.kaleidoscopedoll.KaleidoscopeDoll;
import com.github.ysbbbbbb.kaleidoscopedoll.data.custom.ServerCustomDollLoader;
import com.github.ysbbbbbb.kaleidoscopedoll.data.custom.ServerCustomDollResourceLoader;
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