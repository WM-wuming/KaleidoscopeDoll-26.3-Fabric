package com.github.ysbbbbbb.kaleidoscopedoll.init;


import com.github.ysbbbbbb.kaleidoscopedoll.KaleidoscopeDoll;
import com.github.ysbbbbbb.kaleidoscopedoll.item.crafting.DollEntityCraftingRecipe;
import com.github.ysbbbbbb.kaleidoscopedoll.item.crafting.DollFunctionRecipe;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class ModRecipes {
    public static RecipeSerializer<DollEntityCraftingRecipe> DOLL_ENTITY_CRAFTING;
    public static RecipeSerializer<DollFunctionRecipe> DOLL_FUNCTION;

    public static void registerRecipe() {
        DOLL_ENTITY_CRAFTING = Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,
                Identifier.fromNamespaceAndPath(KaleidoscopeDoll.MOD_ID, "doll_entity_crafting"),
                new RecipeSerializer<>(MapCodec.unit(new DollEntityCraftingRecipe()), StreamCodec.unit(new DollEntityCraftingRecipe())));
        DOLL_FUNCTION = Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,
                Identifier.fromNamespaceAndPath(KaleidoscopeDoll.MOD_ID, "doll_function"),
                new RecipeSerializer<>(MapCodec.unit(new DollFunctionRecipe()), StreamCodec.unit(new DollFunctionRecipe())));
    }
}
