package com.kaleidoscopedoll.item.crafting;

import com.kaleidoscopedoll.init.ModRecipes;
import com.kaleidoscopedoll.item.DollEntityItem;
import com.kaleidoscopedoll.item.DollItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class DollEntityCraftingRecipe extends CustomRecipe {
    public DollEntityCraftingRecipe() {
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        boolean hasDollItem = false;
        boolean hasSlimeBall = false;
        int itemCount = 0;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (!stack.isEmpty()) {
                itemCount++;
                if (stack.getItem() instanceof DollItem) {
                    hasDollItem = true;
                } else if (stack.is(Items.SLIME_BALL)) {
                    hasSlimeBall = true;
                } else {
                    return false;
                }
            }
        }

        return itemCount == 2 && hasDollItem && hasSlimeBall;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        ItemStack dollItemStack = ItemStack.EMPTY;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.getItem() instanceof DollItem) {
                dollItemStack = stack;
                break;
            }
        }
        if (dollItemStack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        DollItem dollItem = (DollItem) dollItemStack.getItem();
        return DollEntityItem.createItemWithBlockState(dollItem.getBlock().defaultBlockState());
    }

    @Override
    public RecipeSerializer<? extends CustomRecipe> getSerializer() {
        return ModRecipes.DOLL_ENTITY_CRAFTING;
    }
}
