package com.github.ysbbbbbb.kaleidoscopedoll.datagen;

import com.github.ysbbbbbb.kaleidoscopedoll.KaleidoscopeDoll;
import com.github.ysbbbbbb.kaleidoscopedoll.event.ModRegisterEvent;
import com.github.ysbbbbbb.kaleidoscopedoll.init.ModBlocks;
import com.github.ysbbbbbb.kaleidoscopedoll.init.ModItems;
import com.github.ysbbbbbb.kaleidoscopedoll.client.render.CustomDollSpecialRenderer;
import com.github.ysbbbbbb.kaleidoscopedoll.render.DollEntitySpecialRenderer;
import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.renderer.block.dispatch.Variant;
import net.minecraft.client.renderer.block.dispatch.VariantMutator;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.Item;

import java.util.List;

public class BlockStateGenerator extends FabricModelProvider {
    private static final PropertyDispatch<VariantMutator> HORIZONTAL_ROTATION =
            PropertyDispatch.modify(BlockStateProperties.HORIZONTAL_FACING)
                    .select(Direction.NORTH, BlockModelGenerators.NOP)
                    .select(Direction.EAST, BlockModelGenerators.Y_ROT_90)
                    .select(Direction.SOUTH, BlockModelGenerators.Y_ROT_180)
                    .select(Direction.WEST, BlockModelGenerators.Y_ROT_270);

    public BlockStateGenerator(FabricPackOutput output) {
        super(output);
    }

    @Override
    public void generateBlockStateModels(BlockModelGenerators generator) {
        ModRegisterEvent.DOLL_BLOCKS.values().forEach(block -> {
            Identifier key = BuiltInRegistries.BLOCK.getKey(block);
            Identifier location = modLoc("block/doll/" + key.getPath());
            MultiVariant variant = BlockModelGenerators.variant(new Variant(location));
            generator.blockStateOutput.accept(MultiVariantGenerator.dispatch(block, variant)
                    .with(HORIZONTAL_ROTATION)
                    .with(PropertyDispatch.modify(BlockStateProperties.WATERLOGGED)
                            .select(true, BlockModelGenerators.NOP)
                            .select(false, BlockModelGenerators.NOP)));
        });

        for (Block block : new Block[]{ModBlocks.COMPUTER, ModBlocks.CUSTOM_DOLL}) {
            Identifier key = BuiltInRegistries.BLOCK.getKey(block);
            MultiVariant variant = BlockModelGenerators.variant(new Variant(modLoc("block/" + key.getPath())));
            generator.blockStateOutput.accept(MultiVariantGenerator.dispatch(block, variant)
                    .with(HORIZONTAL_ROTATION)
                    .with(PropertyDispatch.modify(BlockStateProperties.WATERLOGGED)
                            .select(true, BlockModelGenerators.NOP)
                            .select(false, BlockModelGenerators.NOP)));
        }
    }

    @Override
    public void generateItemModels(ItemModelGenerators generator) {
        ModRegisterEvent.DOLL_ITEMS.forEach(item -> {
            Identifier key = BuiltInRegistries.ITEM.getKey(item);
            Identifier blockModel = modLoc("block/doll/" + key.getPath());
            generator.itemModelOutput.accept(item, ItemModelUtils.plainModel(blockModel), ClientItem.Properties.DEFAULT);
        });

        List<Item> plainItems = List.of(ModItems.DOLL_MACHINE, ModItems.PURPLE_DOLL_GIFT_BOX,
                ModItems.GREEN_DOLL_GIFT_BOX, ModItems.YELLOW_DOLL_GIFT_BOX, ModItems.DOLL_ICON,
                ModItems.COMPUTER, ModItems.TWEAKS_TOOL);
        plainItems.forEach(item -> {
            Identifier key = BuiltInRegistries.ITEM.getKey(item);
            generator.itemModelOutput.accept(item, ItemModelUtils.plainModel(modLoc("item/" + key.getPath())), ClientItem.Properties.DEFAULT);
        });

        generator.itemModelOutput.accept(ModItems.DOLL_ENTITY_ITEM,
                ItemModelUtils.specialModel(modLoc("item/doll_entity_item"), new DollEntitySpecialRenderer.Unbaked()),
                ClientItem.Properties.DEFAULT);

        generator.itemModelOutput.accept(ModItems.CUSTOM_DOLL,
                ItemModelUtils.specialModel(modLoc("item/custom_doll"), new CustomDollSpecialRenderer.Unbaked()),
                ClientItem.Properties.DEFAULT);
    }

    private Identifier modLoc(String path) {
        return Identifier.fromNamespaceAndPath(KaleidoscopeDoll.MOD_ID, path);
    }
}
