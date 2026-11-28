package com.github.ysbbbbbb.kaleidoscopedoll.render;

import com.github.ysbbbbbb.kaleidoscopedoll.client.render.JavaDollModels;
import com.github.ysbbbbbb.kaleidoscopedoll.entity.DollEntity;
import com.github.ysbbbbbb.kaleidoscopedoll.init.ModBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import org.apache.commons.lang3.StringUtils;
import org.joml.Vector3fc;

public class DollEntityRender extends EntityRenderer<DollEntity, DollEntityRender.State> {
    private final LegacyDollRender.Animation<DollEntity> animation = new LegacyDollRender.Animation<>("animated_root");
    private final ItemModelResolver items;

    public DollEntityRender(EntityRendererProvider.Context context) {
        super(context);
        items = context.getItemModelResolver();
    }

    public static final class State extends EntityRenderState {
        DollEntity doll;
        float partialTick;
        final ItemStackRenderState javaModel = new ItemStackRenderState();
        final ItemStackRenderState held = new ItemStackRenderState();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(DollEntity doll, State state, float partialTick) {
        super.extractRenderState(doll, state, partialTick);
        state.doll = doll;
        state.partialTick = partialTick;
        JavaDollModels.extractWorldModel(state.javaModel, doll.getCustomDollId());
        items.updateForNonLiving(state.held, doll.getHoldItem(), ItemDisplayContext.GROUND, doll);
    }

    @Override
    public void render(State state, PoseStack pose, MultiBufferSource buffers, int light) {
        DollEntity doll = state.doll;
        var facing = doll.getVehicle() == null ? doll : doll.getVehicle();
        pose.pushPose();
        translate(pose, doll.getDisplayTranslation());
        pose.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(state.partialTick, facing.yRotO, facing.getYRot())));
        pose.mulPose(Axis.XP.rotationDegrees(Mth.rotLerp(state.partialTick, doll.xRotO, doll.getXRot())));
        scale(pose, doll.getDisplayScale());
        animation.prepare(doll, doll.getId(), state.partialTick);
        animation.applyRoot(pose, 0);
        pose.translate(-0.5, 0, -0.5);

        var block = doll.getDisplayBlockState();
        String customId = doll.getCustomDollId();
        int overlay = OverlayTexture.NO_OVERLAY;
        if (!state.javaModel.isEmpty()) {
            JavaDollModels.renderCentered(state.javaModel, pose, buffers, light, overlay);
        } else if (StringUtils.isNotBlank(customId)) {
            LegacyDollRender.bedrock(customId, pose, buffers, light, overlay);
        } else if (!block.isAir()) {
            LegacyDollRender.block(block, pose, buffers, light, overlay);
        } else if (state.held.isEmpty()) {
            LegacyDollRender.block(ModBlocks.PURPLE_DOLL_GIFT_BOX.defaultBlockState(), pose, buffers, light, overlay);
        }

        if (!state.held.isEmpty()) {
            pose.pushPose();
            translate(pose, doll.getItemTranslation());
            scale(pose, doll.getItemScale());
            Vector3fc rotation = doll.getItemRotation();
            pose.mulPose(Axis.XP.rotationDegrees(rotation.x()));
            pose.mulPose(Axis.YP.rotationDegrees(rotation.y()));
            pose.mulPose(Axis.ZP.rotationDegrees(rotation.z()));
            state.held.render(pose, buffers, light, overlay);
            pose.popPose();
        }
        pose.popPose();
        super.render(state, pose, buffers, light);
    }

    private static void scale(PoseStack pose, Vector3fc scale) {
        pose.scale(scale.x(), scale.y(), scale.z());
    }

    private static void translate(PoseStack pose, Vector3fc vector) {
        pose.translate(vector.x(), vector.y(), vector.z());
    }
}
