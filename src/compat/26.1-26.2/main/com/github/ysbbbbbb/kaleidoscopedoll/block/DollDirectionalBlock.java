package com.github.ysbbbbbb.kaleidoscopedoll.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.material.PushReaction;

import java.util.function.Function;

abstract class DollDirectionalBlock extends HorizontalDirectionalBlock {
    protected static final PushReaction IMMOVABLE = PushReaction.BLOCK;
    private final MapCodec<? extends DollDirectionalBlock> codec;

    protected DollDirectionalBlock(Properties properties, Function<Properties, ? extends DollDirectionalBlock> factory) {
        super(properties);
        this.codec = simpleCodec(factory::apply);
    }

    @Override
    protected final MapCodec<? extends DollDirectionalBlock> codec() {
        return this.codec;
    }
}
