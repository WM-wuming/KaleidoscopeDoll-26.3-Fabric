package com.kaleidoscopedoll.mixin;

import com.kaleidoscopedoll.entity.WindChargeDollCarrier;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.projectile.hurtingprojectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.AbstractWindCharge;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 为风弹注册玩偶物品堆的同步数据槽（仅风弹实例定义，其它弹射物不受影响）
 */
@Mixin(AbstractHurtingProjectile.class)
public abstract class AbstractHurtingProjectileMixin {

    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void kaleidoscopeDoll$defineWindChargeDollItem(SynchedEntityData.Builder builder, CallbackInfo ci) {
        if ((Object) this instanceof AbstractWindCharge) {
            builder.define(WindChargeDollCarrier.DOLL_ITEM, ItemStack.EMPTY);
        }
    }
}
