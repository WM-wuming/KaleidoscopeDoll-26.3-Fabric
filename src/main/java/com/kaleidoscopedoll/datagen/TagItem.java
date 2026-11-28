package com.kaleidoscopedoll.datagen;

import com.kaleidoscopedoll.KaleidoscopeDoll;
import com.kaleidoscopedoll.event.ModRegisterEvent;
import com.kaleidoscopedoll.init.ModItems;
import com.google.common.collect.Lists;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.kaleidoscopedoll.event.ModRegisterEvent.SPECIAL_TOOLTIPS;

public class TagItem extends FabricTagsProvider<Item> {
    /**
     * 原版生物玩偶
     */
    public static final TagKey<Item> VANILLA_DOLLS = itemTagKey("vanilla_dolls");
    /**
     * 玩家玩偶
     */
    public static final TagKey<Item> PLAYER_DOLLS = itemTagKey("player_dolls");
    /**
     * 作者玩偶，注意所有的作者玩偶也是玩家玩偶
     */
    public static final TagKey<Item> AUTHOR_DOLLS = itemTagKey("author_dolls");
    /**
     * 自定义玩偶
     */
    public static final TagKey<Item> CUSTOM_DOLLS = itemTagKey("custom_dolls");
    /**
     * 所有本模组玩偶的 tag，因为方块形态玩偶都使用了不同的注册名
     */
    public static final TagKey<Item> ALL_DOLLS = itemTagKey("all_dolls");
    /**
     * 方块玩偶转换成实体玩偶，合成所需的素材
     */
    public static final TagKey<Item> BLOCK_DOLLS_TO_ENTITY_ITEM = itemTagKey("block_dolls_to_entity_item");

    /**
     * 所有等级的代币
     */
    public static final TagKey<Item> MACHINE_TOKENS = itemTagKey("machine_tokens");
    /**
     * 0 级代币，能够在抽奖机中抽取主世界生物玩偶
     */
    public static final TagKey<Item> TIER_0_MACHINE_TOKENS = itemTagKey("tier_0_machine_tokens");
    /**
     * 1 级代币，能够在抽奖机中抽取下界生物玩偶
     */
    public static final TagKey<Item> TIER_1_MACHINE_TOKENS = itemTagKey("tier_1_machine_tokens");
    /**
     * 2 级代币，能够在抽奖机中抽取末地生物玩偶
     */
    public static final TagKey<Item> TIER_2_MACHINE_TOKENS = itemTagKey("tier_2_machine_tokens");

    /**
     * 0 级玩偶，能够通过 0 级代币在抽奖机中抽取
     */
    public static final TagKey<Item> TIER_0_DOLLS = itemTagKey("tier_0_dolls");
    /**
     * 1 级玩偶，能够通过 1 级代币在抽奖机中抽取
     */
    public static final TagKey<Item> TIER_1_DOLLS = itemTagKey("tier_1_dolls");
    /**
     * 2 级玩偶，能够通过 2 级代币在抽奖机中抽取
     */
    public static final TagKey<Item> TIER_2_DOLLS = itemTagKey("tier_2_dolls");

    /**
     * 用电脑合成玩偶时需要的代币
     */
    public static final TagKey<Item> COMPUTER_TOKENS = itemTagKey("computer_tokens");

    public TagItem(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, Registries.ITEM, lookupProvider);
    }

    private static TagKey<Item> itemTagKey(String key) {
        return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(KaleidoscopeDoll.MOD_ID, key));
    }

    private static ResourceKey<Item> dollItem(String key) {
        return ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(KaleidoscopeDoll.MOD_ID, key));
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        ModRegisterEvent.DOLL_ITEMS.forEach(item -> {
            Identifier key = BuiltInRegistries.ITEM.getKey(item);
            ResourceKey<Item> itemKey = BuiltInRegistries.ITEM.getResourceKey(item).orElseThrow();
            if (!SPECIAL_TOOLTIPS.containsKey(key)) {
                this.builder(VANILLA_DOLLS).add(itemKey);
            } else {
                this.builder(PLAYER_DOLLS).add(itemKey);
                if (ModRegisterEvent.AUTHOR_DOLLS.contains(key)) {
                    this.builder(AUTHOR_DOLLS).add(itemKey);
                }
            }
            this.builder(ALL_DOLLS).add(itemKey);
        });

        this.builder(CUSTOM_DOLLS).add(BuiltInRegistries.ITEM.getResourceKey(ModItems.CUSTOM_DOLL).orElseThrow());
        this.builder(ALL_DOLLS).add(BuiltInRegistries.ITEM.getResourceKey(ModItems.CUSTOM_DOLL).orElseThrow());

        this.builder(BLOCK_DOLLS_TO_ENTITY_ITEM)
                .add(BuiltInRegistries.ITEM.getResourceKey(Items.SLIME_BALL).orElseThrow())
                .add(BuiltInRegistries.ITEM.getResourceKey(Items.CLAY_BALL).orElseThrow());

        this.builder(TIER_0_MACHINE_TOKENS).add(BuiltInRegistries.ITEM.getResourceKey(Items.IRON_INGOT).orElseThrow());
        this.builder(TIER_1_MACHINE_TOKENS).add(BuiltInRegistries.ITEM.getResourceKey(Items.GOLD_INGOT).orElseThrow());
        this.builder(TIER_2_MACHINE_TOKENS).add(BuiltInRegistries.ITEM.getResourceKey(Items.DIAMOND).orElseThrow());
        this.builder(MACHINE_TOKENS).addTag(TIER_0_MACHINE_TOKENS)
                .addTag(TIER_1_MACHINE_TOKENS)
                .addTag(TIER_2_MACHINE_TOKENS);

        List<ResourceKey<Item>> tier1Dolls = Lists.newArrayList(
                dollItem("doll_62"),
                dollItem("doll_64"),
                dollItem("doll_65"),
                dollItem("doll_66"),
                dollItem("doll_69"),
                dollItem("doll_70"),
                dollItem("doll_71"),
                dollItem("doll_72"),
                dollItem("doll_73"),
                dollItem("doll_78"),
                dollItem("doll_74")
        );
        tier1Dolls.forEach(this.builder(TIER_1_DOLLS)::add);

        List<ResourceKey<Item>> tier2Dolls = Lists.newArrayList(
                dollItem("doll_75"),
                dollItem("doll_76"),
                dollItem("doll_77"),
                dollItem("doll_79")
        );
        tier2Dolls.forEach(this.builder(TIER_2_DOLLS)::add);

        // 0 级玩偶是除了 1 级和 2 级玩偶之外的所有玩偶
        ModRegisterEvent.DOLL_ITEMS.stream()
                .filter(item -> !SPECIAL_TOOLTIPS.containsKey(BuiltInRegistries.ITEM.getKey(item)))
                .forEach(item -> BuiltInRegistries.ITEM.getResourceKey(item).ifPresent(k -> {
                    if (!tier1Dolls.contains(k) && !tier2Dolls.contains(k)) {
                        this.builder(TIER_0_DOLLS).add(k);
                    }
                }));

        for (DyeColor color : DyeColor.values()) {
            this.builder(COMPUTER_TOKENS).add(BuiltInRegistries.ITEM.getResourceKey(wool(color)).orElseThrow());
        }
    }

    static Item wool(DyeColor color) {
        return BuiltInRegistries.ITEM.getOptional(Identifier.withDefaultNamespace(color.getName() + "_wool")).orElseThrow();
    }
}
