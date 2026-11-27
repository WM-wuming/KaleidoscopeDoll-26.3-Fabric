package com.github.ysbbbbbb.kaleidoscopedoll.mixin.client;

import com.github.ysbbbbbb.kaleidoscopedoll.client.render.DollRenderStateDuck;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CustomHeadLayer.class)
public class CustomHeadLayerMixin {
    @Inject(
        method = "submit",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/layers/CustomHeadLayer;translateToHead(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/entity/layers/CustomHeadLayer$Transforms;)V"
        )
    )
    private void kaleidoscopeDoll$liftDollAboveHelmet(
        PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords,
        LivingEntityRenderState state,
        float yRot, float xRot, CallbackInfo ci
    ) {
        if (!((DollRenderStateDuck) state).kaleidoscopeDoll$isDollAboveHelmet()) {
            return;
        }

        double lift = 2.0 / 16.0;
        if (state instanceof AvatarRenderState) {
            AABB bounds = state.headItem.getModelBoundingBox();
            if (bounds.minY < bounds.maxY) {
                // 玩家盔顶高 9 像素；原版头戴物品原点高 4 像素，再以 0.625 缩放模型。
                lift = (9.0 - 4.0) / 16.0 - bounds.minY * 0.625;
            }
        }
        poseStack.translate(0.0, -lift, 0.0);
    }
}
