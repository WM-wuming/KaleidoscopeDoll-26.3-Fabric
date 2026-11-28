package com.kaleidoscopedoll.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;

import java.util.concurrent.CompletableFuture;

abstract class VersionedRecipeProvider extends FabricRecipeProvider {
    protected VersionedRecipeProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
        super(output, lookup);
    }

    protected abstract void buildRecipes(StonecuttingOutput output);

    @Override
    public String getName() {
        return "Kaleidoscope Doll recipes";
    }

    @Override
    protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
        return new RecipeProvider(registries, output) {
            @Override
            public void buildRecipes() {
                VersionedRecipeProvider.this.buildRecipes(this::stonecutterResultFromBase);
            }
        };
    }
}
