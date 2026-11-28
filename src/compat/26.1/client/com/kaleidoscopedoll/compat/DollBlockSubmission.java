package com.kaleidoscopedoll.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;

public final class DollBlockSubmission {
    private DollBlockSubmission() {
    }

    public static void submit(OrderedSubmitNodeCollector collector, PoseStack pose, MovingBlockRenderState state, int outlineColor) {
        collector.submitMovingBlock(pose, state);
    }
}
