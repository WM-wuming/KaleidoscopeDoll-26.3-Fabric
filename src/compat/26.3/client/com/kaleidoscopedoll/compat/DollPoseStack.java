package com.kaleidoscopedoll.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import org.joml.Quaternionfc;

public final class DollPoseStack {
    private DollPoseStack() {}
    public static void rotate(PoseStack pose, Quaternionfc rotation) { pose.rotate(rotation); }
    public static void rotateDegrees(PoseStack pose, Axis axis, float degrees) { pose.rotateDegrees(axis, degrees); }
}
