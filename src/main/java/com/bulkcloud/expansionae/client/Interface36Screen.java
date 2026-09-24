package com.bulkcloud.expansionae.client;

import com.mojang.blaze3d.matrix.MatrixStack;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.text.ITextComponent;

import com.bulkcloud.expansionae.feature.extendedprovider.Interface36Container;

import appeng.api.config.Settings;
import appeng.api.config.YesNo;
import appeng.client.gui.Icon;
import appeng.client.gui.implementations.UpgradeableScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.ServerSettingToggleButton;
import appeng.client.gui.widgets.ToggleButton;
import appeng.core.localization.GuiText;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.ConfigButtonPacket;

public final class Interface36Screen
        extends UpgradeableScreen<Interface36Container> {
    private static final int PANEL = 0xFF202831;
    private static final int PANEL_INNER = 0xFF11161C;
    private static final int SLOT_BORDER = 0xFF627482;
    private static final int ACCENT = 0xFF9A7DFF;

    private final ServerSettingToggleButton<YesNo> blockMode;
    private final ToggleButton interfaceMode;

    public Interface36Screen(
            Interface36Container container,
            PlayerInventory inventory,
            ITextComponent title,
            ScreenStyle style) {
        super(container, inventory, title, style);
        this.xSize = 176;
        this.ySize = 360;

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

        panel(matrixStack, offsetX, offsetY, 30, 106);
        panel(matrixStack, offsetX, offsetY, 110, 186);
        panel(matrixStack, offsetX, offsetY, 190, 266);
        panel(matrixStack, offsetX, offsetY, 272, 354);

        fill(
                matrixStack,
                offsetX + 4,
                offsetY + 20,
                offsetX + this.xSize - 4,
                offsetY + 22,
                ACCENT);

        drawGrid(matrixStack, offsetX + 7, offsetY + 34);
        drawGrid(matrixStack, offsetX + 7, offsetY + 114);
        drawGrid(matrixStack, offsetX + 7, offsetY + 194);
    }

    private static void panel(
            MatrixStack matrices,
            int x,
            int y,
            int top,
            int bottom) {
        fill(matrices, x + 4, y + top, x + 172, y + bottom, PANEL_INNER);
    }

    private static void drawGrid(MatrixStack matrices, int startX, int startY) {
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 9; col++) {
                int x = startX + col * 18;
                int y = startY + row * 18;
                fill(matrices, x, y, x + 18, y + 18, SLOT_BORDER);
                fill(matrices, x + 1, y + 1, x + 17, y + 17, 0xFF2F3942);
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
        this.font.drawString(matrixStack, "Config - 36", 8, 24, ACCENT);
        this.font.drawString(matrixStack, "Storage - 36", 8, 104, ACCENT);
        this.font.drawString(matrixStack, "Patterns - 36", 8, 184, ACCENT);
        this.font.drawString(
                matrixStack,
                this.playerInventory.getDisplayName().getString(),
                8,
                268,
                0xB8C4CC);
    }

    private void selectNextInterfaceMode() {
        NetworkHandler.instance().sendToServer(
                new ConfigButtonPacket(
                        Settings.INTERFACE_TERMINAL,
                        this.isHandlingRightClick()));
    }
}
