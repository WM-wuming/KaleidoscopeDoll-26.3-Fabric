package com.github.ysbbbbbb.kaleidoscopedoll.mixin.client;

import com.github.ysbbbbbb.kaleidoscopedoll.item.crafting.DollFunctionRecipe;
import net.minecraft.client.renderer.ItemModelShaper;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {
    @Shadow public abstract ItemModelShaper getItemModelShaper();

    @Inject(method = "getModel", at = @At("HEAD"), cancellable = true)
    private void kaleidoscopeDoll$functionModel(ItemStack stack, Level level, LivingEntity holder, int seed,
                                               CallbackInfoReturnable<BakedModel> callback) {
        if (DollFunctionRecipe.isDollFunctionItem(stack)) {
            var doll = BuiltInRegistries.ITEM.get(DollFunctionRecipe.getDollModel(stack));
            callback.setReturnValue(getItemModelShaper().getItemModel(new ItemStack(doll)));
        }
    }
}
