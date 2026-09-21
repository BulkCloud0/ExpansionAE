package dev.bulkcloud.expansionae.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import dev.bulkcloud.expansionae.QuantumArmorConfigContainer;
import dev.bulkcloud.expansionae.QuantumUpgradeType;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;

public final class QuantumArmorConfigScreen extends ContainerScreen<QuantumArmorConfigContainer> {
    private final Button[] upgradeButtons = new Button[QuantumUpgradeType.values().length];
    private final Button[] removeButtons = new Button[QuantumUpgradeType.values().length];

    public QuantumArmorConfigScreen(QuantumArmorConfigContainer container, PlayerInventory inventory,
            ITextComponent title) {
        super(container, inventory, title);
        this.xSize = 220;
        this.ySize = 220;
    }

    @Override
    protected void init() {
        super.init();

        String[] armorNames = {"Helmet", "Chest", "Legs", "Boots"};
        for (int i = 0; i < armorNames.length; i++) {
            final int index = i;
            addButton(new Button(guiLeft + 8 + i * 51, guiTop + 20, 49, 18,
                    new StringTextComponent(armorNames[i]), b -> container.selectArmor(index)));
        }

        QuantumUpgradeType[] types = QuantumUpgradeType.values();
        for (int i = 0; i < types.length; i++) {
            final QuantumUpgradeType type = types[i];
            upgradeButtons[i] = addButton(new Button(guiLeft + 8, guiTop + 44, 178, 17,
                    label(type), b -> {
                        if (type == QuantumUpgradeType.AUTO_STOCK
                                && container.isInstalled(type) && Screen.hasShiftDown()) {
                            container.captureAutoStock();
                        } else {
                            container.installOrToggle(type);
                        }
                    }));
            removeButtons[i] = addButton(new Button(guiLeft + 188, guiTop + 44, 24, 17,
                    new StringTextComponent("X"), b -> container.uninstall(type)));
        }
        updateButtons();
    }

    @Override
    public void tick() {
        super.tick();
        updateButtons();
    }

    private void updateButtons() {
        int row = 0;
        for (QuantumUpgradeType type : QuantumUpgradeType.values()) {
            Button upgrade = upgradeButtons[type.ordinal()];
            Button remove = removeButtons[type.ordinal()];
            if (upgrade == null || remove == null) continue;

            boolean compatible = type.supports(container.getSelectedEquipmentSlot());
            upgrade.visible = compatible;
            remove.visible = compatible && container.isInstalled(type);
            if (!compatible) continue;

            int y = guiTop + 44 + row * 20;
            upgrade.y = y;
            remove.y = y;
            upgrade.setMessage(label(type));
            row++;
        }
    }

    private ITextComponent label(QuantumUpgradeType type) {
        String state;
        if (!container.isInstalled(type)) state = "INSTALL";
        else state = container.isEnabled(type) ? "ON" : "OFF";
        String suffix = type == QuantumUpgradeType.AUTO_STOCK && container.isInstalled(type)
                ? " (Shift: capture)" : "";
        return new StringTextComponent("[" + state + "] " + pretty(type.id()) + suffix);
    }

    private static String pretty(String id) {
        String[] words = id.split("_");
        StringBuilder out = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (out.length() > 0) out.append(' ');
            out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return out.toString();
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(MatrixStack matrixStack, float partialTicks,
            int mouseX, int mouseY) {
        fill(matrixStack, guiLeft, guiTop, guiLeft + xSize, guiTop + ySize, 0xFF202020);
        fill(matrixStack, guiLeft + 4, guiTop + 4, guiLeft + xSize - 4, guiTop + 17, 0xFF303030);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(MatrixStack matrixStack, int mouseX, int mouseY) {
        font.drawString(matrixStack, title.getString(), 8, 6, 0xFFFFFF);
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        renderBackground(matrixStack);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        renderHoveredTooltip(matrixStack, mouseX, mouseY);
    }
}
