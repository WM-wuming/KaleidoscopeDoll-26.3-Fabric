package com.kaleidoscopedoll.mixin.client;

import com.kaleidoscopedoll.client.render.DollLayerPose;
import com.kaleidoscopedoll.item.crafting.DollFunctionRecipe;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.item.BlockModelWrapper;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BlockModelWrapper.class)
public class CuboidItemModelWrapperMixin {
    @Unique private static final ItemTransform kaleidoscopeDoll$firstPerson = new ItemTransform(
            new Vector3f(-5, 155, -5), new Vector3f(1f / 16, 3f / 16, 1f / 16), new Vector3f(0.4f));
    @Unique private static final ItemTransform kaleidoscopeDoll$thirdPerson = new ItemTransform(
            new Vector3f(85, 25, 0), new Vector3f(0, 3f / 16, 1f / 16), new Vector3f(0.375f));

    @ModifyExpressionValue(method = "update", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;newLayer()Lnet/minecraft/client/renderer/item/ItemStackRenderState$LayerRenderState;"))
    private ItemStackRenderState.LayerRenderState poseDoll(ItemStackRenderState.LayerRenderState layer,
            @Local(argsOnly = true) ItemStack stack, @Local(argsOnly = true) ItemDisplayContext context) {
        if (DollFunctionRecipe.isDollFunctionItem(stack)) {
            ItemTransform transform = switch (context) {
                case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> kaleidoscopeDoll$firstPerson;
                case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND -> kaleidoscopeDoll$thirdPerson;
                default -> null;
            };
            ((DollLayerPose) (Object) layer).kaleidoscopeDoll$setTransform(transform);
        }
        return layer;
    }
}
