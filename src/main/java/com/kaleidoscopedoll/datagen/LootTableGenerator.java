package com.kaleidoscopedoll.datagen;

import com.kaleidoscopedoll.event.ModRegisterEvent;
import com.kaleidoscopedoll.init.ModBlocks;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootSubProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class LootTableGenerator {
    public static class BlockLootTables extends FabricBlockLootSubProvider {
        public final Set<Block> knownBlocks = new HashSet<>();

        public BlockLootTables(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registryLookup) {
            super(output, registryLookup);
        }

        @Override
        public void generate() {
            ModRegisterEvent.DOLL_BLOCKS.values().forEach(this::dropSelf);

            LootTable.Builder table = createSinglePropConditionTable(ModBlocks.DOLL_MACHINE, BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER);
            this.add(ModBlocks.DOLL_MACHINE, table);

            dropSelf(ModBlocks.COMPUTER);
        }
    }
}