package com.github.ysbbbbbb.kaleidoscopedoll.item;

import com.github.ysbbbbbb.kaleidoscopedoll.block.entity.CustomDollBlockEntity;
import com.github.ysbbbbbb.kaleidoscopedoll.client.ClientHooks;
import com.github.ysbbbbbb.kaleidoscopedoll.data.custom.ServerCustomDollLoader;
import com.github.ysbbbbbb.kaleidoscopedoll.init.ModDataComponent;
import com.github.ysbbbbbb.kaleidoscopedoll.init.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class CustomDollItem extends BlockItem {
    public CustomDollItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Nullable
    public static String getModelId(ItemStack doll) {
        return doll.get(ModDataComponent.TAG_CUSTOM_DOLL_ID);
    }

    public static void setModelId(ItemStack doll, String modelId) {
        doll.set(ModDataComponent.TAG_CUSTOM_DOLL_ID, modelId);
    }

    public static void addCreativeTab(CreativeModeTab.Output output) {
        output.accept(new ItemStack(ModItems.CUSTOM_DOLL));
        ServerCustomDollLoader.getModels().forEach(modelId -> {
            ItemStack dollStack = new ItemStack(ModItems.CUSTOM_DOLL);
            setModelId(dollStack, modelId);
            output.accept(dollStack);
        });
    }

    public static ItemStack getDefaultItemStack() {
        ItemStack dollStack = new ItemStack(ModItems.CUSTOM_DOLL);
        ServerCustomDollLoader.getModels().stream().findFirst().ifPresent(modelId -> {
            setModelId(dollStack, modelId);
        });
        return dollStack;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("block.kaleidoscope_doll.doll");
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> list, TooltipFlag flag) {
        String modelId = getModelId(stack);
        if (modelId != null && !modelId.isBlank()) {
            ClientHooks.customDollTooltip.accept(modelId, list);
        } else {
            list.accept(Component.translatable("tooltip.kaleidoscope_doll.custom.unknown").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
