package com.kaleidoscopedoll.compat.jei;

import com.kaleidoscopedoll.item.DollEntityItem;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.apache.commons.lang3.StringUtils;

public class EntityDollSubtype implements ISubtypeInterpreter<ItemStack> {
    @Override
    public Object getSubtypeData(ItemStack itemStack, UidContext context) {
        if (context == UidContext.Ingredient) {
            return StringUtils.EMPTY;
        } else {
            Block block = DollEntityItem.getBlockFromItemStack(itemStack);
            Identifier key = BuiltInRegistries.BLOCK.getKey(block);
            return key.toString();
        }
    }
}