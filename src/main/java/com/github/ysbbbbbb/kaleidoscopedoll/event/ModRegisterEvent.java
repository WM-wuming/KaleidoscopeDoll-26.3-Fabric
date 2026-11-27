package com.github.ysbbbbbb.kaleidoscopedoll.event;

import com.github.ysbbbbbb.kaleidoscopedoll.KaleidoscopeDoll;
import com.github.ysbbbbbb.kaleidoscopedoll.block.DollBlock;
import com.github.ysbbbbbb.kaleidoscopedoll.item.DollItem;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import com.github.ysbbbbbb.kaleidoscopedoll.init.ModBlocks;
import com.github.ysbbbbbb.kaleidoscopedoll.init.ModItems;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EquipmentSlot;

import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

public class ModRegisterEvent {
    public static final Map<Identifier, DollBlock> DOLL_BLOCKS = Maps.newLinkedHashMap();
    public static final Map<Identifier, String> VANILLA_TOOLTIPS = Maps.newHashMap();
    public static final Map<Identifier, String> SPECIAL_TOOLTIPS = Maps.newHashMap();
    public static final Set<Item> DOLL_ITEMS = Sets.newLinkedHashSet();
    // 作者栏只记录 ID
    public static final Set<Identifier> AUTHOR_DOLLS = Sets.newHashSet();
    private static final int[] EXTRA_AUTHOR_DOLL_IDS = {599, 601, 614, 692, 708};

    private static void registerAllVanillaTooltips() {
        registerVanillaTooltips("doll_54", "creaking");
        registerVanillaTooltips("doll_55", "zombie_camel");
        registerVanillaTooltips("doll_56", "zombie_horse");
        registerVanillaTooltips("doll_57", "zombie_nautilus");
        registerVanillaTooltips("doll_58", "skeleton_horse");
        registerVanillaTooltips("doll_59", "resin_golem");
        registerVanillaTooltips("doll_60", "sniffer");
        registerVanillaTooltips("doll_61", "nautilus");
        registerVanillaTooltips("doll_74", "happy_ghast");
        registerVanillaTooltips("doll_80", "wine_fox");
        registerVanillaTooltips("doll_81", "dokimobs_warden");
        registerVanillaTooltips("doll_82", "dokimobs_skeletons");
        registerVanillaTooltips("doll_83", "dokimobs_creeper");
    }

    private static void registerAuthorTooltips() {
        registerAuthorTooltips("doll_0", "author_ysbb");
        registerAuthorTooltips("doll_1", "author_tartaric_acid");
        registerAuthorTooltips("doll_67", "author_abert_cat");
        registerAuthorTooltips("doll_68", "author_cr_019");
        registerAuthorTooltips("doll_599", "author_bzdxg");
        registerAuthorTooltips("doll_601", "author_yca");
        registerAuthorTooltips("doll_614", "author_grainalcohol");
        registerAuthorTooltips("doll_692", "author_mly");
        registerAuthorTooltips("doll_708", "author_molisha");
    }

    public static void registerBlocks() {
        registerAllVanillaTooltips();
        registerAuthorTooltips();
        // The creative tab follows ID order: Overworld, Nether, End, Boss, then Special.
        // Author dolls keep their original IDs, including 0, 1, 67 and 68.
        IntStream.concat(IntStream.rangeClosed(0, 83), IntStream.of(EXTRA_AUTHOR_DOLL_IDS)).forEach(i -> {
            Identifier name = Identifier.fromNamespaceAndPath(KaleidoscopeDoll.MOD_ID, "doll_" + i);
            DollBlock block = new DollBlock(DollBlock.dollProperties().setId(ModBlocks.blockKey("doll_" + i)));
            // 未登记专属介绍的玩偶默认走各自的语言键 tooltip.kaleidoscope_doll.doll.doll_N
            String vanillaDesc = VANILLA_TOOLTIPS.getOrDefault(name, "doll_" + i);
            String specialDesc = SPECIAL_TOOLTIPS.getOrDefault(name, vanillaDesc);
            Item item = new DollItem(block, specialDesc,
                    new Item.Properties().useBlockDescriptionPrefix().equippable(EquipmentSlot.HEAD).setId(ModItems.itemKey("doll_" + i)));
            DOLL_BLOCKS.put(name, block);
            DOLL_ITEMS.add(item);

            Registry.register(BuiltInRegistries.BLOCK, name, block);
            Registry.register(BuiltInRegistries.ITEM, name, item);
        });
    }

    private static void registerVanillaTooltips(String name, String tooltip) {
        Identifier id = Identifier.fromNamespaceAndPath(KaleidoscopeDoll.MOD_ID, name);
        VANILLA_TOOLTIPS.put(id, tooltip);
    }

    private static void registerAuthorTooltips(String name, String tooltip) {
        Identifier id = Identifier.fromNamespaceAndPath(KaleidoscopeDoll.MOD_ID, name);
        SPECIAL_TOOLTIPS.put(id, tooltip);
        AUTHOR_DOLLS.add(id);
    }
}
