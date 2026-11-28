package com.kaleidoscopedoll.compat.jei;

import com.kaleidoscopedoll.event.ModRegisterEvent;
import com.kaleidoscopedoll.item.DollItem;
import com.kaleidoscopedoll.item.crafting.DollFunctionRecipe;
import com.google.common.collect.Lists;
import mezz.jei.api.constants.ModIds;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.core.registries.Registries;

import java.util.List;

/**
 * 功能合成（玩偶 + 雪球/鸡蛋/末影珍珠/风弹/不死图腾）的 JEI 显示：
 * CustomRecipe 没有形状信息，JEI 不会自动展示，这里为每个组合生成一条合成类显示
 */
public class DollFunctionRecipeMaker {
    private static final String GROUP = "jei.doll.function";

    public static List<RecipeHolder<CraftingRecipe>> createRecipes() {
        List<RecipeHolder<CraftingRecipe>> recipes = Lists.newArrayList();
        ModRegisterEvent.DOLL_ITEMS.forEach(item -> {
            if (!(item instanceof DollItem)) {
                return;
            }
            Identifier key = BuiltInRegistries.ITEM.getKey(item);
            Ingredient inputDoll = Ingredient.of(item);
            ItemStack dollStack = new ItemStack(item);

            for (Item sourceItem : DollFunctionRecipe.FUNCTION_ITEMS) {
                Identifier sourceId = BuiltInRegistries.ITEM.getKey(sourceItem);
                ItemStack source = new ItemStack(sourceItem);
                ItemStack output = DollFunctionRecipe.assembleResult(dollStack, source);
                if (output.isEmpty()) {
                    continue;
                }
                Ingredient inputSource = Ingredient.of(sourceItem);
                Identifier id = Identifier.fromNamespaceAndPath(ModIds.MINECRAFT_ID,
                        GROUP + "." + key.getPath() + "." + sourceId.getPath());
                ShapelessRecipe recipe = new ShapelessRecipe(new Recipe.CommonInfo(true),
                        new CraftingRecipe.CraftingBookInfo(CraftingBookCategory.MISC, GROUP),
                        ItemStackTemplate.fromNonEmptyStack(output),
                        List.of(inputDoll, inputSource));
                recipes.add(new RecipeHolder<>(ResourceKey.create(Registries.RECIPE, id), recipe));
            }
        });
        return recipes;
    }

    private DollFunctionRecipeMaker() {
    }
}
