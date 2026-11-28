package com.kaleidoscopedoll.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

public class TweaksToolItem extends Item {
    public TweaksToolItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltipComponents, TooltipFlag isAdvanced) {
        tooltipComponents.accept(Component.translatable("tooltip.kaleidoscope_doll.tweaks_tool.1").withStyle(ChatFormatting.DARK_GRAY));
        tooltipComponents.accept(Component.translatable("tooltip.kaleidoscope_doll.tweaks_tool.2").withStyle(ChatFormatting.DARK_GRAY));
    }
}