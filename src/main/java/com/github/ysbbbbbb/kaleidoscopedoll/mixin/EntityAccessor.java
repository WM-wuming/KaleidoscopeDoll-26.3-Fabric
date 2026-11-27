package com.github.ysbbbbbb.kaleidoscopedoll.mixin;

import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * entityData 声明在 Entity 中（protected），通过 Accessor 访问
 */
@Mixin(Entity.class)
public interface EntityAccessor {
    @Accessor("entityData")
    SynchedEntityData kaleidoscopeDoll$getEntityData();
}
