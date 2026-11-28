package com.github.ysbbbbbb.kaleidoscopedoll.mixin.client;

import com.github.ysbbbbbb.kaleidoscopedoll.KaleidoscopeDoll;
import com.github.ysbbbbbb.kaleidoscopedoll.init.ModDataComponent;
import com.github.ysbbbbbb.kaleidoscopedoll.item.DollItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CustomHeadLayer.class)
public abstract class CustomHeadLayerMixin<T extends LivingEntity, M extends EntityModel<T> & HeadedModel> extends RenderLayer<T, M> {
    protected CustomHeadLayerMixin(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
            at = @At("HEAD"))
    private void kaleidoscopeDoll$renderHelmetDoll(PoseStack pose, MultiBufferSource buffers, int light, T entity,
            float limbAngle, float limbDistance, float partialTick, float age, float yaw, float pitch, CallbackInfo callback) {
        String path = entity.getItemBySlot(EquipmentSlot.HEAD).get(ModDataComponent.DOLL_ON_HELMET);
        if (path == null || path.isEmpty()) return;
        ResourceLocation id = ResourceLocation.tryBuild(KaleidoscopeDoll.MOD_ID, path);
        if (id == null || !(BuiltInRegistries.ITEM.get(id) instanceof DollItem doll)) return;

        pose.pushPose();
        getParentModel().getHead().translateAndRotate(pose);
        pose.translate(0, -2 / 16.0, 0);
        CustomHeadLayer.translateToHead(pose, false);
        Minecraft.getInstance().getItemRenderer().renderStatic(new ItemStack(doll), ItemDisplayContext.HEAD,
                light, OverlayTexture.NO_OVERLAY, pose, buffers, entity.level(), entity.getId());
        pose.popPose();
    }
}
