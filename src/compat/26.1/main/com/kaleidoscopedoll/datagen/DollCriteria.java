package com.kaleidoscopedoll.datagen;

import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.criterion.InventoryChangeTrigger;
import net.minecraft.advancements.criterion.PlayerTrigger;
import net.minecraft.world.item.Item;

final class DollCriteria {
    private DollCriteria() {
    }

    static Criterion<?> tick() {
        return PlayerTrigger.TriggerInstance.tick();
    }

    static Criterion<?> hasItem(Item item) {
        return InventoryChangeTrigger.TriggerInstance.hasItems(item);
    }
}
