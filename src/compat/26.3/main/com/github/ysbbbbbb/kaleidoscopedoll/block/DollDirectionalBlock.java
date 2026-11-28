package com.github.ysbbbbbb.kaleidoscopedoll.block;

import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.material.PushReaction;

import java.util.function.Function;

abstract class DollDirectionalBlock extends HorizontalDirectionalBlock {
    protected static final PushReaction IMMOVABLE = PushReaction.IMMOVEABLE;

    protected DollDirectionalBlock(Properties properties, Function<Properties, ? extends DollDirectionalBlock> factory) {
        super(properties);
    }
}
