package com.kaleidoscopedoll.network;

import com.kaleidoscopedoll.KaleidoscopeDoll;
import com.kaleidoscopedoll.network.LegacyServerNetwork.Channel;
import com.kaleidoscopedoll.network.LegacyServerNetwork.Payload;
import com.google.common.hash.Hashing;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.function.Function;

public final class DollPackPackets {
    public static final int MAX_PACKS = 64;
    public static final int MAX_TOTAL_MIB = 64;
    public static final int BYTES_PER_MIB = 1024 * 1024;
    public static final int MAX_TOTAL_BYTES = MAX_TOTAL_MIB * BYTES_PER_MIB;
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

    public record Offer(int version, List<PackInfo> packs, byte[] key) implements Payload {
        public static final Channel<Offer> TYPE = channel("doll_pack_offer_v2", buffer ->
                new Offer(buffer.readVarInt(), buffer.readCollection(
                        FriendlyByteBuf.limitValue(java.util.ArrayList::new, MAX_PACKS), PackInfo::read),
                        buffer.readByteArray(DollPackEncryption.KEY_BYTES)));

        public boolean isWithinLimits() {
            return (key.length == 0 || key.length == DollPackEncryption.KEY_BYTES)
                    && packs.size() <= MAX_PACKS
                    && packs.stream().allMatch(pack -> pack.size() > 0 && pack.sha256().matches("[0-9a-f]{64}"))
                    && packs.stream().mapToLong(PackInfo::size).sum() <= MAX_TOTAL_BYTES;
        }

        public int transferSize(int index) {
            return packs.get(index).size() + (key.length == 0 ? 0 : DollPackEncryption.OVERHEAD);
        }

        @Override
        public Channel<Offer> type() {
            return TYPE;
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
            buffer.writeVarInt(version);
            buffer.writeCollection(packs, (buf, pack) -> pack.write(buf));
            buffer.writeByteArray(key);
        }
    }

    public record Request(int version) implements Payload {
        public static final Channel<Request> TYPE = channel("doll_pack_request_v2", buffer -> new Request(buffer.readVarInt()));

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
        public static final Channel<Chunk> TYPE = channel("doll_pack_chunk_v2", buffer ->
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
