package com.github.ysbbbbbb.kaleidoscopedoll.mixin.client;

import com.github.ysbbbbbb.kaleidoscopedoll.client.render.DollRenderStateDuck;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CustomHeadLayer.class)
public class CustomHeadLayerMixin {
    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;FF)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/layers/CustomHeadLayer;translateToHead(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/entity/layers/CustomHeadLayer$Transforms;)V"))
    private void liftDoll(PoseStack pose, MultiBufferSource buffers, int light, LivingEntityRenderState state,
                          float yaw, float pitch, CallbackInfo callback) {
        if (((DollRenderStateDuck) state).kaleidoscopeDoll$isDollAboveHelmet()) {
            pose.translate(0, -2 / 16.0, 0);
        }
    }
}
