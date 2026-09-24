package com.bulkcloud.expansionae.client;

import com.mojang.blaze3d.matrix.MatrixStack;

import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.text.ITextComponent;

import com.bulkcloud.expansionae.feature.extendedprovider.PatternProvider36Container;

public final class PatternProvider36Screen
        extends ContainerScreen<PatternProvider36Container> {
    private static final int PANEL = 0xFF202831;
    private static final int PANEL_INNER = 0xFF11161C;
    private static final int SLOT_BORDER = 0xFF627482;
    private static final int ACCENT = 0xFF67D8FF;

    public PatternProvider36Screen(
            PatternProvider36Container container,
            PlayerInventory inventory,
            ITextComponent title) {
        super(container, inventory, title);
        this.xSize = 176;
        this.ySize = 228;
    }

    @Override
    public void render(
            MatrixStack matrixStack,
            int mouseX,
            int mouseY,
            float partialTicks) {
        this.renderBackground(matrixStack);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        this.renderHoveredTooltip(matrixStack, mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(
            MatrixStack matrixStack,
            float partialTicks,
            int mouseX,
            int mouseY) {
        fill(
                matrixStack,
                this.guiLeft,
                this.guiTop,
                this.guiLeft + this.xSize,
                this.guiTop + this.ySize,
                PANEL);
        fill(
                matrixStack,
                this.guiLeft + 4,
                this.guiTop + 16,
                this.guiLeft + this.xSize - 4,
                this.guiTop + 132,
                PANEL_INNER);
        fill(
                matrixStack,
                this.guiLeft + 4,
                this.guiTop + 136,
                this.guiLeft + this.xSize - 4,
                this.guiTop + this.ySize - 6,
                PANEL_INNER);
        fill(
                matrixStack,
                this.guiLeft + 4,
                this.guiTop + 14,
                this.guiLeft + this.xSize - 4,
                this.guiTop + 16,
                ACCENT);

        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 6; col++) {
                int x = this.guiLeft + 34 + col * 18;
                int y = this.guiTop + 21 + row * 18;
                fill(matrixStack, x, y, x + 18, y + 18, SLOT_BORDER);
                fill(matrixStack, x + 1, y + 1, x + 17, y + 17, 0xFF2F3942);
            }
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(
            MatrixStack matrixStack,
            int mouseX,
            int mouseY) {
        this.font.drawString(
                matrixStack,
                this.title.getString(),
                8,
                5,
                0xE8F7FF);
        this.font.drawString(
                matrixStack,
                "36 encoded patterns",
                8,
                124,
                0x67D8FF);
        this.font.drawString(
                matrixStack,
                this.playerInventory.getDisplayName().getString(),
                8,
                135,
                0xB8C4CC);
    }
}
