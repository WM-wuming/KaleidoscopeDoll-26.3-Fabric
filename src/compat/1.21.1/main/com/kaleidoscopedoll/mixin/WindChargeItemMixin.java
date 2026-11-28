package com.kaleidoscopedoll.mixin;

import com.kaleidoscopedoll.entity.WindChargeDollCarrier;
import com.kaleidoscopedoll.item.crafting.DollFunctionRecipe;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.WindChargeItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(WindChargeItem.class)
public abstract class WindChargeItemMixin {
    @ModifyArg(method = "use", index = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private Entity kaleidoscopeDoll$copyThrownModel(Entity projectile,
            @Local(argsOnly = true) Player player, @Local(argsOnly = true) InteractionHand hand) {
        kaleidoscopeDoll$copyModel(projectile, player.getItemInHand(hand));
        return projectile;
    }

    @ModifyReturnValue(method = "asProjectile", at = @At("RETURN"))
    private Projectile kaleidoscopeDoll$copyDispensedModel(Projectile projectile, @Local(argsOnly = true) ItemStack stack) {
        kaleidoscopeDoll$copyModel(projectile, stack);
        return projectile;
    }

    @Unique
    private static void kaleidoscopeDoll$copyModel(Entity projectile, ItemStack stack) {
        if (projectile instanceof WindChargeDollCarrier carrier && DollFunctionRecipe.isDollFunctionItem(stack)) {
            carrier.kaleidoscopeDoll$setDollItem(stack.copyWithCount(1));
        }
    }
}
