package com.github.ysbbbbbb.kaleidoscopedoll.mixin;

import com.github.ysbbbbbb.kaleidoscopedoll.entity.WindChargeDollCarrier;
import net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.AbstractWindCharge;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 风弹玩偶：getItem 返回携带的玩偶物品堆，供客户端渲染成飞行玩偶
 */
@Mixin(AbstractWindCharge.class)
public abstract class AbstractWindChargeMixin implements WindChargeDollCarrier {

    @Inject(method = "getItem", at = @At("HEAD"), cancellable = true)
    private void kaleidoscopeDoll$getDollItem(CallbackInfoReturnable<ItemStack> cir) {
        ItemStack doll = this.kaleidoscopeDoll$getDollItem();
        if (!doll.isEmpty()) {
            cir.setReturnValue(doll);
        }
    }

    @Override
    public void kaleidoscopeDoll$setDollItem(ItemStack stack) {
        ((EntityAccessor) (Object) this).kaleidoscopeDoll$getEntityData().set(WindChargeDollCarrier.DOLL_ITEM, stack);
    }

    @Override
    public ItemStack kaleidoscopeDoll$getDollItem() {
        return ((EntityAccessor) (Object) this).kaleidoscopeDoll$getEntityData().get(WindChargeDollCarrier.DOLL_ITEM);
    }
}
