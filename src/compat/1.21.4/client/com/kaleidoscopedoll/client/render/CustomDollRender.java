package com.kaleidoscopedoll.client.render;

import com.kaleidoscopedoll.block.entity.CustomDollBlockEntity;
import com.kaleidoscopedoll.render.LegacyDollRender;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;

public class CustomDollRender implements BlockEntityRenderer<CustomDollBlockEntity> {
    private final LegacyDollRender.Animation<CustomDollBlockEntity> animation = new LegacyDollRender.Animation<>("animated_root");
    private final ItemStackRenderState javaModel = new ItemStackRenderState();

    public CustomDollRender(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(CustomDollBlockEntity doll, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        animation.prepare(doll, doll.getBlockPos().asLong(), partialTick);
        animation.applyRoot(pose, 0.5);
        var facing = doll.getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        pose.rotateAround(Axis.YN.rotationDegrees(facing.toYRot() + 180), 0.5f, 0, 0.5f);
        JavaDollModels.extractWorldModel(javaModel, doll.getModelId());
        if (javaModel.isEmpty()) {
            LegacyDollRender.bedrock(doll.getModelId(), pose, buffers, light, overlay);
        } else {
            JavaDollModels.renderCentered(javaModel, pose, buffers, light, overlay);
        }
        pose.popPose();
    }
}
