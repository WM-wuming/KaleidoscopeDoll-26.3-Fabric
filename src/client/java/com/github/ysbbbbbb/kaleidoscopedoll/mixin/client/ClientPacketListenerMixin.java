package com.github.ysbbbbbb.kaleidoscopedoll.mixin.client;

import com.github.ysbbbbbb.kaleidoscopedoll.client.DollTotemActivation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin implements DollTotemActivation {
    @Unique
    private ItemStack kaleidoscopeDoll$consumedTotem = ItemStack.EMPTY;

    @Override
    public void kaleidoscopeDoll$setConsumedTotem(ItemStack stack) {
        this.kaleidoscopeDoll$consumedTotem = stack;
    }

    @WrapOperation(method = "handleEntityEvent", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;findTotem(Lnet/minecraft/world/entity/player/Player;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack kaleidoscopeDoll$resolveActivatedTotem(Player player, Operation<ItemStack> original) {
        ItemStack consumedTotem = this.kaleidoscopeDoll$consumedTotem;
        this.kaleidoscopeDoll$consumedTotem = ItemStack.EMPTY;
        return consumedTotem.isEmpty() ? original.call(player) : consumedTotem;
    }
}
