package com.kaleidoscopedoll.mixin.client;

import com.kaleidoscopedoll.client.render.DollLayerPose;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemStackRenderState.LayerRenderState.class)
public class DollLayerPoseMixin implements DollLayerPose {
    @Unique private ItemTransform kaleidoscopeDoll$transform;

    @Override
    public void kaleidoscopeDoll$setTransform(ItemTransform transform) {
        kaleidoscopeDoll$transform = transform;
    }

    @Inject(method = "clear", at = @At("HEAD"))
    private void clearTransform(CallbackInfo callback) {
        kaleidoscopeDoll$transform = null;
    }

    @ModifyReturnValue(method = "transform", at = @At("RETURN"))
    private ItemTransform overrideTransform(ItemTransform original) {
        return kaleidoscopeDoll$transform == null ? original : kaleidoscopeDoll$transform;
    }
}
