package com.github.ysbbbbbb.kaleidoscopedoll.mixin;

import com.github.ysbbbbbb.kaleidoscopedoll.item.crafting.DollFunctionRecipe;
import com.github.ysbbbbbb.kaleidoscopedoll.network.NetworkHandler.DollTotemActivationPayload;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @WrapOperation(method = "checkTotemDeathProtection", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;shrink(I)V"))
    private void kaleidoscopeDoll$sendConsumedTotem(ItemStack stack, int count, Operation<Void> original) {
        if ((Object) this instanceof ServerPlayer player && DollFunctionRecipe.isDollTotem(stack)
                && ServerPlayNetworking.canSend(player, DollTotemActivationPayload.TYPE)) {
            ServerPlayNetworking.send(player, new DollTotemActivationPayload(stack.copyWithCount(1)));
        }
        original.call(stack, count);
    }
}
