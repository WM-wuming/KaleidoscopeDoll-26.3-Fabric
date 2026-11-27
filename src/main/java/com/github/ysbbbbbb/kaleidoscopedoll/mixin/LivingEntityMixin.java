package com.github.ysbbbbbb.kaleidoscopedoll.mixin;

import com.github.ysbbbbbb.kaleidoscopedoll.item.crafting.DollFunctionRecipe;
import com.github.ysbbbbbb.kaleidoscopedoll.network.NetworkHandler.DollTotemActivationPayload;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DeathProtection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @WrapOperation(method = "checkTotemDeathProtection", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/component/DeathProtection;applyEffects(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;)V"))
    private void kaleidoscopeDoll$sendConsumedTotem(DeathProtection protection, ItemStack consumedStack,
                                                   LivingEntity entity, Operation<Void> original) {
        original.call(protection, consumedStack, entity);
        // This is the pre-consumption copy, sent just before vanilla's activation event.
        if (entity instanceof ServerPlayer player && DollFunctionRecipe.isDollTotem(consumedStack)
                && ServerPlayNetworking.canSend(player, DollTotemActivationPayload.TYPE)) {
            ServerPlayNetworking.send(player, new DollTotemActivationPayload(consumedStack.copyWithCount(1)));
        }
    }
}
