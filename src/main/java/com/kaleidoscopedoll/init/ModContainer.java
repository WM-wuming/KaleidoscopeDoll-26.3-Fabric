package com.kaleidoscopedoll.init;

import com.kaleidoscopedoll.KaleidoscopeDoll;
import com.kaleidoscopedoll.inventory.ComputerMenu;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

public class ModContainer {
    public static final MenuType<ComputerMenu> COMPUTER_CONTAINER = new MenuType<>(ComputerMenu::new, FeatureFlags.VANILLA_SET);

    public static void registerContainers() {
        Registry.register(BuiltInRegistries.MENU, Identifier.fromNamespaceAndPath(KaleidoscopeDoll.MOD_ID, "computer"), COMPUTER_CONTAINER);
    }
}