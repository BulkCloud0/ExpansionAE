/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2014, AlgorithmX2, All rights reserved.
 *
 * Applied Energistics 2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Applied Energistics 2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Applied Energistics 2.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package dev.bulkcloud.expansionae.client;
// Adapted for ExpansionAE on 2026-09-18.
import appeng.client.gui.implementations.UpgradeableScreen;
import dev.bulkcloud.expansionae.ExpandedContainer;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.text.ITextComponent;

import appeng.api.config.Settings;
import appeng.api.config.YesNo;
import appeng.client.gui.Icon;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.ServerSettingToggleButton;
import appeng.client.gui.widgets.SettingToggleButton;
import appeng.client.gui.widgets.ToggleButton;

import appeng.core.localization.GuiText;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.ConfigButtonPacket;

public class ExpandedScreen extends UpgradeableScreen<ExpandedContainer> {

    @Override
    public void drawBG(com.mojang.blaze3d.matrix.MatrixStack pose, int x, int y, int mouseX, int mouseY, float partial) {
        fill(pose, x, y, x + this.xSize, y + this.ySize, 0xff373737);
        fill(pose, x + 1, y + 1, x + this.xSize - 1, y + this.ySize - 1, 0xffeeeeee);
        fill(pose, x + 3, y + 3, x + this.xSize - 3, y + this.ySize - 3, 0xffc6c6c6);
        for (net.minecraft.inventory.container.Slot slot : container.inventorySlots) {
            int sx = x + slot.xPos, sy = y + slot.yPos;
            if (sx < x || sy < y || sx + 16 > x + xSize || sy + 16 > y + ySize) continue;
            fill(pose, sx - 1, sy - 1, sx + 17, sy + 17, 0xffeeeeee);
            fill(pose, sx - 1, sy - 1, sx + 16, sy + 16, 0xff373737);
            fill(pose, sx, sy, sx + 16, sy + 16, 0xff8b8b8b);
        }
    }

    private final SettingToggleButton<YesNo> blockMode;
    private final ToggleButton interfaceMode;

    public ExpandedScreen(ExpandedContainer container, PlayerInventory playerInventory, ITextComponent title,
            ScreenStyle style) {
        super(container, playerInventory, title, style);

        widgets.addOpenPriorityButton();

        this.blockMode = new ServerSettingToggleButton<>(Settings.BLOCK, YesNo.NO);
        this.addToLeftToolbar(this.blockMode);

        this.interfaceMode = new ToggleButton(Icon.INTERFACE_TERMINAL_SHOW, Icon.INTERFACE_TERMINAL_HIDE,
                GuiText.InterfaceTerminal.text(), GuiText.InterfaceTerminalHint.text(),
                btn -> selectNextInterfaceMode());
        this.addToLeftToolbar(this.interfaceMode);
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();

        this.blockMode.set(this.container.getBlockingMode());
        this.interfaceMode.setState(this.container.getInterfaceTerminalMode() == YesNo.YES);
    }

    private void selectNextInterfaceMode() {
        final boolean backwards = isHandlingRightClick();
        NetworkHandler.instance().sendToServer(new ConfigButtonPacket(Settings.INTERFACE_TERMINAL, backwards));
    }

}
