package com.github.ysbbbbbb.kaleidoscopedoll.init;

import com.github.ysbbbbbb.kaleidoscopedoll.KaleidoscopeDoll;
import com.github.ysbbbbbb.kaleidoscopedoll.item.CustomDollItem;
import com.github.ysbbbbbb.kaleidoscopedoll.item.DollEntityItem;
import com.github.ysbbbbbb.kaleidoscopedoll.item.GiftBoxItem;
import com.github.ysbbbbbb.kaleidoscopedoll.item.TooltipBlockItem;
import com.github.ysbbbbbb.kaleidoscopedoll.item.TweaksToolItem;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

public final class ModItems {
    public static final Item DOLL_MACHINE = new TooltipBlockItem(ModBlocks.DOLL_MACHINE, blockItemProperties("doll_machine"), "tooltip.kaleidoscope_doll.doll_machine");
    public static final Item COMPUTER = new TooltipBlockItem(ModBlocks.COMPUTER, blockItemProperties("computer"), "tooltip.kaleidoscope_doll.computer");
    public static final Item PURPLE_DOLL_GIFT_BOX = new GiftBoxItem(ModBlocks.PURPLE_DOLL_GIFT_BOX, blockItemProperties("purple_doll_gift_box"));
    public static final Item GREEN_DOLL_GIFT_BOX = new GiftBoxItem(ModBlocks.GREEN_DOLL_GIFT_BOX, blockItemProperties("green_doll_gift_box"));
    public static final Item YELLOW_DOLL_GIFT_BOX = new GiftBoxItem(ModBlocks.YELLOW_DOLL_GIFT_BOX, blockItemProperties("yellow_doll_gift_box"));
    public static final Item DOLL_ICON = new Item(new Item.Properties().setId(itemKey("doll_icon")));
    public static final Item TWEAKS_TOOL = new TweaksToolItem(new Item.Properties().stacksTo(1).setId(itemKey("tweaks_tool")));
    public static final Item DOLL_ENTITY_ITEM = new DollEntityItem(new Item.Properties().setId(itemKey("doll_entity_item")));
    public static final Item CUSTOM_DOLL = new CustomDollItem(ModBlocks.CUSTOM_DOLL,
            new Item.Properties().useBlockDescriptionPrefix().equippable(net.minecraft.world.entity.EquipmentSlot.HEAD).setId(itemKey("custom_doll")));

    public static ResourceKey<Item> itemKey(String name) {
        return ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(KaleidoscopeDoll.MOD_ID, name));
    }

    public static Item.Properties blockItemProperties(String name) {
        return new Item.Properties().useBlockDescriptionPrefix().setId(itemKey(name));
    }

    public static void registerItems() {
        Registry.register(BuiltInRegistries.ITEM, itemKey("doll_machine"), DOLL_MACHINE);
        Registry.register(BuiltInRegistries.ITEM, itemKey("computer"), COMPUTER);
        Registry.register(BuiltInRegistries.ITEM, itemKey("purple_doll_gift_box"), PURPLE_DOLL_GIFT_BOX);
        Registry.register(BuiltInRegistries.ITEM, itemKey("green_doll_gift_box"), GREEN_DOLL_GIFT_BOX);
        Registry.register(BuiltInRegistries.ITEM, itemKey("yellow_doll_gift_box"), YELLOW_DOLL_GIFT_BOX);
        Registry.register(BuiltInRegistries.ITEM, itemKey("doll_icon"), DOLL_ICON);
        Registry.register(BuiltInRegistries.ITEM, itemKey("tweaks_tool"), TWEAKS_TOOL);
        Registry.register(BuiltInRegistries.ITEM, itemKey("doll_entity_item"), DOLL_ENTITY_ITEM);
        Registry.register(BuiltInRegistries.ITEM, itemKey("custom_doll"), CUSTOM_DOLL);
    }
}