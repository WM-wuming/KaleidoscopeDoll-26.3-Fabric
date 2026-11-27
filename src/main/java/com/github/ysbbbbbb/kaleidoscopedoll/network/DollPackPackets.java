package com.github.ysbbbbbb.kaleidoscopedoll.network;

import com.github.ysbbbbbb.kaleidoscopedoll.KaleidoscopeDoll;
import com.google.common.hash.Hashing;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;

public final class DollPackPackets {
    public static final int MAX_PACKS = 8;
    public static final int MAX_TOTAL_BYTES = 16 * 1024 * 1024;
    public static final int CHUNK_BYTES = 48 * 1024;

    private DollPackPackets() {
    }

    public static void register() {
        PayloadTypeRegistry.clientboundPlay().register(Offer.TYPE, Offer.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Chunk.TYPE, Chunk.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(Request.TYPE, Request.CODEC);
    }

    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> type(String path) {
        return new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(KaleidoscopeDoll.MOD_ID, path));
    }

    public record PackInfo(int size, String sha256) {
        private static final StreamCodec<ByteBuf, PackInfo> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, PackInfo::size,
                ByteBufCodecs.stringUtf8(64), PackInfo::sha256,
                PackInfo::new);

        public static PackInfo of(byte[] bytes) {
            return new PackInfo(bytes.length, Hashing.sha256().hashBytes(bytes).toString());
        }
    }

    public record Offer(int version, List<PackInfo> packs) implements CustomPacketPayload {
        public static final Type<Offer> TYPE = DollPackPackets.type("doll_pack_offer");
        private static final StreamCodec<ByteBuf, Offer> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Offer::version,
                PackInfo.CODEC.apply(ByteBufCodecs.list(MAX_PACKS)), Offer::packs,
                Offer::new);

        public boolean isWithinLimits() {
            return packs.size() <= MAX_PACKS
                    && packs.stream().allMatch(pack -> pack.size() > 0 && pack.sha256().matches("[0-9a-f]{64}"))
                    && packs.stream().mapToLong(PackInfo::size).sum() <= MAX_TOTAL_BYTES;
        }

        @Override
        public Type<Offer> type() {
            return TYPE;
        }
    }

    public record Request(int version) implements CustomPacketPayload {
        public static final Type<Request> TYPE = DollPackPackets.type("doll_pack_request");
        private static final StreamCodec<ByteBuf, Request> CODEC = ByteBufCodecs.VAR_INT.map(Request::new, Request::version);

        @Override
        public Type<Request> type() {
            return TYPE;
        }
    }

    public record Chunk(int version, int packIndex, int offset, byte[] data) implements CustomPacketPayload {
        public static final Type<Chunk> TYPE = DollPackPackets.type("doll_pack_chunk");
        private static final StreamCodec<ByteBuf, Chunk> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Chunk::version,
                ByteBufCodecs.VAR_INT, Chunk::packIndex,
                ByteBufCodecs.VAR_INT, Chunk::offset,
                ByteBufCodecs.byteArray(CHUNK_BYTES), Chunk::data,
                Chunk::new);

        @Override
        public Type<Chunk> type() {
            return TYPE;
        }
    }
}
