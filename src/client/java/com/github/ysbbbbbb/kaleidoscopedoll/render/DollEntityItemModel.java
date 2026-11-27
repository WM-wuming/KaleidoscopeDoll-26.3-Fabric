package com.github.ysbbbbbb.kaleidoscopedoll.render;

import com.github.ysbbbbbb.kaleidoscopedoll.init.ModItems;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public record DollEntityItemModel(ItemModel original) implements ItemModel {
    public static final Identifier ID = BuiltInRegistries.ITEM.getKey(ModItems.DOLL_ENTITY_ITEM);

    @Override
    public void update(ItemStackRenderState state, ItemStack stack, ItemModelResolver resolver,
                       ItemDisplayContext context, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
        boolean inHand = context.firstPerson() || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                || context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
        if (stack.is(ModItems.DOLL_ENTITY_ITEM) && (inHand || context == ItemDisplayContext.GUI)) {
            ItemStack displayStack = DollEntitySpecialRenderer.createDisplayStack(stack);
            // Custom dolls need the GUI transform from their own item model.
            if ((inHand || displayStack.is(ModItems.CUSTOM_DOLL))
                    && !displayStack.isEmpty() && !displayStack.is(ModItems.DOLL_ENTITY_ITEM)) {
                resolver.appendItemLayers(state, displayStack, context, level, owner, seed);
                return;
            }
        }

        original.update(state, stack, resolver, context, level, owner, seed);
    }
}
