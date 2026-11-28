package com.kaleidoscopedoll.mixin.client;

import com.kaleidoscopedoll.entity.WindChargeDollCarrier;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.WindChargeRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.projectile.windcharge.AbstractWindCharge;
import net.minecraft.world.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WindChargeRenderer.class)
public class WindChargeRendererMixin {
    @Inject(method = "render(Lnet/minecraft/world/entity/projectile/windcharge/AbstractWindCharge;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"), cancellable = true)
    private void kaleidoscopeDoll$renderDoll(AbstractWindCharge projectile, float yaw, float partialTick, PoseStack pose,
                                            MultiBufferSource buffers, int light, CallbackInfo callback) {
        var item = ((WindChargeDollCarrier) projectile).kaleidoscopeDoll$getDollItem();
        if (item.isEmpty()) return;
        Minecraft client = Minecraft.getInstance();
        pose.pushPose();
        pose.mulPose(client.getEntityRenderDispatcher().cameraOrientation());
        client.getItemRenderer().renderStatic(item, ItemDisplayContext.GROUND, light, OverlayTexture.NO_OVERLAY,
                pose, buffers, projectile.level(), projectile.getId());
        pose.popPose();
        callback.cancel();
    }
}
