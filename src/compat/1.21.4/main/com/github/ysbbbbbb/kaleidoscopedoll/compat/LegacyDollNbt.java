package com.github.ysbbbbbb.kaleidoscopedoll.compat;

import com.mojang.serialization.Codec;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;

import java.util.Optional;

public final class LegacyDollNbt {
    private LegacyDollNbt() {
    }

    public static <T> Optional<T> read(CompoundTag tag, String key, Codec<T> codec, HolderLookup.Provider registries) {
        Tag value = tag.get(key);
        return value == null ? Optional.empty() : codec.parse(registries.createSerializationContext(NbtOps.INSTANCE), value).result();
    }

    public static <T> void store(CompoundTag tag, String key, Codec<T> codec, T value, HolderLookup.Provider registries) {
        codec.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), value)
                .result().ifPresent(encoded -> tag.put(key, encoded));
    }

    public static CompoundTag child(CompoundTag tag, String key) {
        CompoundTag child = new CompoundTag();
        tag.put(key, child);
        return child;
    }

    public static float readFloat(CompoundTag tag, String key, float fallback) {
        return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? tag.getFloat(key) : fallback;
    }
}
