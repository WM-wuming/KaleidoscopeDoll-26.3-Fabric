package com.kaleidoscopedoll.datagen;

import com.kaleidoscopedoll.KaleidoscopeDoll;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.SingleItemRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.concurrent.CompletableFuture;

abstract class VersionedRecipeProvider extends FabricRecipeProvider {
    protected VersionedRecipeProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    public void buildRecipes(RecipeOutput output) {
        buildRecipes((category, result, base, count) -> SingleItemRecipeBuilder.stonecutting(Ingredient.of(base), category, result, count)
                .unlockedBy(getHasName(base), has(base))
                .save(output, ResourceLocation.fromNamespaceAndPath(KaleidoscopeDoll.MOD_ID,
                        getConversionRecipeName(result, base) + "_stonecutting")));
    }

    protected abstract void buildRecipes(StonecuttingOutput output);
}
