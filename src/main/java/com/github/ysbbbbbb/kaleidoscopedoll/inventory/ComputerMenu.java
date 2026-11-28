package com.github.ysbbbbbb.kaleidoscopedoll.inventory;

import com.github.ysbbbbbb.kaleidoscopedoll.compat.DollInventory;
import com.github.ysbbbbbb.kaleidoscopedoll.KaleidoscopeDoll;
import com.github.ysbbbbbb.kaleidoscopedoll.data.custom.ServerCustomDollLoader;
import com.github.ysbbbbbb.kaleidoscopedoll.datagen.TagItem;
import com.github.ysbbbbbb.kaleidoscopedoll.init.ModContainer;
import com.github.ysbbbbbb.kaleidoscopedoll.item.CustomDollItem;
import net.minecraft.resources.Identifier;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

public class ComputerMenu extends AbstractContainerMenu {
    public static final MenuType<ComputerMenu> TYPE = ModContainer.COMPUTER_CONTAINER;
    private static final Identifier OUTPUT_SLOT = Identifier.fromNamespaceAndPath(KaleidoscopeDoll.MOD_ID, "slot/computer_output_slot");

    private final SimpleContainer input = new SimpleContainer(1);
    private final SimpleContainer output = new SimpleContainer(1) {
        @Override
        public int getMaxStackSize() {
            return 1;
        }
    };

    public ComputerMenu(int id, Inventory inventory) {
        super(TYPE, id);

        // 输入槽
        this.addSlot(new Slot(input, 0, 108, 98) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(TagItem.COMPUTER_TOKENS);
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                super.onTake(player, stack);
                // 清空输出槽
                output.setItem(0, ItemStack.EMPTY);
            }
        });

        // 输出槽
        this.addSlot(new Slot(output, 0, 151, 98) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                super.onTake(player, stack);
                consumeWool(1);
            }

            @Override
            public @Nullable Identifier getNoItemIcon() {
                return OUTPUT_SLOT;
            }
        });

        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(inventory, i, 8 + i * 18, 184));
        }

        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(inventory, j + i * 9 + 9, 8 + j * 18, 126 + i * 18));
            }
        }
    }

    public boolean clickDollButton(ItemStack doll, boolean batch) {
        ItemStack inputStack = this.input.getItem(0);
        // 防止作弊，必须检查一次
        if (!inputStack.is(TagItem.COMPUTER_TOKENS)) {
            return false;
        }

        ItemStack result;
        if (doll.is(TagItem.PLAYER_DOLLS)) {
            result = new ItemStack(doll.getItem());
        } else if (doll.is(TagItem.CUSTOM_DOLLS)) {
            String id = CustomDollItem.getModelId(doll);
            if (StringUtils.isBlank(id) || !ServerCustomDollLoader.getModels().contains(id)) {
                return false;
            }
            result = new ItemStack(doll.getItem());
            CustomDollItem.setModelId(result, id);
        } else {
            return false;
        }

        if (!batch && this.output.isEmpty()) {
            this.output.setItem(0, result);
            return true;
        }

        int requested = batch ? inputStack.getCount() : 1;
        result.setCount(requested);
        while (!result.isEmpty() && this.moveItemStackTo(result, 2, this.slots.size(), false)) {
            // 每次转移最多填入一个空槽。
        }
        int delivered = requested - result.getCount();
        if (delivered == 0) {
            return false;
        }
        this.consumeWool(delivered);
        return true;
    }

    private void consumeWool(int count) {
        ItemStack inputStack = this.input.getItem(0);
        inputStack.shrink(count);
        if (inputStack.isEmpty()) {
            this.input.setItem(0, ItemStack.EMPTY);
            // 最后一块羊毛用掉后，未领取的输出预览也失效。
            this.output.setItem(0, ItemStack.EMPTY);
        } else {
            this.input.setChanged();
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack slotItem = slot.getItem();
            itemStack = slotItem.copy();
            if (index < 2) {
                if (!this.moveItemStackTo(slotItem, 2, this.slots.size(), false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(slotItem, 0, 1, false)) {
                return ItemStack.EMPTY;
            }

            if (slotItem.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            if (index < 2) {
                slot.onTake(player, itemStack);
            }
        }

        return itemStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        ItemStack remaining = input.getItem(0);
        if (!remaining.isEmpty()) {
            if (!player.getInventory().add(remaining)) {
                DollInventory.drop(player, remaining);
            }
            input.setItem(0, ItemStack.EMPTY);
        }
    }
}
