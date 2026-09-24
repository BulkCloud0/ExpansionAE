package com.bulkcloud.expansionae.client;

import java.util.List;

import com.mojang.blaze3d.matrix.MatrixStack;

import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Slot;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;

import com.bulkcloud.expansionae.feature.extendedprovider.Interface36Container;

import appeng.api.config.Settings;
import appeng.api.config.YesNo;
import appeng.client.gui.Icon;
import appeng.client.gui.implementations.UpgradeableScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.ServerSettingToggleButton;
import appeng.client.gui.widgets.ToggleButton;
import appeng.container.SlotSemantic;
import appeng.core.localization.GuiText;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.ConfigButtonPacket;

public final class Interface36Screen
        extends UpgradeableScreen<Interface36Container> {
    private enum Page {
        CONFIG,
        STORAGE,
        PATTERNS
    }

    private static final int PANEL = 0xFF202831;
    private static final int PANEL_INNER = 0xFF11161C;
    private static final int SLOT_BORDER = 0xFF627482;
    private static final int ACCENT = 0xFF9A7DFF;
    private static final int GRID_X = 34;
    private static final int GRID_Y = 55;

    private final ServerSettingToggleButton<YesNo> blockMode;
    private final ToggleButton interfaceMode;
    private Page page = Page.CONFIG;

    public Interface36Screen(
            Interface36Container container,
            PlayerInventory inventory,
            ITextComponent title,
            ScreenStyle style) {
        super(container, inventory, title, style);
        this.xSize = 176;
        this.ySize = 260;

        this.widgets.addOpenPriorityButton();

        this.blockMode =
                new ServerSettingToggleButton<>(Settings.BLOCK, YesNo.NO);
        this.addToLeftToolbar(this.blockMode);

        this.interfaceMode = new ToggleButton(
                Icon.INTERFACE_TERMINAL_SHOW,
                Icon.INTERFACE_TERMINAL_HIDE,
                GuiText.InterfaceTerminal.text(),
                GuiText.InterfaceTerminalHint.text(),
                button -> this.selectNextInterfaceMode());
        this.addToLeftToolbar(this.interfaceMode);
    }

    @Override
    protected void init() {
        super.init();

        this.addButton(new Button(
                this.guiLeft + 8,
                this.guiTop + 28,
                50,
                18,
                new StringTextComponent("Config"),
                button -> this.setPage(Page.CONFIG)));
        this.addButton(new Button(
                this.guiLeft + 63,
                this.guiTop + 28,
                50,
                18,
                new StringTextComponent("Storage"),
                button -> this.setPage(Page.STORAGE)));
        this.addButton(new Button(
                this.guiLeft + 118,
                this.guiTop + 28,
                50,
                18,
                new StringTextComponent("Patterns"),
                button -> this.setPage(Page.PATTERNS)));

        this.applyPageLayout();
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        this.blockMode.set(this.container.getBlockingMode());
        this.interfaceMode.setState(
                this.container.getInterfaceTerminalMode() == YesNo.YES);
    }

    @Override
    public void drawBG(
            MatrixStack matrixStack,
            int offsetX,
            int offsetY,
            int mouseX,
            int mouseY,
            float partialTicks) {
        fill(
                matrixStack,
                offsetX,
                offsetY,
                offsetX + this.xSize,
                offsetY + this.ySize,
                PANEL);
        fill(
                matrixStack,
                offsetX + 4,
                offsetY + 48,
                offsetX + this.xSize - 4,
                offsetY + 166,
                PANEL_INNER);
        fill(
                matrixStack,
                offsetX + 4,
                offsetY + 171,
                offsetX + this.xSize - 4,
                offsetY + this.ySize - 6,
                PANEL_INNER);
        fill(
                matrixStack,
                offsetX + 4,
                offsetY + 22,
                offsetX + this.xSize - 4,
                offsetY + 24,
                ACCENT);

        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 6; col++) {
                int x = offsetX + GRID_X - 1 + col * 18;
                int y = offsetY + GRID_Y - 1 + row * 18;
                fill(matrixStack, x, y, x + 18, y + 18, SLOT_BORDER);
                fill(matrixStack, x + 1, y + 1, x + 17, y + 17, 0xFF2F3942);
            }
        }
    }

    @Override
    public void drawFG(
            MatrixStack matrixStack,
            int offsetX,
            int offsetY,
            int mouseX,
            int mouseY) {
        this.font.drawString(
                matrixStack,
                this.title.getString(),
                8,
                7,
                0xE8F7FF);
        this.font.drawString(
                matrixStack,
                "36 " + this.page.name().toLowerCase() + " slots",
                8,
                158,
                0x9A7DFF);
        this.font.drawString(
                matrixStack,
                this.playerInventory.getDisplayName().getString(),
                8,
                172,
                0xB8C4CC);
    }

    private void setPage(Page page) {
        this.page = page;
        this.applyPageLayout();
    }

    private void applyPageLayout() {
        this.layoutSemantic(SlotSemantic.CONFIG, this.page == Page.CONFIG);
        this.layoutSemantic(SlotSemantic.STORAGE, this.page == Page.STORAGE);
        this.layoutSemantic(
                SlotSemantic.ENCODED_PATTERN,
                this.page == Page.PATTERNS);
    }

    private void layoutSemantic(SlotSemantic semantic, boolean visible) {
        List<Slot> slots = this.container.getSlots(semantic);
        for (int index = 0; index < slots.size(); index++) {
            Slot slot = slots.get(index);
            if (visible) {
                int col = index % 6;
                int row = index / 6;
                slot.xPos = GRID_X + col * 18;
                slot.yPos = GRID_Y + row * 18;
            } else {
                slot.xPos = -9999;
                slot.yPos = -9999;
            }
        }
    }

    private void selectNextInterfaceMode() {
        NetworkHandler.instance().sendToServer(
                new ConfigButtonPacket(
                        Settings.INTERFACE_TERMINAL,
                        this.isHandlingRightClick()));
    }
}
