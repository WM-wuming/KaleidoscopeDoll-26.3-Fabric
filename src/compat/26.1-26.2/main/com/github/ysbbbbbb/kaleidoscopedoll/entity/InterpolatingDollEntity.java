package com.github.ysbbbbbb.kaleidoscopedoll.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.level.Level;

abstract class InterpolatingDollEntity extends Entity {
    private final InterpolationHandler interpolation = new InterpolationHandler(this);

    protected InterpolatingDollEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    public InterpolationHandler getInterpolation() {
        return this.interpolation;
    }

    @Override
    public void tick() {
        super.tick();
        this.interpolation.interpolate();
    }
}
