package com.github.ysbbbbbb.kaleidoscopedoll.client.render;

import com.github.ysbbbbbb.kaleidoscopedoll.compat.DollPoseStack;
import com.geckolib.renderer.GeoBlockRenderer;
import com.github.ysbbbbbb.kaleidoscopedoll.block.entity.CustomDollBlockEntity;
import com.github.ysbbbbbb.kaleidoscopedoll.client.bedrock.BedrockModel;
import com.github.ysbbbbbb.kaleidoscopedoll.client.custom.CustomDollLoader;
import com.github.ysbbbbbb.kaleidoscopedoll.client.resources.CustomDollReloadListener;
import com.github.ysbbbbbb.kaleidoscopedoll.render.AnimatedRootModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

public class CustomDollRender extends GeoBlockRenderer<CustomDollBlockEntity, CustomDollRenderState> {
    public CustomDollRender(BlockEntityRendererProvider.Context render) {
        super(render, new AnimatedRootModel<>());
    }

    @Override
    public CustomDollRenderState createRenderState() {
        return new CustomDollRenderState();
    }

    @Override
    public void extractRenderState(CustomDollBlockEntity doll, CustomDollRenderState state, float partialTicks, Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        super.extractRenderState(doll, state, partialTicks, cameraPosition, breakProgress);
        state.modelId = doll.getModelId();
        state.facing = doll.getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        JavaDollModels.extractWorldModel(state.javaModel, state.modelId);
    }

    @Override
    public void submit(CustomDollRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        if (!state.javaModel.isEmpty()) {
            poseStack.pushPose();
            AnimatedRootModel.apply(this, state, poseStack, camera, 0.5);
            poseStack.translate(0.5, 0, 0.5);
            DollPoseStack.rotate(poseStack, Axis.YN.rotationDegrees(state.facing.toYRot() + 180));
            poseStack.translate(-0.5, 0, -0.5);
            JavaDollModels.submitCentered(state.javaModel, poseStack, submitNodeCollector, state.lightCoords, 0);
            poseStack.popPose();
            return;
        }

        BedrockModel model;
        Identifier texture;

        if (StringUtils.isBlank(state.modelId)) {
            model = CustomDollReloadListener.DFAULT_DOLL_MODEL;
            texture = CustomDollReloadListener.DEFAULT_TEXTURE_ID;
        } else {
            model = CustomDollLoader.getModel(state.modelId);
            if (model == null) {
                model = CustomDollReloadListener.DFAULT_DOLL_MODEL;
                texture = CustomDollReloadListener.DEFAULT_TEXTURE_ID;
            } else {
                texture = CustomDollLoader.getTexture(state.modelId);
                if (texture == null) {
                    texture = MissingTextureAtlasSprite.getLocation();
                }
            }
        }

        if (model == null) {
            return;
        }

        poseStack.pushPose();
        AnimatedRootModel.apply(this, state, poseStack, camera, 0.5);
        poseStack.translate(0.5, 1.5, 0.5);
        DollPoseStack.rotate(poseStack, Axis.ZN.rotationDegrees(180));
        DollPoseStack.rotate(poseStack, Axis.YN.rotationDegrees(180 - state.facing.get2DDataValue() * 90));

        final BedrockModel finalModel = model;
        final int light = state.lightCoords;
        submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(texture), (pose, buffer) -> {
            PoseStack modelPose = new PoseStack();
            modelPose.last().set(pose);
            finalModel.renderToBuffer(modelPose, buffer, light, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        });

        poseStack.popPose();
    }
}
