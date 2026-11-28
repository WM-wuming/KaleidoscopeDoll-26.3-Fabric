package com.kaleidoscopedoll.client.gui;

import com.kaleidoscopedoll.KaleidoscopeDoll;
import com.kaleidoscopedoll.client.custom.CustomDollLoader;
import com.kaleidoscopedoll.data.custom.ServerCustomDollLoader;
import com.kaleidoscopedoll.datagen.TagItem;
import com.kaleidoscopedoll.init.ModItems;
import com.kaleidoscopedoll.inventory.ComputerMenu;
import com.kaleidoscopedoll.item.CustomDollItem;
import com.kaleidoscopedoll.network.NetworkHandler;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.StreamSupport;

import static com.kaleidoscopedoll.event.ModRegisterEvent.SPECIAL_TOOLTIPS;

public class ComputerMenuScreen extends AbstractContainerScreen<ComputerMenu> {
    private static final Identifier BG = Identifier.fromNamespaceAndPath(KaleidoscopeDoll.MOD_ID, "textures/gui/computer.png");
    private static final int MAX_DOLLS_PER_PAGE = 32;

    private List<ItemStack> dolls = Lists.newArrayList();
    private int currentPage = 0;
    private float scrollOffs;
    private boolean scrolling;

    private EditBox searchField;

    public ComputerMenuScreen(ComputerMenu screenContainer, Inventory inv, Component titleIn) {
        super(screenContainer, inv, titleIn, 176, 208);
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();
        this.initSearch();
        this.initDollButtons();
    }

    private void initDollButtons() {
        String searchText = this.searchField.getValue().toLowerCase(Locale.ENGLISH);

        this.dolls.clear();
        if (StringUtils.isEmpty(searchText)) {
            this.getDolls();
        } else {
            this.getSearchDolls(searchText);
        }

        int start = currentPage * MAX_DOLLS_PER_PAGE;
        int end = Math.min(start + MAX_DOLLS_PER_PAGE, this.dolls.size());
        List<ItemStack> dollsToShow = this.dolls.subList(start, end);
        int xPos = this.leftPos + 7;
        int yPos = this.topPos + 17;
        int xOffset = 18;
        int yOffset = 18;
        for (int i = 0; i < dollsToShow.size(); i++) {
            ItemStack item = dollsToShow.get(i);
            int x = xPos + (i % 8) * xOffset;
            int y = yPos + (i / 8) * yOffset;
            this.addRenderableWidget(new DollButton(x, y, item, (button, batch) -> {
                button.setFocused(!button.isFocused());
                if (batch && ClientPlayNetworking.canSend(NetworkHandler.ComputerDollBatchClickPayload.TYPE)) {
                    ClientPlayNetworking.send(new NetworkHandler.ComputerDollBatchClickPayload(item));
                } else {
                    ClientPlayNetworking.send(new NetworkHandler.ComputerDollClickPayload(item));
                }
            }));
        }
    }

    private void getDolls() {
        // 自定义玩偶
        ServerCustomDollLoader.getModels().forEach(id -> {
            ItemStack stack = ModItems.CUSTOM_DOLL.getDefaultInstance();
            CustomDollItem.setModelId(stack, id);
            this.dolls.add(stack);
        });

        this.dolls.addAll(StreamSupport.stream(BuiltInRegistries.ITEM.getTagOrEmpty(TagItem.PLAYER_DOLLS).spliterator(), false)
                .map(Holder::value)
                .map(Item::getDefaultInstance)
                .toList());
    }

    private void getSearchDolls(String searchText) {
        // 自定义玩偶
        ServerCustomDollLoader.getModels().forEach(id -> {
            if (doSearchCustom(id, searchText)) {
                ItemStack stack = ModItems.CUSTOM_DOLL.getDefaultInstance();
                CustomDollItem.setModelId(stack, id);
                this.dolls.add(stack);
            }
        });

        this.dolls.addAll(StreamSupport.stream(BuiltInRegistries.ITEM.getTagOrEmpty(TagItem.PLAYER_DOLLS).spliterator(), false)
                .map(Holder::value)
                .filter(item -> doSearch(item, searchText))
                .map(Item::getDefaultInstance)
                .toList());
    }

    private boolean doSearch(Item item, String searchText) {
        // 可以直接搜索物品 id 或者文本描述
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        if (id.toString().contains(searchText)) {
            return true;
        }
        String key = SPECIAL_TOOLTIPS.get(id);
        if (key == null) {
            return false;
        }
        // 语言文件的 key 也可作为搜索判断
        if (key.contains(searchText)) {
            return true;
        }
        return I18n.get("tooltip.kaleidoscope_doll.doll." + key).contains(searchText);
    }

    private boolean doSearchCustom(String id, String searchText) {
        // 可以直接搜索物品 id 或者文本描述
        if (id.contains(searchText)) {
            return true;
        }

        String local = Minecraft.getInstance().getLanguageManager().getSelected();
        String language = CustomDollLoader.getLanguage(local, id);
        // 语言文件搜索
        if (language.toLowerCase(Locale.ROOT).contains(searchText)) {
            return true;
        }

        // 回退搜索英文
        if (!"en_us".equals(local)) {
            language = CustomDollLoader.getLanguage("en_us", id);
            return language.toLowerCase(Locale.ROOT).contains(searchText);
        }

        return false;
    }

