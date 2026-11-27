package com.github.ysbbbbbb.kaleidoscopedoll.mixin.client;

import com.github.ysbbbbbb.kaleidoscopedoll.KaleidoscopeDoll;
import com.github.ysbbbbbb.kaleidoscopedoll.client.render.DollRenderStateDuck;
import com.github.ysbbbbbb.kaleidoscopedoll.init.ModDataComponent;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.Equippable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 头部装备带 doll_on_helmet 组件时，把玩偶物品塞进 headItem 渲染状态：
 * 头盔本体走 HumanoidArmorLayer（assetId）渲染，玩偶走 CustomHeadLayer 渲染，两者叠加。
 * 同时给渲染状态打上"头盔+玩偶"标记，供 CustomHeadLayerMixin 调整玩偶位置。
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, S extends LivingEntityRenderState> {
    @Shadow @Final protected ItemModelResolver itemModelResolver;

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void kaleidoscopeDoll$renderDollOnHelmet(T entity, S state, float partialTicks, CallbackInfo ci) {
        if (!(state instanceof DollRenderStateDuck duckState)) {
            return;
        }
        ItemStack head = entity.getItemBySlot(EquipmentSlot.HEAD);
        String dollPath = head.get(ModDataComponent.DOLL_ON_HELMET);
        if (dollPath == null || dollPath.isEmpty()) {
            duckState.kaleidoscopeDoll$setDollAboveHelmet(false);
            return;
        }
        // 是否为真正的头盔（有 assetId 的装备，走盔甲层渲染）：只有这种情况才需要上移避让
        boolean isHelmet = head.get(DataComponents.EQUIPPABLE) instanceof Equippable equippable
                && equippable.assetId().isPresent();
        duckState.kaleidoscopeDoll$setDollAboveHelmet(isHelmet);

        Item dollItem = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(KaleidoscopeDoll.MOD_ID, dollPath));
        if (dollItem == null || dollItem == Items.AIR) {
            return;
        }
        this.itemModelResolver.updateForLiving(state.headItem, new ItemStack(dollItem), ItemDisplayContext.HEAD, entity);
    }
}
