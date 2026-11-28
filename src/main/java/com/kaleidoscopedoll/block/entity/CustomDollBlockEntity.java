package com.kaleidoscopedoll.block.entity;

import com.kaleidoscopedoll.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.apache.commons.lang3.StringUtils;

public class CustomDollBlockEntity extends DollBlockEntity {
    private static final String NBT_MODEL_ID = "DollModelId";

    private String modelId = StringUtils.EMPTY;

    public CustomDollBlockEntity(BlockEntityType<?> entityType, BlockPos pos, BlockState state) {
        super(entityType, pos, state);
    }

    public CustomDollBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlocks.CUSTOM_DOLL_BE, pos, state);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putString(NBT_MODEL_ID, modelId);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.modelId = input.getStringOr(NBT_MODEL_ID, StringUtils.EMPTY);
    }

    public void refresh() {
        this.setChanged();
        if (level != null) {
            BlockState state = level.getBlockState(worldPosition);
            level.sendBlockUpdated(worldPosition, state, state, net.minecraft.world.level.block.Block.UPDATE_ALL);
        }
    }

    @Override
    public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        return this.saveWithFullMetadata(registries);
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void applyImplicitComponents(net.minecraft.core.component.DataComponentGetter components) {
        super.applyImplicitComponents(components);
        String id = components.get(com.kaleidoscopedoll.init.ModDataComponent.TAG_CUSTOM_DOLL_ID);
        if (id != null) {
            this.modelId = id;
        }
    }

    @Override
    protected void collectImplicitComponents(net.minecraft.core.component.DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (!modelId.isEmpty()) {
            components.set(com.kaleidoscopedoll.init.ModDataComponent.TAG_CUSTOM_DOLL_ID, modelId);
        }
    }

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
        refresh();
    }
}
