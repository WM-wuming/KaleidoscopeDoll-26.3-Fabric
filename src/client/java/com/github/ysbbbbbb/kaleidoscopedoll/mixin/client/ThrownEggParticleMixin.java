package com.github.ysbbbbbb.kaleidoscopedoll.mixin.client;

import com.github.ysbbbbbb.kaleidoscopedoll.item.crafting.DollFunctionRecipe;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEgg;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * 玩偶鸡蛋砸碎时播放原版鸡蛋的蛋壳粒子
 */
@Mixin(ThrownEgg.class)
public class ThrownEggParticleMixin {
    @ModifyArg(method = "handleEntityEvent", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/core/particles/ItemParticleOption;<init>(Lnet/minecraft/core/particles/ParticleType;Lnet/minecraft/world/item/ItemStackTemplate;)V"),
            index = 1)
    private ItemStackTemplate kaleidoscopeDoll$vanillaEggParticle(ItemStackTemplate template) {
        ItemStack stack = ((ThrownEgg) (Object) this).getItem();
        if (DollFunctionRecipe.isDollFunctionItem(stack)) {
            return new ItemStackTemplate(stack.getItem());
        }
        return template;
    }
}
