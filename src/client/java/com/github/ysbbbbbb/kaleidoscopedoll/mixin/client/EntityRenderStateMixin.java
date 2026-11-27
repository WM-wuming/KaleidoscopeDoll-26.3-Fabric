package com.github.ysbbbbbb.kaleidoscopedoll.mixin.client;

import com.github.ysbbbbbb.kaleidoscopedoll.client.render.DollRenderStateDuck;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * 基类渲染状态 Mixin：标记与玩偶物品渲染状态对所有实体渲染状态可用
 */
@Mixin(EntityRenderState.class)
public class EntityRenderStateMixin implements DollRenderStateDuck {
    @Unique
    private boolean kaleidoscopeDoll$dollAboveHelmet;

    @Unique
    private final ItemStackRenderState kaleidoscopeDoll$dollItemState = new ItemStackRenderState();

    @Override
    public void kaleidoscopeDoll$setDollAboveHelmet(boolean value) {
        this.kaleidoscopeDoll$dollAboveHelmet = value;
    }

    @Override
    public boolean kaleidoscopeDoll$isDollAboveHelmet() {
        return this.kaleidoscopeDoll$dollAboveHelmet;
    }

    @Override
    public ItemStackRenderState kaleidoscopeDoll$getDollItemState() {
        return this.kaleidoscopeDoll$dollItemState;
    }

    @Override
    public void kaleidoscopeDoll$clearDollItemState() {
        this.kaleidoscopeDoll$dollItemState.clear();
    }
}
