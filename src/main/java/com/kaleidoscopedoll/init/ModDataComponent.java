package com.kaleidoscopedoll.init;

import com.kaleidoscopedoll.KaleidoscopeDoll;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;

public class ModDataComponent {
    public static final DataComponentType<CompoundTag> TAG_DOLL_ENTITY = DataComponentType.<CompoundTag>builder()
            .persistent(CompoundTag.CODEC)
            .networkSynchronized(ByteBufCodecs.COMPOUND_TAG)
            .build();

    public static final DataComponentType<String> TAG_CUSTOM_DOLL_ID = DataComponentType.<String>builder()
            .persistent(com.mojang.serialization.Codec.STRING)
            .networkSynchronized(ByteBufCodecs.STRING_UTF8)
            .build();

    /**
     * 铁砧合成：头盔 + 玩偶 → 头盔记录玩偶物品的注册路径（如 "doll_5"），
     * 佩戴该头盔时相当于同时佩戴头盔和玩偶
     */
    public static final DataComponentType<String> DOLL_ON_HELMET = DataComponentType.<String>builder()
            .persistent(com.mojang.serialization.Codec.STRING)
            .networkSynchronized(ByteBufCodecs.STRING_UTF8)
            .build();

    /**
     * 仅用于读取旧存档，旧功能不再生效。
     */
    private static final DataComponentType<String> LEGACY_DOLL_FUNCTION = DataComponentType.<String>builder()
            .persistent(com.mojang.serialization.Codec.STRING)
            .networkSynchronized(ByteBufCodecs.STRING_UTF8)
            .build();

    public static void registerDataComponent() {
        Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Identifier.fromNamespaceAndPath(KaleidoscopeDoll.MOD_ID, "doll_entity"), TAG_DOLL_ENTITY);
        Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Identifier.fromNamespaceAndPath(KaleidoscopeDoll.MOD_ID, "custom_doll_id"), TAG_CUSTOM_DOLL_ID);
        Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Identifier.fromNamespaceAndPath(KaleidoscopeDoll.MOD_ID, "doll_on_helmet"), DOLL_ON_HELMET);
        Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Identifier.fromNamespaceAndPath(KaleidoscopeDoll.MOD_ID, "doll_function"), LEGACY_DOLL_FUNCTION);
    }
}