    private void initSearch() {
        String perText = "";
        boolean focus = false;
        if (searchField != null) {
            perText = searchField.getValue();
            focus = searchField.isFocused();
        }
        searchField = new EditBox(this.font, leftPos + 8, topPos + 102, 85, 16, Component.literal("Doll Search Box"));
        searchField.setValue(perText);
        searchField.setBordered(false);
        searchField.setMaxLength(32);
        searchField.setTextColor(0xFF39FF56);
        searchField.setFocused(focus);
        searchField.moveCursorToEnd(false);
        this.addWidget(this.searchField);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        graphics.text(this.font, Component.translatable("block.kaleidoscope_doll.computer"),
                this.titleLabelX, this.titleLabelY, 0xFF404040, false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int posX = this.leftPos;
        int posY = (this.height - this.imageHeight) / 2;
        graphics.blit(RenderPipelines.GUI_TEXTURED, BG, posX, posY, 0, 0, this.imageWidth, this.imageHeight, 256, 256);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int x, int y, float partialTicks) {
        super.extractRenderState(graphics, x, y, partialTicks);

        int uOffset = this.isScrollBarActive() ? 0 : 14;
        int offset = (int) (57 * this.scrollOffs);
        graphics.blit(RenderPipelines.GUI_TEXTURED, BG, this.leftPos + 155, this.topPos + 17 + offset,
                177 + uOffset, 0, 12, 15, 256, 256);

        this.searchField.extractRenderState(graphics, x, y, partialTicks);
        if (!this.searchField.isFocused() && this.searchField.getValue().isEmpty()) {
            graphics.text(font, Component.translatable("gui.kaleidoscope_doll.computer.search")
                            .withStyle(ChatFormatting.GRAY),
                    this.leftPos + 9, this.topPos + 102, 0xFFFFFFFF, false);
        }

        if (this.isHovering(108, 98, 16, 16, x, y)
            && this.hoveredSlot != null && !this.hoveredSlot.hasItem()) {
            graphics.setTooltipForNextFrame(Component.translatable("tooltip.kaleidoscope_doll.computer.input"), x, y);
        }
    }

    @Override
    public void resize(int width, int height) {
        String value = this.searchField.getValue();
        super.resize(width, height);
        this.searchField.setValue(value);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (this.searchField.mouseClicked(event, doubleClick)) {
            this.setFocused(this.searchField);
            return true;
        } else {
            this.searchField.setFocused(false);
        }

        this.scrolling = false;
        double scrollX = this.leftPos + 155;
        double scrollY = this.topPos + 17;
        if (scrollX <= event.x() && event.x() < (scrollX + 12) && scrollY <= event.y() && event.y() < (scrollY + 72)) {
            this.scrolling = true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (this.scrolling && this.isScrollBarActive()) {
            float yOffset = (float) event.y() - this.topPos - 17;
            this.scrollOffs = Mth.clamp(yOffset / 57, 0F, 1F);
            this.currentPage = (int) ((this.scrollOffs * this.getOffscreenRows()) + 0.5);
            this.init();
            return true;
        } else {
            return super.mouseDragged(event, dragX, dragY);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.isScrollBarActive()) {
            int offscreenRows = this.getOffscreenRows();
            float scrollAmount = (float) scrollY / offscreenRows;
            this.scrollOffs = Mth.clamp(this.scrollOffs - scrollAmount, 0.0F, 1.0F);
            this.currentPage = (int) ((this.scrollOffs * offscreenRows) + 0.5);
            this.init();
        }
        return true;
    }

    private int getOffscreenRows() {
        return (this.dolls.size() + MAX_DOLLS_PER_PAGE - 1) / MAX_DOLLS_PER_PAGE - 1;
    }

    private boolean isScrollBarActive() {
        return this.dolls.size() > MAX_DOLLS_PER_PAGE;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        String preText = this.searchField.getValue();
        if (this.searchField.keyPressed(event)) {
            if (!Objects.equals(preText, this.searchField.getValue())) {
                currentPage = 0;
                this.init();
            }
            return true;
        } else {
            return this.searchField.isFocused()
                   && this.searchField.isVisible()
                   && event.key() != InputConstants.KEY_ESCAPE
                   || super.keyPressed(event);
        }
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (searchField == null) {
            return false;
        }
        String perText = this.searchField.getValue();
        if (this.searchField.charTyped(event)) {
            if (!Objects.equals(perText, this.searchField.getValue())) {
                currentPage = 0;
                this.init();
            }
            return true;
        }
        return false;
    }

    @Override
    protected void insertText(String text, boolean overwrite) {
        if (overwrite) {
            this.searchField.setValue(text);
        } else {
            this.searchField.insertText(text);
        }
    }
}
