package com.kaleidoscopedoll.network;

import com.kaleidoscopedoll.network.LegacyServerNetwork.Channel;
import com.kaleidoscopedoll.network.LegacyServerNetwork.Payload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

import java.util.function.BiConsumer;

public final class LegacyClientNetwork {
    private LegacyClientNetwork() {
    }

    public record Context(Minecraft client, LocalPlayer player) {}

    public static <T extends Payload> void registerGlobalReceiver(Channel<T> channel, BiConsumer<T, Context> handler) {
        ClientPlayNetworking.registerGlobalReceiver(channel.id(), (client, listener, buffer, sender) -> {
            T packet = channel.read().apply(buffer);
            client.execute(() -> {
                if (client.getConnection() == listener && client.player != null) {
                    handler.accept(packet, new Context(client, client.player));
                }
            });
        });
    }

    public static boolean canSend(Channel<?> channel) {
        return ClientPlayNetworking.canSend(channel.id());
    }

    public static void send(Payload packet) {
        if (!canSend(packet.type())) return;
        var buffer = PacketByteBufs.create();
        packet.write(buffer);
        ClientPlayNetworking.send(packet.type().id(), buffer);
    }
}
