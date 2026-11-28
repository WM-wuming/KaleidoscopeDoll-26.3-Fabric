package com.kaleidoscopedoll.render;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;

public class DollBlockRenderState extends BlockEntityRenderState {
    public final MovingBlockRenderState movingBlockRenderState = new MovingBlockRenderState();

    void captureBlock(BlockEntity blockEntity) {
        BlockPos pos = blockEntity.getBlockPos();
        movingBlockRenderState.blockState = blockEntity.getBlockState();
        movingBlockRenderState.randomSeedPos = pos;
        movingBlockRenderState.blockPos = pos;
        if (blockEntity.getLevel() instanceof ClientLevel clientLevel) {
            movingBlockRenderState.biome = clientLevel.getBiome(pos);
            movingBlockRenderState.cardinalLighting = clientLevel.cardinalLighting();
            movingBlockRenderState.lightEngine = clientLevel.getLightEngine();
        }
    }
}
