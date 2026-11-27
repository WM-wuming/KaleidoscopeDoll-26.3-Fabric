package com.github.ysbbbbbb.kaleidoscopedoll.mixin;

import com.github.ysbbbbbb.kaleidoscopedoll.entity.WindChargeDollCarrier;
import com.github.ysbbbbbb.kaleidoscopedoll.item.crafting.DollFunctionRecipe;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.WindChargeItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(WindChargeItem.class)
public abstract class WindChargeItemMixin {
    @ModifyArg(method = "use", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/projectile/Projectile;spawnProjectileFromRotation(Lnet/minecraft/world/entity/projectile/Projectile$ProjectileFactory;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;FFF)Lnet/minecraft/world/entity/projectile/Projectile;"),
            index = 0)
    private Projectile.ProjectileFactory<? extends Projectile> kaleidoscopeDoll$copyThrownModel(
            Projectile.ProjectileFactory<? extends Projectile> factory) {
        return (level, owner, stack) -> kaleidoscopeDoll$copyModel(factory.create(level, owner, stack), stack);
    }

    @ModifyReturnValue(method = "asProjectile", at = @At("RETURN"))
    private Projectile kaleidoscopeDoll$copyDispensedModel(Projectile projectile, @Local(argsOnly = true) ItemStack stack) {
        return kaleidoscopeDoll$copyModel(projectile, stack);
    }

    @Unique
    private static Projectile kaleidoscopeDoll$copyModel(Projectile projectile, ItemStack stack) {
        if (projectile instanceof WindChargeDollCarrier carrier && DollFunctionRecipe.isDollFunctionItem(stack)) {
            carrier.kaleidoscopeDoll$setDollItem(stack.copyWithCount(1));
        }
        return projectile;
    }
}
