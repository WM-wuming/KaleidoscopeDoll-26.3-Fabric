package com.kaleidoscopedoll.mixin;

import com.kaleidoscopedoll.init.ModDataComponent;
import com.kaleidoscopedoll.item.DollItem;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.DataSlot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 铁砧合成：头盔（任意头戴装备）+ 普通玩偶 → 给头盔打上 doll_on_helmet 组件，
 * 佩戴后相当于同时佩戴头盔和玩偶。合成消耗 1 级经验，玩偶消耗 1 个。
 */
@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin {
    @Shadow @Final private DataSlot cost;
    @Shadow private int repairItemCountCost;

    @Inject(method = "createResult", at = @At("HEAD"), cancellable = true)
    private void kaleidoscopeDoll$combineDollWithHelmet(CallbackInfo ci) {
        ItemCombinerMenuAccessor self = (ItemCombinerMenuAccessor) this;
        ItemStack input = self.kaleidoscopeDoll$getInputSlots().getItem(0);
        ItemStack addition = self.kaleidoscopeDoll$getInputSlots().getItem(1);
        Equippable equippable = input.get(DataComponents.EQUIPPABLE);
        if (equippable == null || equippable.slot() != EquipmentSlot.HEAD) {
            return;
        }
        if (!(addition.getItem() instanceof DollItem) || input.getCount() != 1) {
            return;
        }

        ItemStack result = input.copy();
        Identifier dollId = BuiltInRegistries.ITEM.getKey(addition.getItem());
        result.set(ModDataComponent.DOLL_ON_HELMET, dollId.getPath());

        self.kaleidoscopeDoll$getResultSlots().setItem(0, result);
        this.repairItemCountCost = 1;
        this.cost.set(1);
        ((AbstractContainerMenu) (Object) this).broadcastChanges();
        ci.cancel();
    }
}
