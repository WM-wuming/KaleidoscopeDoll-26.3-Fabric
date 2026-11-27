package com.github.ysbbbbbb.kaleidoscopedoll.mixin;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.ResultContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * inputSlots / resultSlots 声明在父类 ItemCombinerMenu 中，
 * 子类 Mixin 的 @Shadow 无法定位继承字段，通过 Accessor 访问
 */
@Mixin(ItemCombinerMenu.class)
public interface ItemCombinerMenuAccessor {
    @Accessor("inputSlots")
    Container kaleidoscopeDoll$getInputSlots();

    @Accessor("resultSlots")
    ResultContainer kaleidoscopeDoll$getResultSlots();
}
