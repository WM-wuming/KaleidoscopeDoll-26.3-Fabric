package com.github.ysbbbbbb.kaleidoscopedoll.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

abstract class InterpolatingDollEntity extends Entity {
    private int interpolationSteps;
    private double targetX;
    private double targetY;
    private double targetZ;
    private float targetYaw;
    private float targetPitch;

    protected InterpolatingDollEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public void lerpTo(double x, double y, double z, float yaw, float pitch, int steps) {
        interpolationSteps = steps;
        targetX = x;
        targetY = y;
        targetZ = z;
        targetYaw = yaw;
        targetPitch = pitch;
        if (steps <= 0) {
            setPos(x, y, z);
            setRot(yaw, pitch);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide && interpolationSteps > 0) {
            lerpPositionAndRotationStep(interpolationSteps--, targetX, targetY, targetZ, targetYaw, targetPitch);
        }
    }

    @Override
    public void cancelLerp() {
        interpolationSteps = 0;
    }
}
