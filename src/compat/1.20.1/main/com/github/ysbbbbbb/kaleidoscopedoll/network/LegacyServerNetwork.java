package com.github.ysbbbbbb.kaleidoscopedoll.network;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.BiConsumer;
import java.util.function.Function;

public final class LegacyServerNetwork {
    private LegacyServerNetwork() {
    }

    public record Channel<T extends Payload>(ResourceLocation id, Function<FriendlyByteBuf, T> read) {}

    public interface Payload {
        Channel<? extends Payload> type();
        void write(FriendlyByteBuf buffer);
    }

    public record Context(MinecraftServer server, ServerPlayer player) {}

    public static <T extends Payload> void registerGlobalReceiver(Channel<T> channel, BiConsumer<T, Context> handler) {
        ServerPlayNetworking.registerGlobalReceiver(channel.id(), (server, player, connection, buffer, sender) -> {
            T packet = channel.read().apply(buffer);
            server.execute(() -> handler.accept(packet, new Context(server, player)));
        });
    }

    public static boolean canSend(ServerPlayer player, Channel<?> channel) {
        return ServerPlayNetworking.canSend(player, channel.id());
    }

    public static void send(ServerPlayer player, Payload packet) {
        var buffer = PacketByteBufs.create();
        packet.write(buffer);
        ServerPlayNetworking.send(player, packet.type().id(), buffer);
    }
}
