package com.github.ysbbbbbb.kaleidoscopedoll.client.render;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.Direction;

public class CustomDollRenderState extends BlockEntityRenderState {
    public String modelId = "";
    public Direction facing = Direction.NORTH;
    public final ItemStackRenderState javaModel = new ItemStackRenderState();
}
