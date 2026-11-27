package com.github.ysbbbbbb.kaleidoscopedoll.client.gui;

import com.github.ysbbbbbb.kaleidoscopedoll.KaleidoscopeDoll;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public class DollButton extends AbstractWidget {
    private static final Identifier BG = Identifier.fromNamespaceAndPath(KaleidoscopeDoll.MOD_ID, "textures/gui/computer.png");
    private final ItemStack doll;
    private final OnPress onPress;

    public DollButton(int x, int y, ItemStack doll, OnPress onPress) {
        super(x, y, 18, 18, Component.empty());
        this.doll = doll;
        this.onPress = onPress;
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        this.onPress.onPress(this, event.hasShiftDown());
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // 先画背景再画物品，26.3 新 GUI 管线没有旧版的深度测试语义，后画的会直接覆盖先画的
        int v = 16;
        if (this.isFocused()) {
            v += 18;
        } else if (this.isHovered()) {
            v += 36;
        }
        graphics.blit(RenderPipelines.GUI_TEXTURED, BG, this.getX(), this.getY(), 177, v, 18, 18, 256, 256);
        graphics.item(this.doll, this.getX() + 1, this.getY() + 1);
        if (this.isHovered()) {
            graphics.setTooltipForNextFrame(Minecraft.getInstance().font, this.doll, mouseX, mouseY);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }

    @FunctionalInterface
    public interface OnPress {
        void onPress(DollButton button, boolean batch);
    }
}
