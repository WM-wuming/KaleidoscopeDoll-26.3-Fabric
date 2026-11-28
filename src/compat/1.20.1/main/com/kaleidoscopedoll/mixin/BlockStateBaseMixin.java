package com.kaleidoscopedoll.mixin;

import com.kaleidoscopedoll.init.ModItems;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateBaseMixin {
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void kaleidoscopeDoll$skipBlockUse(Level level, Player player, InteractionHand hand, BlockHitResult hit,
                                              CallbackInfoReturnable<InteractionResult> callback) {
        if (player.getItemInHand(hand).is(ModItems.DOLL_ENTITY_ITEM)) {
            callback.setReturnValue(InteractionResult.PASS);
        }
    }
}
