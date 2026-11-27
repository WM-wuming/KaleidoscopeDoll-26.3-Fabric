package com.github.ysbbbbbb.kaleidoscopedoll.init;

import com.github.ysbbbbbb.kaleidoscopedoll.KaleidoscopeDoll;
import com.github.ysbbbbbb.kaleidoscopedoll.block.ComputerBlock;
import com.github.ysbbbbbb.kaleidoscopedoll.block.CustomDollBlock;
import com.github.ysbbbbbb.kaleidoscopedoll.block.DollGiftBoxBlock;
import com.github.ysbbbbbb.kaleidoscopedoll.block.DollMachineBlock;
import com.github.ysbbbbbb.kaleidoscopedoll.block.entity.CustomDollBlockEntity;
import com.github.ysbbbbbb.kaleidoscopedoll.block.entity.DollBlockEntity;
import com.github.ysbbbbbb.kaleidoscopedoll.block.entity.DollGiftBoxBlockEntiy;
import com.github.ysbbbbbb.kaleidoscopedoll.block.entity.DollMachineBlockEntity;
import com.github.ysbbbbbb.kaleidoscopedoll.event.ModRegisterEvent;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.Set;

public class ModBlocks {
    public static final Block DOLL_MACHINE = new DollMachineBlock(DollMachineBlock.machineProperties().setId(blockKey("doll_machine")));
    public static final Block PURPLE_DOLL_GIFT_BOX = new DollGiftBoxBlock(DollGiftBoxBlock.giftBoxProperties().setId(blockKey("purple_doll_gift_box")));
    public static final Block GREEN_DOLL_GIFT_BOX = new DollGiftBoxBlock(DollGiftBoxBlock.giftBoxProperties().setId(blockKey("green_doll_gift_box")));
    public static final Block YELLOW_DOLL_GIFT_BOX = new DollGiftBoxBlock(DollGiftBoxBlock.giftBoxProperties().setId(blockKey("yellow_doll_gift_box")));
    public static final Block COMPUTER = new ComputerBlock(ComputerBlock.computerProperties().setId(blockKey("computer")));
    public static final Block CUSTOM_DOLL = new CustomDollBlock(dollProperties().setId(blockKey("custom_doll")));

    public static final BlockEntityType<DollGiftBoxBlockEntiy> DOLL_GIFT_BOX_BE = new BlockEntityType<>(DollGiftBoxBlockEntiy::new,
            Set.of(PURPLE_DOLL_GIFT_BOX, GREEN_DOLL_GIFT_BOX, YELLOW_DOLL_GIFT_BOX));
    public static final BlockEntityType<DollMachineBlockEntity> DOLL_MACHINE_BE = new BlockEntityType<>(DollMachineBlockEntity::new, Set.of(DOLL_MACHINE));
    public static final BlockEntityType<CustomDollBlockEntity> CUSTOM_DOLL_BE = new BlockEntityType<>(CustomDollBlockEntity::new, Set.of(CUSTOM_DOLL));
    /**
     * 所有方块玩偶共享的 BE 类型，在 registerBlocks() 末尾创建（依赖 ModRegisterEvent 先创建玩偶方块）
     */
    public static BlockEntityType<DollBlockEntity> DOLL_BE;

    private static net.minecraft.world.level.block.state.BlockBehaviour.Properties dollProperties() {
        return com.github.ysbbbbbb.kaleidoscopedoll.block.DollBlock.dollProperties();
    }

    public static ResourceKey<Block> blockKey(String name) {
        return ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(KaleidoscopeDoll.MOD_ID, name));
    }

    public static void registerBlocks() {
        Registry.register(BuiltInRegistries.BLOCK, blockKey("doll_machine"), DOLL_MACHINE);
        Registry.register(BuiltInRegistries.BLOCK, blockKey("purple_doll_gift_box"), PURPLE_DOLL_GIFT_BOX);
        Registry.register(BuiltInRegistries.BLOCK, blockKey("green_doll_gift_box"), GREEN_DOLL_GIFT_BOX);
        Registry.register(BuiltInRegistries.BLOCK, blockKey("yellow_doll_gift_box"), YELLOW_DOLL_GIFT_BOX);
        Registry.register(BuiltInRegistries.BLOCK, blockKey("computer"), COMPUTER);
        Registry.register(BuiltInRegistries.BLOCK, blockKey("custom_doll"), CUSTOM_DOLL);

        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Identifier.fromNamespaceAndPath(KaleidoscopeDoll.MOD_ID, "doll_gift_box"), DOLL_GIFT_BOX_BE);
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Identifier.fromNamespaceAndPath(KaleidoscopeDoll.MOD_ID, "doll_machine"), DOLL_MACHINE_BE);
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Identifier.fromNamespaceAndPath(KaleidoscopeDoll.MOD_ID, "custom_doll"), CUSTOM_DOLL_BE);

        ModRegisterEvent.registerBlocks();

        DOLL_BE = new BlockEntityType<>(DollBlockEntity::new, Set.copyOf(ModRegisterEvent.DOLL_BLOCKS.values()));
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Identifier.fromNamespaceAndPath(KaleidoscopeDoll.MOD_ID, "doll"), DOLL_BE);
    }
}