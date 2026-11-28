package com.kaleidoscopedoll.datagen;

import com.kaleidoscopedoll.KaleidoscopeDoll;
import com.kaleidoscopedoll.event.ModRegisterEvent;
import com.kaleidoscopedoll.init.ModBlocks;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.models.BlockModelGenerators;
import net.minecraft.data.models.ItemModelGenerators;
import net.minecraft.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.data.models.blockstates.PropertyDispatch;
import net.minecraft.data.models.blockstates.Variant;
import net.minecraft.data.models.blockstates.VariantProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

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
        ModRegisterEvent.DOLL_ITEMS.forEach(item -> {
            String name = BuiltInRegistries.ITEM.getKey(item).getPath();
            generator.output.accept(id("item/" + name), () -> {
                var json = new JsonObject();
                json.addProperty("parent", id("block/doll/" + name).toString());
                return json;
            });
        });
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(KaleidoscopeDoll.MOD_ID, path);
    }
}
