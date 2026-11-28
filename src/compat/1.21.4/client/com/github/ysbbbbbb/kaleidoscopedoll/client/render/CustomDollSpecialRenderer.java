package com.github.ysbbbbbb.kaleidoscopedoll.client.render;

import com.github.ysbbbbbb.kaleidoscopedoll.item.CustomDollItem;
import com.github.ysbbbbbb.kaleidoscopedoll.render.LegacyDollRender;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class CustomDollSpecialRenderer implements SpecialModelRenderer<ItemStack> {
    @Override
    public ItemStack extractArgument(ItemStack stack) {
        return stack;
    }

    @Override
    public void render(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers,
                       int light, int overlay, boolean hasFoil) {
        if (stack == null || stack.isEmpty()) return;
        ItemStack display = JavaDollModels.withDollModel(stack);
        if (stack == display) {
            LegacyDollRender.bedrock(CustomDollItem.getModelId(stack), pose, buffers, light, overlay);
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        var model = new ItemStackRenderState();
        minecraft.getItemModelResolver().updateForTopItem(model, display, ItemDisplayContext.NONE, false, minecraft.level, null, 0);
        JavaDollModels.renderCentered(model, pose, buffers, light, overlay);
    }

    public record Unbaked() implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());

        @Override
        public SpecialModelRenderer<ItemStack> bake(EntityModelSet modelSet) {
            return new CustomDollSpecialRenderer();
        }

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
