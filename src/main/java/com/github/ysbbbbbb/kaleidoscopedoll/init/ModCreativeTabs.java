package com.github.ysbbbbbb.kaleidoscopedoll.init;

import com.github.ysbbbbbb.kaleidoscopedoll.KaleidoscopeDoll;
import com.github.ysbbbbbb.kaleidoscopedoll.event.ModRegisterEvent;
import com.github.ysbbbbbb.kaleidoscopedoll.item.CustomDollItem;
import com.github.ysbbbbbb.kaleidoscopedoll.item.DollEntityItem;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;

import static com.github.ysbbbbbb.kaleidoscopedoll.event.ModRegisterEvent.AUTHOR_DOLLS;
import static com.github.ysbbbbbb.kaleidoscopedoll.event.ModRegisterEvent.SPECIAL_TOOLTIPS;

public class ModCreativeTabs {
    private static final Identifier VANILLA_ICON_ID = Identifier.fromNamespaceAndPath(KaleidoscopeDoll.MOD_ID, "doll_40");
    private static final Identifier AUTHOR_ICON_ID = Identifier.fromNamespaceAndPath(KaleidoscopeDoll.MOD_ID, "doll_0");
    private static final Identifier ENTITY_ICON_ID = Identifier.fromNamespaceAndPath(KaleidoscopeDoll.MOD_ID, "doll_3");

    private static final ResourceKey<CreativeModeTab> VANILLA_DOLL_TAB = tabKey("vanilla_doll");
    private static final ResourceKey<CreativeModeTab> AUTHOR_DOLL_TAB = tabKey("author_doll");
    private static final ResourceKey<CreativeModeTab> CUSTOM_DOLL_TAB = tabKey("custom_doll");
    private static final ResourceKey<CreativeModeTab> ENTITY_DOLL_TAB = tabKey("entity_doll");

    public static CreativeModeTab CUSTOM_DOLL_TAB_INSTANCE;
    public static CreativeModeTab ENTITY_DOLL_TAB_INSTANCE;

    private static ResourceKey<CreativeModeTab> tabKey(String name) {
        return ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath(KaleidoscopeDoll.MOD_ID, name));
    }

    public static void registerTabs() {
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, VANILLA_DOLL_TAB, FabricCreativeModeTab.builder()
                .title(Component.translatable("item_group.kaleidoscope_doll.vanilla_doll.name"))
                .icon(() -> BuiltInRegistries.ITEM.getValue(VANILLA_ICON_ID).getDefaultInstance())
                .displayItems((par, output) -> {
                    output.accept(ModItems.DOLL_MACHINE);
                    output.accept(ModItems.COMPUTER);
                    output.accept(ModItems.TWEAKS_TOOL);
                    output.accept(ModItems.PURPLE_DOLL_GIFT_BOX);
                    output.accept(ModItems.GREEN_DOLL_GIFT_BOX);
                    output.accept(ModItems.YELLOW_DOLL_GIFT_BOX);
                    ModRegisterEvent.DOLL_ITEMS.stream()
                            .filter(item -> !SPECIAL_TOOLTIPS.containsKey(BuiltInRegistries.ITEM.getKey(item)))
                            .forEach(output::accept);
                }).build());

        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, AUTHOR_DOLL_TAB, FabricCreativeModeTab.builder()
                .title(Component.translatable("item_group.kaleidoscope_doll.author_doll.name"))
                .icon(() -> BuiltInRegistries.ITEM.getValue(AUTHOR_ICON_ID).getDefaultInstance())
                .displayItems((par, output) -> {
                    ModRegisterEvent.DOLL_ITEMS.stream()
                            .filter(item -> AUTHOR_DOLLS.contains(BuiltInRegistries.ITEM.getKey(item)))
                            .forEach(output::accept);
                }).build());

        CUSTOM_DOLL_TAB_INSTANCE = FabricCreativeModeTab.builder()
                .title(Component.translatable("item_group.kaleidoscope_doll.custom_doll.name"))
                .icon(CustomDollItem::getDefaultItemStack)
                .displayItems((par, output) -> {
                    CustomDollItem.addCreativeTab(output);
                }).build();
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, CUSTOM_DOLL_TAB, CUSTOM_DOLL_TAB_INSTANCE);

        ENTITY_DOLL_TAB_INSTANCE = FabricCreativeModeTab.builder()
                .title(Component.translatable("item_group.kaleidoscope_doll.entity_doll.name"))
                .icon(() -> BuiltInRegistries.ITEM.getValue(ENTITY_ICON_ID).getDefaultInstance())
                .displayItems((par, output) -> {
                    DollEntityItem.addCreativeTab(output);
                }).build();
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, ENTITY_DOLL_TAB, ENTITY_DOLL_TAB_INSTANCE);
    }
}
