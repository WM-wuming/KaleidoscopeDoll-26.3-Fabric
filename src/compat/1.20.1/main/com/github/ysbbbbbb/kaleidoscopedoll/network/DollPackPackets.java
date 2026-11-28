package com.github.ysbbbbbb.kaleidoscopedoll.network;

import com.github.ysbbbbbb.kaleidoscopedoll.KaleidoscopeDoll;
import com.github.ysbbbbbb.kaleidoscopedoll.network.LegacyServerNetwork.Channel;
import com.github.ysbbbbbb.kaleidoscopedoll.network.LegacyServerNetwork.Payload;
import com.google.common.hash.Hashing;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.function.Function;

public final class DollPackPackets {
    public static final int MAX_PACKS = 8;
    public static final int MAX_TOTAL_BYTES = 16 * 1024 * 1024;
    public static final int CHUNK_BYTES = 48 * 1024;

    private DollPackPackets() {
    }

    static <T extends Payload> Channel<T> channel(String path, Function<FriendlyByteBuf, T> reader) {
        return new Channel<>(new ResourceLocation(KaleidoscopeDoll.MOD_ID, path), reader);
    }

    public record PackInfo(int size, String sha256) {
        public static PackInfo of(byte[] bytes) {
            return new PackInfo(bytes.length, Hashing.sha256().hashBytes(bytes).toString());
        }

        private static PackInfo read(FriendlyByteBuf buffer) {
            return new PackInfo(buffer.readVarInt(), buffer.readUtf(64));
        }

        private void write(FriendlyByteBuf buffer) {
            buffer.writeVarInt(size);
            buffer.writeUtf(sha256, 64);
        }
    }

    public record Offer(int version, List<PackInfo> packs) implements Payload {
        public static final Channel<Offer> TYPE = channel("doll_pack_offer", buffer ->
                new Offer(buffer.readVarInt(), buffer.readCollection(
                        FriendlyByteBuf.limitValue(java.util.ArrayList::new, MAX_PACKS), PackInfo::read)));

        public boolean isWithinLimits() {
            return packs.size() <= MAX_PACKS
                    && packs.stream().allMatch(pack -> pack.size() > 0 && pack.sha256().matches("[0-9a-f]{64}"))
                    && packs.stream().mapToLong(PackInfo::size).sum() <= MAX_TOTAL_BYTES;
        }

        @Override
        public Channel<Offer> type() {
            return TYPE;
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
            buffer.writeVarInt(version);
            buffer.writeCollection(packs, (buf, pack) -> pack.write(buf));
        }
    }

    public record Request(int version) implements Payload {
        public static final Channel<Request> TYPE = channel("doll_pack_request", buffer -> new Request(buffer.readVarInt()));

        @Override
        public Channel<Request> type() {
            return TYPE;
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
            buffer.writeVarInt(version);
        }
    }

    public record Chunk(int version, int packIndex, int offset, byte[] data) implements Payload {
        public static final Channel<Chunk> TYPE = channel("doll_pack_chunk", buffer ->
                new Chunk(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readByteArray(CHUNK_BYTES)));

        @Override
        public Channel<Chunk> type() {
            return TYPE;
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
            buffer.writeVarInt(version);
            buffer.writeVarInt(packIndex);
            buffer.writeVarInt(offset);
            buffer.writeByteArray(data);
        }
    }
}
