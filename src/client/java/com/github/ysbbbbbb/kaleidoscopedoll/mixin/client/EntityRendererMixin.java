package com.github.ysbbbbbb.kaleidoscopedoll.mixin.client;

import com.github.ysbbbbbb.kaleidoscopedoll.client.render.DollRenderStateDuck;
import com.github.ysbbbbbb.kaleidoscopedoll.item.crafting.DollFunctionRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.AbstractWindCharge;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 风弹玩偶：提取渲染状态时把携带的玩偶物品解析进渲染状态，
 * WindChargeRenderer 据此把飞行中的风弹渲染成玩偶模型
 */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity, S extends EntityRenderState> {

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void kaleidoscopeDoll$extractWindChargeDoll(T entity, S state, float partialTicks, CallbackInfo ci) {
        if (!(state instanceof DollRenderStateDuck duck)) {
            return;
        }
        if (!(entity instanceof ItemSupplier supplier) || !(entity instanceof AbstractWindCharge)) {
            return;
        }
        ItemStack doll = supplier.getItem();
        if (!DollFunctionRecipe.isDollFunctionItem(doll)) {
            duck.kaleidoscopeDoll$clearDollItemState();
            return;
        }
        ItemModelResolver resolver = Minecraft.getInstance().getItemModelResolver();
        resolver.updateForNonLiving(duck.kaleidoscopeDoll$getDollItemState(), doll, ItemDisplayContext.GROUND, entity);
    }
}
