package com.github.ysbbbbbb.kaleidoscopedoll.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.DyeColor;

import java.util.concurrent.CompletableFuture;

import static com.github.ysbbbbbb.kaleidoscopedoll.event.ModRegisterEvent.DOLL_ITEMS;

public class ModRecipeProvider extends VersionedRecipeProvider {
    public ModRecipeProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(StonecuttingOutput output) {
        for (Item doll : DOLL_ITEMS) {
            DyeColor color = getStonecuttingWool(BuiltInRegistries.ITEM.getKey(doll).getPath());
            if (color != null) {
                output.add(RecipeCategory.DECORATIONS, doll, TagItem.wool(color), 1);
            }
        }
    }

    private static DyeColor getStonecuttingWool(String dollPath) {
        // Keep recipe eligibility and wool colors independent of the display order.
        return switch (dollPath) {
            case "doll_0", "doll_1", "doll_67", "doll_68" -> DyeColor.WHITE;
            case "doll_78", "doll_79", "doll_80", "doll_81" -> DyeColor.YELLOW;
            case "doll_599", "doll_601", "doll_614" -> DyeColor.LIGHT_GRAY;
            case "doll_54", "doll_55", "doll_56", "doll_57", "doll_58", "doll_74", "doll_82", "doll_83", "doll_692" -> DyeColor.CYAN;
            case "doll_59", "doll_60", "doll_61", "doll_708" -> DyeColor.PURPLE;
            default -> null;
        };
    }
}
