package com.kaleidoscopedoll.render;

import com.kaleidoscopedoll.compat.DollBlockSubmission;
import com.kaleidoscopedoll.compat.DollPoseStack;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.kaleidoscopedoll.client.bedrock.BedrockModel;
import com.kaleidoscopedoll.client.custom.CustomDollLoader;
import com.kaleidoscopedoll.client.render.JavaDollModels;
import com.kaleidoscopedoll.client.resources.CustomDollReloadListener;
import com.kaleidoscopedoll.entity.DollEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

public class DollEntityRender extends GeoEntityRenderer<DollEntity, DollEntityRenderState> {
    public DollEntityRender(EntityRendererProvider.Context context) {
        super(context, new AnimatedRootModel<>());
    }

    @Override
    public DollEntityRenderState createRenderState(DollEntity entity, @Nullable Void relatedObject) {
        return new DollEntityRenderState();
    }

    @Override
    public void extractRenderState(DollEntity entity, DollEntityRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);

        BlockState blockState = entity.getDisplayBlockState();
        // 空玩偶回退渲染紫色礼盒，避免实体玩偶因显示状态为空气而不可见（旧存档/历史叠放产生的空玩偶）
        if (blockState.isAir() && StringUtils.isBlank(entity.getCustomDollId()) && entity.getHoldItem().isEmpty()) {
            blockState = com.kaleidoscopedoll.init.ModBlocks.PURPLE_DOLL_GIFT_BOX.defaultBlockState();
        }
        state.movingBlockRenderState.blockState = blockState;
        BlockPos pos = entity.blockPosition();
        state.movingBlockRenderState.randomSeedPos = pos;
        state.movingBlockRenderState.blockPos = pos;
        if (entity.level() instanceof ClientLevel clientLevel) {
            state.movingBlockRenderState.biome = clientLevel.getBiome(pos);
            state.movingBlockRenderState.cardinalLighting = clientLevel.cardinalLighting();
            state.movingBlockRenderState.lightEngine = clientLevel.getLightEngine();
        }

        state.translation.set(entity.getDisplayTranslation());
        state.scale.set(entity.getDisplayScale());

        Entity vehicle = entity.getVehicle();
        Entity yawSource = vehicle != null ? vehicle : entity;
        state.yaw = Mth.rotLerp(partialTick, yawSource.yRotO, yawSource.getYRot());
        state.pitch = Mth.rotLerp(partialTick, entity.xRotO, entity.getXRot());

        state.customDollId = entity.getCustomDollId();
        JavaDollModels.extractWorldModel(state.javaDollModel, state.customDollId);
        state.holdItem = entity.getHoldItem();
        state.itemTranslation.set(entity.getItemTranslation());
        state.itemScale.set(entity.getItemScale());
        state.itemRotation.set(entity.getItemRotation());
        if (!state.holdItem.isEmpty()) {
            this.itemModelResolver.updateForNonLiving(state.holdItemState, state.holdItem, ItemDisplayContext.GROUND, entity);
        }
    }

    @Override
    public void submit(DollEntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        BlockState blockState = state.movingBlockRenderState.blockState;
        boolean hasBody = StringUtils.isNotBlank(state.customDollId) || !blockState.isAir();
        boolean hasHoldItem = !state.holdItem.isEmpty();
        if (!hasBody && !hasHoldItem) {
            return;
        }

        poseStack.pushPose();

        // 应用位移变换
        poseStack.translate(state.translation.x, state.translation.y, state.translation.z);

        // 应用旋转
        DollPoseStack.rotateDegrees(poseStack, Axis.YP, -state.yaw);
        DollPoseStack.rotateDegrees(poseStack, Axis.XP, state.pitch);

        // 应用缩放变换
        poseStack.scale(state.scale.x, state.scale.y, state.scale.z);

        RenderPassInfo<DollEntityRenderState> animationPass = AnimatedRootModel.apply(this, state, poseStack, camera, 0);

        // 将方块中心对齐到实体位置
        poseStack.translate(-0.5, 0, -0.5);

        // 渲染本体
        // 玩偶方块的 RenderShape 是 INVISIBLE（由方块实体渲染器负责绘制），
        // 这里不能按 RenderShape.MODEL 过滤，否则实体玩偶会因方块形状被跳过而不可见
        if (!state.javaDollModel.isEmpty()) {
            JavaDollModels.submitCentered(state.javaDollModel, poseStack, submitNodeCollector, state.lightCoords, state.outlineColor);
        } else if (StringUtils.isNotBlank(state.customDollId)) {
            renderCustom(state.customDollId, poseStack, submitNodeCollector, state.lightCoords);
        } else if (!blockState.isAir()) {
            DollBlockSubmission.submit(submitNodeCollector, poseStack, state.movingBlockRenderState, state.outlineColor);
        }

        // 渲染持有物品
        if (hasHoldItem) {
            poseStack.pushPose();
            poseStack.translate(state.itemTranslation.x, state.itemTranslation.y, state.itemTranslation.z);
            poseStack.scale(state.itemScale.x, state.itemScale.y, state.itemScale.z);
            DollPoseStack.rotateDegrees(poseStack, Axis.XP, state.itemRotation.x);
            DollPoseStack.rotateDegrees(poseStack, Axis.YP, state.itemRotation.y);
            DollPoseStack.rotateDegrees(poseStack, Axis.ZP, state.itemRotation.z);
            state.holdItemState.submit(poseStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
            poseStack.popPose();
        }

        poseStack.popPose();
        super.postRenderPass(animationPass, submitNodeCollector);
    }

    private void renderCustom(String modelId, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int packedLight) {
        BedrockModel model;
        Identifier texture;

        model = CustomDollLoader.getModel(modelId);
        if (model == null) {
            model = CustomDollReloadListener.DFAULT_DOLL_MODEL;
            texture = CustomDollReloadListener.DEFAULT_TEXTURE_ID;
        } else {
            texture = CustomDollLoader.getTexture(modelId);
            if (texture == null) {
                texture = MissingTextureAtlasSprite.getLocation();
            }
        }

        if (model == null) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(0.5, 1.5, 0.5);
        DollPoseStack.rotate(poseStack, Axis.ZN.rotationDegrees(180));
        DollPoseStack.rotate(poseStack, Axis.YN.rotationDegrees(180));

        final BedrockModel finalModel = model;
        submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(texture), (pose, buffer) -> {
            PoseStack modelPose = new PoseStack();
            modelPose.last().set(pose);
            finalModel.renderToBuffer(modelPose, buffer, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        });
        poseStack.popPose();
    }
}
