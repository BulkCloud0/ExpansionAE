package dev.bulkcloud.expansionae.client;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.style.ScreenStyle;
import com.mojang.blaze3d.matrix.MatrixStack;
import dev.bulkcloud.expansionae.AssemblerMatrixPatternContainer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.text.ITextComponent;

public final class AssemblerMatrixPatternScreen extends AEBaseScreen<AssemblerMatrixPatternContainer> {
    public AssemblerMatrixPatternScreen(AssemblerMatrixPatternContainer container, PlayerInventory inventory,
            ITextComponent title, ScreenStyle style) {
        super(container, inventory, title, style);
    }

    @Override
    public void drawBG(MatrixStack pose, int x, int y, int mouseX, int mouseY, float partialTicks) {
        fill(pose, x, y, x + xSize, y + ySize, 0xff373737);
        fill(pose, x + 1, y + 1, x + xSize - 1, y + ySize - 1, 0xffeeeeee);
        fill(pose, x + 3, y + 3, x + xSize - 3, y + ySize - 3, 0xffc6c6c6);
        for (net.minecraft.inventory.container.Slot slot : container.inventorySlots) {
            int sx = x + slot.xPos, sy = y + slot.yPos;
            if (sx < x || sy < y || sx + 16 > x + xSize || sy + 16 > y + ySize) continue;
            fill(pose, sx - 1, sy - 1, sx + 17, sy + 17, 0xffeeeeee);
            fill(pose, sx - 1, sy - 1, sx + 16, sy + 16, 0xff373737);
            fill(pose, sx, sy, sx + 16, sy + 16, 0xff8b8b8b);
        }
    }
}
