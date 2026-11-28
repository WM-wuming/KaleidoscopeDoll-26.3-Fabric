package com.kaleidoscopedoll.client.render;

import com.kaleidoscopedoll.KaleidoscopeDoll;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public record CustomDollItemModel(ItemModel original) implements ItemModel {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(KaleidoscopeDoll.MOD_ID, "custom_doll");

    @Override
    public void update(ItemStackRenderState state, ItemStack stack, ItemModelResolver resolver,
                       ItemDisplayContext context, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
        ItemStack display = JavaDollModels.withDollModel(stack);
        if (display != stack) {
            resolver.appendItemLayers(state, display, context, level, owner, seed);
        } else {
            original.update(state, stack, resolver, context, level, owner, seed);
        }
    }
}
