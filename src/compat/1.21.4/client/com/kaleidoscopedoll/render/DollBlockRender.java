package com.kaleidoscopedoll.render;

import com.kaleidoscopedoll.block.entity.DollBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public class DollBlockRender implements BlockEntityRenderer<DollBlockEntity> {
    private final LegacyDollRender.Animation<DollBlockEntity> animation = new LegacyDollRender.Animation<>("animated_root");

    public DollBlockRender(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(DollBlockEntity doll, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        animation.prepare(doll, doll.getBlockPos().asLong(), partialTick);
        animation.applyRoot(pose, 0.5);
        LegacyDollRender.block(doll.getBlockState(), pose, buffers, light, overlay);
        pose.popPose();
    }
}
