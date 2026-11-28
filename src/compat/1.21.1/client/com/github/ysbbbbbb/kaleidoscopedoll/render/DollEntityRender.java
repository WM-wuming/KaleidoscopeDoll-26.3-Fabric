package com.github.ysbbbbbb.kaleidoscopedoll.render;

import com.github.ysbbbbbb.kaleidoscopedoll.client.render.JavaDollModels;
import com.github.ysbbbbbb.kaleidoscopedoll.entity.DollEntity;
import com.github.ysbbbbbb.kaleidoscopedoll.init.ModBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import org.apache.commons.lang3.StringUtils;
import org.joml.Vector3fc;

public class DollEntityRender extends EntityRenderer<DollEntity> {
    private final LegacyDollRender.Animation<DollEntity> animation = new LegacyDollRender.Animation<>("animated_root");

    public DollEntityRender(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(DollEntity doll, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        var facing = doll.getVehicle() == null ? doll : doll.getVehicle();
        pose.pushPose();
        translate(pose, doll.getDisplayTranslation());
        pose.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(partialTick, facing.yRotO, facing.getYRot())));
        pose.mulPose(Axis.XP.rotationDegrees(Mth.rotLerp(partialTick, doll.xRotO, doll.getXRot())));
        scale(pose, doll.getDisplayScale());
        animation.prepare(doll, doll.getId(), partialTick);
        animation.applyRoot(pose, 0);
        pose.translate(-0.5, 0, -0.5);

        var block = doll.getDisplayBlockState();
        var held = doll.getHoldItem();
        String customId = doll.getCustomDollId();
        int overlay = OverlayTexture.NO_OVERLAY;
        if (StringUtils.isNotBlank(customId)) {
            if (!JavaDollModels.render(customId, ItemDisplayContext.NONE, pose, buffers, light, overlay)) {
                LegacyDollRender.bedrock(customId, pose, buffers, light, overlay);
            }
        } else if (!block.isAir()) {
            LegacyDollRender.block(block, pose, buffers, light, overlay);
        } else if (held.isEmpty()) {
            LegacyDollRender.block(ModBlocks.PURPLE_DOLL_GIFT_BOX.defaultBlockState(), pose, buffers, light, overlay);
        }

        if (!held.isEmpty()) {
            pose.pushPose();
            translate(pose, doll.getItemTranslation());
            scale(pose, doll.getItemScale());
            Vector3fc rotation = doll.getItemRotation();
            pose.mulPose(Axis.XP.rotationDegrees(rotation.x()));
            pose.mulPose(Axis.YP.rotationDegrees(rotation.y()));
            pose.mulPose(Axis.ZP.rotationDegrees(rotation.z()));
            Minecraft.getInstance().getItemRenderer().renderStatic(held, ItemDisplayContext.GROUND, light, overlay, pose, buffers, doll.level(), doll.getId());
            pose.popPose();
        }
        pose.popPose();
        super.render(doll, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(DollEntity doll) {
        return MissingTextureAtlasSprite.getLocation();
    }

    private static void scale(PoseStack pose, Vector3fc scale) {
        pose.scale(scale.x(), scale.y(), scale.z());
    }

    private static void translate(PoseStack pose, Vector3fc vector) {
        pose.translate(vector.x(), vector.y(), vector.z());
    }
}
