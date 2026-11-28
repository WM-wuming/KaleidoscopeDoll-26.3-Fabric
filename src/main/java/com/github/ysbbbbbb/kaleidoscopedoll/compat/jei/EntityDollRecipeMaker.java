package com.github.ysbbbbbb.kaleidoscopedoll.compat.jei;

import com.github.ysbbbbbb.kaleidoscopedoll.event.ModRegisterEvent;
import com.github.ysbbbbbb.kaleidoscopedoll.item.DollEntityItem;
import com.github.ysbbbbbb.kaleidoscopedoll.item.DollItem;
import com.google.common.collect.Lists;
import mezz.jei.api.constants.ModIds;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class EntityDollRecipeMaker {
    private static final String GROUP = "jei.doll.entity";

    public static List<RecipeHolder<CraftingRecipe>> createRecipes() {
        List<RecipeHolder<CraftingRecipe>> recipes = Lists.newArrayList();
        ModRegisterEvent.DOLL_ITEMS.forEach(item -> {
            Identifier key = BuiltInRegistries.ITEM.getKey(item);
            if (!(item instanceof DollItem dollItem)) {
                return;
            }

            Ingredient inputDoll = Ingredient.of(item);
            Ingredient slimeBall = Ingredient.of(Items.SLIME_BALL);
            List<Ingredient> inputs = List.of(inputDoll, slimeBall);

            BlockState dollState = dollItem.getBlock().defaultBlockState();
            ItemStack output = DollEntityItem.createItemWithBlockState(dollState);

            Identifier id = Identifier.fromNamespaceAndPath(ModIds.MINECRAFT_ID, GROUP + "." + key.getPath());
            ShapelessRecipe recipe = new ShapelessRecipe(new Recipe.CommonInfo(true),
                    new CraftingRecipe.CraftingBookInfo(CraftingBookCategory.MISC, GROUP),
                    ItemStackTemplate.fromNonEmptyStack(output), inputs);

            recipes.add(new RecipeHolder<>(ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE, id), recipe));
        });
        return recipes;
    }

    private EntityDollRecipeMaker() {
    }
}
