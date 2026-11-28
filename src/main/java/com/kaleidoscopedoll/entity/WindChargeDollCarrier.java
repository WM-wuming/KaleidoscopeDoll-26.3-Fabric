package com.kaleidoscopedoll.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.AbstractWindCharge;
import net.minecraft.world.item.ItemStack;

/**
 * 风弹玩偶：风弹实体携带的玩偶物品堆（同步到客户端，用于渲染成飞行玩偶）
 */
public interface WindChargeDollCarrier {
    EntityDataAccessor<ItemStack> DOLL_ITEM =
            SynchedEntityData.defineId(AbstractWindCharge.class, EntityDataSerializers.ITEM_STACK);

    void kaleidoscopeDoll$setDollItem(ItemStack stack);

    ItemStack kaleidoscopeDoll$getDollItem();
}
