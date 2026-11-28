package com.kaleidoscopedoll.datagen;

import com.kaleidoscopedoll.event.ModRegisterEvent;
import com.kaleidoscopedoll.init.ModItems;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricAdvancementProvider;
import net.minecraft.advancements.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.core.ClientAsset;
import java.util.Optional;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static com.kaleidoscopedoll.event.ModRegisterEvent.SPECIAL_TOOLTIPS;

public class AdvancementGenerator extends FabricAdvancementProvider {
    protected AdvancementGenerator(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registryLookup) {
        super(output, registryLookup);
    }

    @Override
    public void generateAdvancement(HolderLookup.Provider registryLookup, Consumer<AdvancementHolder> saver) {
        AdvancementHolder root = make(BuiltInRegistries.ITEM.getValue(id("doll_40")), "root", true)
                .requirements(AdvancementRequirements.Strategy.OR)
                .addCriterion("tick", DollCriteria.tick())
                .build(id("doll/root"));
        saver.accept(root);

        AdvancementHolder dollMachine = make(ModItems.DOLL_MACHINE, "doll_machine").parent(root)
                .addCriterion("doll_machine", DollCriteria.hasItem(ModItems.DOLL_MACHINE))
                .build(id("doll/doll_machine"));
        saver.accept(dollMachine);

        saver.accept(make(ModItems.PURPLE_DOLL_GIFT_BOX, "purple_doll_gift_box").parent(dollMachine)
                .addCriterion("purple_doll_gift_box", DollCriteria.hasItem(ModItems.PURPLE_DOLL_GIFT_BOX))
                .build(id("doll/purple_doll_gift_box")));
        saver.accept(make(ModItems.GREEN_DOLL_GIFT_BOX, "green_doll_gift_box").parent(dollMachine)
                .addCriterion("green_doll_gift_box", DollCriteria.hasItem(ModItems.GREEN_DOLL_GIFT_BOX))
                .build(id("doll/green_doll_gift_box")));
        AdvancementHolder yellowGiftBox = make(ModItems.YELLOW_DOLL_GIFT_BOX, "yellow_doll_gift_box").parent(dollMachine)
                .addCriterion("yellow_doll_gift_box", DollCriteria.hasItem(ModItems.YELLOW_DOLL_GIFT_BOX))
                .build(id("doll/yellow_doll_gift_box"));
        saver.accept(yellowGiftBox);

        Advancement.Builder fullCollection = makeChallenge(BuiltInRegistries.ITEM.getValue(id("doll_3")), "full_collection").parent(yellowGiftBox)
                .requirements(AdvancementRequirements.Strategy.AND);
        ModRegisterEvent.DOLL_ITEMS.stream()
                .filter(item -> !SPECIAL_TOOLTIPS.containsKey(BuiltInRegistries.ITEM.getKey(item)))
                .forEach(item -> fullCollection.addCriterion(BuiltInRegistries.ITEM.getKey(item).getPath(), DollCriteria.hasItem(item)));
        saver.accept(fullCollection.rewards(AdvancementRewards.Builder.experience(100))
                .build(id("doll/full_collection")));
    }

    private static final ClientAsset.ResourceTexture BACKGROUND = new ClientAsset.ResourceTexture(
            Identifier.fromNamespaceAndPath(com.kaleidoscopedoll.KaleidoscopeDoll.MOD_ID, "textures/advancement/background.png"));

    private static Advancement.Builder make(Item item, String key) {
        return make(item, key, false);
    }

    private static Advancement.Builder make(Item item, String key, boolean root) {
        MutableComponent title = Component.translatable(String.format("advancements.kaleidoscope_doll.doll.%s.title", key));
        MutableComponent desc = Component.translatable(String.format("advancements.kaleidoscope_doll.doll.%s.description", key));

        return Advancement.Builder.advancement().display(new DisplayInfo(new ItemStackTemplate(item), title, desc,
                root ? Optional.of(BACKGROUND) : Optional.empty(), AdvancementType.TASK, false, false, false));
    }

    private static Advancement.Builder makeChallenge(Item item, String key) {
        MutableComponent title = Component.translatable(String.format("advancements.kaleidoscope_doll.doll.%s.title", key));
        MutableComponent desc = Component.translatable(String.format("advancements.kaleidoscope_doll.doll.%s.description", key));

        return Advancement.Builder.advancement().display(new DisplayInfo(new ItemStackTemplate(item), title, desc,
                Optional.empty(), AdvancementType.CHALLENGE, true, true, false));
    }

    private static Identifier id(String id) {
        return Identifier.fromNamespaceAndPath(com.kaleidoscopedoll.KaleidoscopeDoll.MOD_ID, id);
    }
}
