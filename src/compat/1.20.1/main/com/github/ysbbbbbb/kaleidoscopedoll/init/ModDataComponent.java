package com.github.ysbbbbbb.kaleidoscopedoll.init;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.function.BiFunction;

public final class ModDataComponent {
    public static final Key<CompoundTag> TAG_DOLL_ENTITY = new Key<>("doll_entity", Tag.TAG_COMPOUND,
            CompoundTag::getCompound, CompoundTag::put);
    public static final Key<String> TAG_CUSTOM_DOLL_ID = new Key<>("custom_doll_id", Tag.TAG_STRING,
            CompoundTag::getString, CompoundTag::putString);
    public static final Key<String> DOLL_ON_HELMET = new Key<>("doll_on_helmet", Tag.TAG_STRING,
            CompoundTag::getString, CompoundTag::putString);
    public static final Key<String> LEGACY_DOLL_FUNCTION = new Key<>("doll_function", Tag.TAG_STRING,
            CompoundTag::getString, CompoundTag::putString);

    private ModDataComponent() {
    }

    public static void registerDataComponent() {
        // The old ItemStack format has no component registry.
    }

    public static <T> T get(ItemStack stack, Key<T> key) {
        CompoundTag tag = stack.getTag();
        return tag == null || !tag.contains(key.name(), key.type()) ? null : key.read().apply(tag, key.name());
    }

    public static <T> void set(ItemStack stack, Key<T> key, T value) {
        if (value == null) {
            remove(stack, key);
        } else {
            key.write().set(stack.getOrCreateTag(), key.name(), value);
        }
    }

    public static boolean has(ItemStack stack, Key<?> key) {
        return get(stack, key) != null;
    }

    public static void remove(ItemStack stack, Key<?> key) {
        stack.removeTagKey(key.name());
    }

    public record Key<T>(String name, int type, BiFunction<CompoundTag, String, T> read, Writer<T> write) {}

    @FunctionalInterface
    public interface Writer<T> {
        void set(CompoundTag tag, String key, T value);
    }
}
