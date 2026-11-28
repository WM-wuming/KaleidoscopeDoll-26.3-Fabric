package com.github.ysbbbbbb.kaleidoscopedoll.compat;

import net.minecraft.util.Prediction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class DollInventory {
    private DollInventory() {
    }

    public static void drop(Player player, ItemStack stack) {
        player.drop(stack, false, Prediction.PREDICTED);
    }

    public static void returnToInventory(Player player, ItemStack stack) {
        player.getInventory().placeItemBackInInventory(stack, Prediction.SERVER_ONLY);
    }
}
