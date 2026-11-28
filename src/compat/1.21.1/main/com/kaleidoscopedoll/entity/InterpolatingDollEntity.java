package com.kaleidoscopedoll.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

abstract class InterpolatingDollEntity extends Entity {
    private int steps;
    private Vec3 target = Vec3.ZERO;
    private float yaw;
    private float pitch;

    protected InterpolatingDollEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    public void lerpTo(double x, double y, double z, float yaw, float pitch, int steps) {
        this.steps = steps;
        this.target = new Vec3(x, y, z);
        this.yaw = yaw;
        this.pitch = pitch;
        if (steps <= 0) {
            setPos(x, y, z);
            setRot(yaw, pitch);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide && steps > 0) {
            lerpPositionAndRotationStep(steps--, target.x, target.y, target.z, yaw, pitch);
        }
    }
}
