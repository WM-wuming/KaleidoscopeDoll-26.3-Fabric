package com.kaleidoscopedoll.render;

import com.kaleidoscopedoll.client.render.CustomDollSpecialRenderer;
import com.kaleidoscopedoll.init.ModItems;
import com.kaleidoscopedoll.item.CustomDollItem;
import com.kaleidoscopedoll.item.DollEntityItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import org.apache.commons.lang3.StringUtils;

public class DollEntitySpecialRenderer implements SpecialModelRenderer<ItemStack> {
    private final CustomDollSpecialRenderer custom = new CustomDollSpecialRenderer();

    @Override
    public ItemStack extractArgument(ItemStack stack) {
        String id = DollEntityItem.getCustomDollIdFromItemStack(stack);
        if (StringUtils.isNotBlank(id)) {
            ItemStack display = new ItemStack(ModItems.CUSTOM_DOLL);
            CustomDollItem.setModelId(display, id);
            return display;
        }
        var block = DollEntityItem.getBlockFromItemStack(stack);
        return block == Blocks.AIR ? new ItemStack(ModItems.PURPLE_DOLL_GIFT_BOX) : new ItemStack(block);
    }

    @Override
    public void render(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers,
                       int light, int overlay, boolean hasFoil) {
        if (stack == null || stack.isEmpty()) return;
        if (stack.is(ModItems.CUSTOM_DOLL)) {
            custom.render(stack, context, pose, buffers, light, overlay, hasFoil);
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        var model = new ItemStackRenderState();
        minecraft.getItemModelResolver().updateForTopItem(model, stack, ItemDisplayContext.GUI, false, minecraft.level, null, 0);
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        model.render(pose, buffers, light, overlay);
        pose.popPose();
    }

    public record Unbaked() implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());

        @Override
        public SpecialModelRenderer<ItemStack> bake(EntityModelSet modelSet) {
            return new DollEntitySpecialRenderer();
        }

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
