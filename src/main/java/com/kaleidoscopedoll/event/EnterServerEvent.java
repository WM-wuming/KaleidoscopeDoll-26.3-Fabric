package com.kaleidoscopedoll.event;

import com.kaleidoscopedoll.data.custom.ServerCustomDollLoader;
import com.kaleidoscopedoll.network.NetworkHandler;
import com.kaleidoscopedoll.network.ServerDollPackSync;
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
