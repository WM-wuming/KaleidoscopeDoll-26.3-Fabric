package com.github.ysbbbbbb.kaleidoscopedoll.render;

import com.github.ysbbbbbb.kaleidoscopedoll.compat.DollBlockSubmission;
import com.geckolib.renderer.GeoBlockRenderer;
import com.github.ysbbbbbb.kaleidoscopedoll.block.entity.DollBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * 方块玩偶渲染器：正常情况渲染效果与静态方块模型一致，右键时施加收缩动画
 */
public class DollBlockRender extends GeoBlockRenderer<DollBlockEntity, DollBlockRenderState> {
    public DollBlockRender(BlockEntityRendererProvider.Context context) {
        super(context, new AnimatedRootModel<>());
    }

    @Override
    public DollBlockRenderState createRenderState() {
        return new DollBlockRenderState();
    }

    @Override
    public void extractRenderState(DollBlockEntity blockEntity, DollBlockRenderState state, float partialTicks,
                                   @Nullable Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        state.captureBlock(blockEntity);
    }

    @Override
    public void submit(DollBlockRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        poseStack.pushPose();
        AnimatedRootModel.apply(this, state, poseStack, camera, 0.5);
        DollBlockSubmission.submit(submitNodeCollector, poseStack, state.movingBlockRenderState, 0);
        poseStack.popPose();
    }
}
