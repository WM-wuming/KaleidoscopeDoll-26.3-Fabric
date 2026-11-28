package com.kaleidoscopedoll.block;

import com.kaleidoscopedoll.block.entity.CustomDollBlockEntity;
import com.kaleidoscopedoll.data.custom.ServerCustomDollLoader;
import com.kaleidoscopedoll.item.CustomDollItem;
import com.google.common.collect.Lists;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CustomDollBlock extends DollBlock implements EntityBlock {
    public CustomDollBlock(Properties properties) {
        super(properties, CustomDollBlock::new);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new CustomDollBlockEntity(pPos, pState);
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        ItemStack stack = super.getCloneItemStack(level, pos, state, includeData);
        if (level.getBlockEntity(pos) instanceof CustomDollBlockEntity blockEntity) {
            CustomDollItem.setModelId(stack, blockEntity.getModelId());
        }
        return stack;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        // 因为可能是不可变 List，故复制一份
        List<ItemStack> stacks = Lists.newArrayList(super.getDrops(state, params));
        BlockEntity blockEntity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (blockEntity instanceof CustomDollBlockEntity be) {
            var replacement = ServerCustomDollLoader.blockDrops(be.getModelId(), state, params);
            if (replacement.isPresent()) {
                stacks.addAll(replacement.get());
            } else {
                var stack = new ItemStack(this);
                CustomDollItem.setModelId(stack, be.getModelId());
                stacks.add(stack);
            }
        }
        return stacks;
    }
}
