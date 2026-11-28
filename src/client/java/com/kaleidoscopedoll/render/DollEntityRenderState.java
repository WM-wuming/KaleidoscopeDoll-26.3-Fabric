package com.kaleidoscopedoll.render;

import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public class DollEntityRenderState extends EntityRenderState {
    public final MovingBlockRenderState movingBlockRenderState = new MovingBlockRenderState();
    public final Vector3f translation = new Vector3f();
    public final Vector3f scale = new Vector3f(1, 1, 1);
    public float yaw;
    public float pitch;

    public String customDollId = "";
    public final ItemStackRenderState javaDollModel = new ItemStackRenderState();
    public ItemStack holdItem = ItemStack.EMPTY;
    public final ItemStackRenderState holdItemState = new ItemStackRenderState();
    public final Vector3f itemTranslation = new Vector3f();
    public final Vector3f itemScale = new Vector3f(1, 1, 1);
    public final Vector3f itemRotation = new Vector3f();
}
