package com.kaleidoscopedoll.datagen;

import com.kaleidoscopedoll.KaleidoscopeDoll;
import com.kaleidoscopedoll.client.render.CustomDollSpecialRenderer;
import com.kaleidoscopedoll.event.ModRegisterEvent;
import com.kaleidoscopedoll.init.ModBlocks;
import com.kaleidoscopedoll.init.ModItems;
import com.kaleidoscopedoll.render.DollEntitySpecialRenderer;
import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.blockstates.Variant;
import net.minecraft.client.data.models.blockstates.VariantProperties;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.List;

public class BlockStateGenerator extends FabricModelProvider {
    public BlockStateGenerator(FabricDataOutput output) {
        super(output);
    }

    @Override
    public void generateBlockStateModels(BlockModelGenerators generator) {
        ModRegisterEvent.DOLL_BLOCKS.values().forEach(block -> register(generator, block, "block/doll/"));
        register(generator, ModBlocks.COMPUTER, "block/");
        register(generator, ModBlocks.CUSTOM_DOLL, "block/");
    }

    private void register(BlockModelGenerators generator, Block block, String folder) {
        var model = id(folder + BuiltInRegistries.BLOCK.getKey(block).getPath());
        generator.blockStateOutput.accept(MultiVariantGenerator.multiVariant(block, Variant.variant().with(VariantProperties.MODEL, model))
                .with(BlockModelGenerators.createHorizontalFacingDispatch())
                .with(PropertyDispatch.property(BlockStateProperties.WATERLOGGED).generate(waterlogged -> Variant.variant())));
    }

    @Override
    public void generateItemModels(ItemModelGenerators generator) {
        ModRegisterEvent.DOLL_ITEMS.forEach(item -> generator.itemModelOutput.accept(item,
                ItemModelUtils.plainModel(id("block/doll/" + BuiltInRegistries.ITEM.getKey(item).getPath()))));
        List.of(ModItems.DOLL_MACHINE, ModItems.PURPLE_DOLL_GIFT_BOX, ModItems.GREEN_DOLL_GIFT_BOX,
                ModItems.YELLOW_DOLL_GIFT_BOX, ModItems.DOLL_ICON, ModItems.COMPUTER, ModItems.TWEAKS_TOOL)
                .forEach(item -> generator.itemModelOutput.accept(item,
                        ItemModelUtils.plainModel(id("item/" + BuiltInRegistries.ITEM.getKey(item).getPath()))));
        generator.itemModelOutput.accept(ModItems.DOLL_ENTITY_ITEM,
                ItemModelUtils.specialModel(id("item/doll_entity_item"), new DollEntitySpecialRenderer.Unbaked()));
        generator.itemModelOutput.accept(ModItems.CUSTOM_DOLL,
                ItemModelUtils.specialModel(id("item/custom_doll"), new CustomDollSpecialRenderer.Unbaked()));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(KaleidoscopeDoll.MOD_ID, path);
    }
}
