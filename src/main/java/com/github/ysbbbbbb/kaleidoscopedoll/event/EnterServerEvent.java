package com.github.ysbbbbbb.kaleidoscopedoll.event;

import com.github.ysbbbbbb.kaleidoscopedoll.data.custom.ServerCustomDollLoader;
import com.github.ysbbbbbb.kaleidoscopedoll.network.NetworkHandler;
import com.github.ysbbbbbb.kaleidoscopedoll.network.ServerDollPackSync;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

public class EnterServerEvent {
    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            sync(handler.player);
        });
    }

    public static void sync(ServerPlayer player) {
        if (ServerPlayNetworking.canSend(player, NetworkHandler.CustomDollReloadPayload.TYPE)) {
            ServerPlayNetworking.send(player, new NetworkHandler.CustomDollReloadPayload(ServerCustomDollLoader.getModels()));
        }
        ServerDollPackSync.offer(player);
    }
}
