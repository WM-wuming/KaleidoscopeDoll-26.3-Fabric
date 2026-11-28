package com.kaleidoscopedoll.item.crafting;

import com.kaleidoscopedoll.event.ModRegisterEvent;
import com.kaleidoscopedoll.init.ModRecipes;
import com.kaleidoscopedoll.item.DollItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DeathProtection;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 玩偶 + 功能物品（雪球/鸡蛋/末影珍珠/风弹/不死图腾）→ 带该物品功能的合成。
 * 使用普通玩偶作为外观原料。
 * 保留原版功能物品，通过 item_model 换成玩偶模型。
 */
public class DollFunctionRecipe extends CustomRecipe {
    public static final List<Item> FUNCTION_ITEMS = List.of(
            Items.SNOWBALL, Items.EGG, Items.BLUE_EGG, Items.BROWN_EGG,
            Items.ENDER_PEARL, Items.WIND_CHARGE, Items.TOTEM_OF_UNDYING);

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return findIngredients(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        Ingredients ingredients = findIngredients(input);
        return ingredients == null ? ItemStack.EMPTY
                : assembleResult(ingredients.doll(), ingredients.source());
    }

    public static ItemStack assembleResult(ItemStack doll, ItemStack source) {
        if (!isBaseDoll(doll) || !isFunctionItem(source)) {
            return ItemStack.EMPTY;
        }
        ItemStack result = source.copyWithCount(1);
        if (result.is(Items.TOTEM_OF_UNDYING)) {
            result.set(DataComponents.DEATH_PROTECTION, DeathProtection.TOTEM_OF_UNDYING);
        }
        result.set(DataComponents.ITEM_MODEL, BuiltInRegistries.ITEM.getKey(doll.getItem()));
        return result;
    }

    private static @Nullable Ingredients findIngredients(CraftingInput input) {
        if (input.ingredientCount() != 2) {
            return null;
        }

        ItemStack doll = ItemStack.EMPTY;
        ItemStack source = ItemStack.EMPTY;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.getItem() instanceof DollItem) {
                if (!doll.isEmpty() || !isBaseDoll(stack)) {
                    return null;
                }
                doll = stack;
            } else {
                if (!source.isEmpty() || !isFunctionItem(stack)) {
                    return null;
                }
                source = stack;
            }
        }
        return !doll.isEmpty() && !source.isEmpty() ? new Ingredients(doll, source) : null;
    }

    private static boolean isBaseDoll(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof DollItem;
    }

    private static boolean isFunctionItem(ItemStack stack) {
        return FUNCTION_ITEMS.contains(stack.getItem());
    }

    public static boolean isDollFunctionItem(ItemStack stack) {
        Identifier model = stack.get(DataComponents.ITEM_MODEL);
        return model != null && isFunctionItem(stack) && ModRegisterEvent.DOLL_BLOCKS.containsKey(model);
    }

    public static boolean isDollTotem(ItemStack stack) {
        return stack.is(Items.TOTEM_OF_UNDYING) && isDollFunctionItem(stack);
    }

    @Override
    public RecipeSerializer<? extends CustomRecipe> getSerializer() {
        return ModRecipes.DOLL_FUNCTION;
    }

    private record Ingredients(ItemStack doll, ItemStack source) {
    }
}
