package com.kaleidoscopedoll.datagen;

import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.world.level.ItemLike;

@FunctionalInterface
interface StonecuttingOutput {
    void add(RecipeCategory category, ItemLike result, ItemLike material, int count);
}
