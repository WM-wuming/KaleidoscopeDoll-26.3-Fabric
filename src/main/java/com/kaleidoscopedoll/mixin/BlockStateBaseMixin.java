package com.kaleidoscopedoll.mixin;

import com.kaleidoscopedoll.init.ModItems;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateBaseMixin {
    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void kaleidoscopeDoll$skipBlockUseWhenHoldingDoll(ItemStack stack, Level level, Player player,
                                                              InteractionHand hand, BlockHitResult hit,
                                                              CallbackInfoReturnable<InteractionResult> callback) {
        if (player.isHolding(ModItems.DOLL_ENTITY_ITEM)) {
            // PASS skips the block's empty-hand fallback but still lets the player use the item.
            callback.setReturnValue(InteractionResult.PASS);
        }
    }
}
