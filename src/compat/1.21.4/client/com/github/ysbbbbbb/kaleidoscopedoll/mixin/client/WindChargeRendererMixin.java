package com.github.ysbbbbbb.kaleidoscopedoll.mixin.client;

import com.github.ysbbbbbb.kaleidoscopedoll.client.render.DollRenderStateDuck;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.WindChargeRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WindChargeRenderer.class)
public class WindChargeRendererMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void renderDoll(EntityRenderState state, PoseStack pose, MultiBufferSource buffers, int light, CallbackInfo callback) {
        var item = ((DollRenderStateDuck) state).kaleidoscopeDoll$getDollItemState();
        if (item.isEmpty()) return;
        pose.pushPose();
        pose.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        item.render(pose, buffers, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
        callback.cancel();
    }
}
