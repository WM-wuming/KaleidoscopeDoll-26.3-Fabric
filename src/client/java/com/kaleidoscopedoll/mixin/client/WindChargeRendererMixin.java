package com.kaleidoscopedoll.mixin.client;

import com.kaleidoscopedoll.compat.DollPoseStack;
import com.kaleidoscopedoll.client.render.DollRenderStateDuck;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.WindChargeRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 风弹玩偶：携带玩偶物品堆时，把风弹渲染成朝向镜头的玩偶模型（替代原版风纹模型）
 */
@Mixin(WindChargeRenderer.class)
public class WindChargeRendererMixin {
    @Inject(method = "submit", at = @At("HEAD"), cancellable = true)
    private void kaleidoscopeDoll$renderDollModel(
        EntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
        CameraRenderState camera, CallbackInfo ci
    ) {
        if (!(state instanceof DollRenderStateDuck duck)) {
            return;
        }
        if (duck.kaleidoscopeDoll$getDollItemState().isEmpty()) {
            return;
        }
        poseStack.pushPose();
        DollPoseStack.rotate(poseStack, camera.orientation);
        duck.kaleidoscopeDoll$getDollItemState().submit(
            poseStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor
        );
        poseStack.popPose();
        ci.cancel();
    }
}
