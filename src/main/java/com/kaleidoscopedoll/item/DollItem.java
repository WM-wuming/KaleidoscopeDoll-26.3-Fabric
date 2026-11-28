package com.kaleidoscopedoll.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.function.Consumer;

public class DollItem extends BlockItem {
    private final String langKey;

    public DollItem(Block block, String langKey, Item.Properties properties) {
        super(block, properties);
        this.langKey = "tooltip.kaleidoscope_doll.doll." + langKey;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("block.kaleidoscope_doll.doll");
    }

    /**
     * 玩偶介绍的语言键（tooltip.kaleidoscope_doll.doll.xxx），
     * 供实体玩偶物品复用显示同一行介绍
     */
    public String tooltipKey() {
        return this.langKey;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> list, TooltipFlag flag) {
        list.accept(Component.translatable(this.langKey).withStyle(ChatFormatting.DARK_GRAY));
    }
}
