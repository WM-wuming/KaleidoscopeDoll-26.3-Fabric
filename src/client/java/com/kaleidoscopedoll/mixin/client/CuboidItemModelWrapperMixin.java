package com.kaleidoscopedoll.mixin.client;

import com.kaleidoscopedoll.item.crafting.DollFunctionRecipe;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.item.ItemStackRenderState.LayerRenderState;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(CuboidItemModelWrapper.class)
public abstract class CuboidItemModelWrapperMixin {
    @Unique
    private static final ItemTransform kaleidoscopeDoll$firstPersonDoll = new ItemTransform(
            new Vector3f(-5, 155, -5), new Vector3f(1.0f / 16, 3.0f / 16, 1.0f / 16), new Vector3f(0.4f));
    @Unique
    private static final ItemTransform kaleidoscopeDoll$thirdPersonDoll = new ItemTransform(
            new Vector3f(85, 25, 0), new Vector3f(0, 3.0f / 16, 1.0f / 16), new Vector3f(0.375f));

    @WrapOperation(method = "update", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/item/ModelRenderProperties;applyToLayer(Lnet/minecraft/client/renderer/item/ItemStackRenderState$LayerRenderState;Lnet/minecraft/world/item/ItemDisplayContext;)V"))
    private void kaleidoscopeDoll$poseHeldDoll(ModelRenderProperties properties, LayerRenderState layer,
                                               ItemDisplayContext context, Operation<Void> original,
                                               @Local(argsOnly = true) ItemStack stack) {
        original.call(properties, layer, context);
        if (DollFunctionRecipe.isDollFunctionItem(stack)) {
            switch (context) {
                case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> layer.setItemTransform(kaleidoscopeDoll$firstPersonDoll);
                case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND -> layer.setItemTransform(kaleidoscopeDoll$thirdPersonDoll);
                default -> {}
            }
        }
    }
}
