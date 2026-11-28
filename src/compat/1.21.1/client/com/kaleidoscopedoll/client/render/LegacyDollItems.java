package com.kaleidoscopedoll.client.render;

import com.kaleidoscopedoll.init.ModItems;
import com.kaleidoscopedoll.item.CustomDollItem;
import com.kaleidoscopedoll.item.DollEntityItem;
import com.kaleidoscopedoll.render.LegacyDollRender;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import org.apache.commons.lang3.StringUtils;

public final class LegacyDollItems {
    private LegacyDollItems() {
    }

    public static void register() {
        JavaDollModels.register();
        BuiltinItemRendererRegistry.INSTANCE.register(ModItems.CUSTOM_DOLL, LegacyDollItems::renderCustom);
        BuiltinItemRendererRegistry.INSTANCE.register(ModItems.DOLL_ENTITY_ITEM, LegacyDollItems::renderEntity);
    }

    private static void renderCustom(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                                     MultiBufferSource buffers, int light, int overlay) {
        String id = CustomDollItem.getModelId(stack);
        if (!JavaDollModels.render(id, ItemDisplayContext.NONE, pose, buffers, light, overlay)) {
            LegacyDollRender.bedrock(id, pose, buffers, light, overlay);
        }
    }

    private static void renderEntity(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                                     MultiBufferSource buffers, int light, int overlay) {
        String id = DollEntityItem.getCustomDollIdFromItemStack(stack);
        if (StringUtils.isNotBlank(id)) {
            if (!JavaDollModels.render(id, ItemDisplayContext.GUI, pose, buffers, light, overlay)) {
                LegacyDollRender.bedrock(id, pose, buffers, light, overlay);
            }
            return;
        }
        var block = DollEntityItem.getBlockFromItemStack(stack);
        var model = block == Blocks.AIR ? ModItems.PURPLE_DOLL_GIFT_BOX : block.asItem();
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        Minecraft client = Minecraft.getInstance();
        client.getItemRenderer().renderStatic(new ItemStack(model), ItemDisplayContext.GUI, light, overlay, pose, buffers, client.level, 0);
        pose.popPose();
    }
}
