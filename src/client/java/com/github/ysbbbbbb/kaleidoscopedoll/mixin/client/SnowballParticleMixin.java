package com.github.ysbbbbbb.kaleidoscopedoll.mixin.client;

import com.github.ysbbbbbb.kaleidoscopedoll.item.crafting.DollFunctionRecipe;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 玩偶雪球砸碎时播放原版雪球的碎屑粒子，而不是玩偶图标粒子
 */
@Mixin(Snowball.class)
public class SnowballParticleMixin {
    @Inject(method = "getParticle", at = @At("HEAD"), cancellable = true)
    private void kaleidoscopeDoll$vanillaSnowballParticle(CallbackInfoReturnable<ParticleOptions> cir) {
        ItemStack stack = ((Snowball) (Object) this).getItem();
        if (DollFunctionRecipe.isDollFunctionItem(stack)) {
            cir.setReturnValue(new ItemParticleOption(ParticleTypes.ITEM, new ItemStackTemplate(Items.SNOWBALL)));
        }
    }
}
