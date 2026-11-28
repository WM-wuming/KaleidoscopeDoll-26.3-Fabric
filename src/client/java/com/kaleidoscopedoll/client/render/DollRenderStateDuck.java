package com.kaleidoscopedoll.client.render;

import net.minecraft.client.renderer.item.ItemStackRenderState;

/**
 * 给 EntityRenderState 附加的渲染标记（基类 Mixin，所有实体渲染状态共享）：
 * 1. dollAboveHelmet：头部同时佩戴头盔（assetId 装备）和玩偶组件时为 true，
 *    CustomHeadLayer 据此调整玩偶位置，避免和头盔模型穿插；
 * 2. dollItemState：风弹玩偶解析好的物品渲染状态，WindChargeRenderer 据此把
 *    飞行中的风弹渲染成玩偶模型。
 * 注意：duck 接口不能放在 mixin 包下（mixin 包的类不允许被普通代码直接引用）。
 */
public interface DollRenderStateDuck {
    void kaleidoscopeDoll$setDollAboveHelmet(boolean value);

    boolean kaleidoscopeDoll$isDollAboveHelmet();

    ItemStackRenderState kaleidoscopeDoll$getDollItemState();

    void kaleidoscopeDoll$clearDollItemState();
}
