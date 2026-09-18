package dev.bulkcloud.expansionae.client;

import com.mojang.blaze3d.matrix.MatrixStack;

import appeng.api.config.FullnessMode;
import appeng.api.config.OperationMode;
import appeng.api.config.RedstoneMode;
import appeng.api.config.Settings;
import appeng.client.gui.implementations.UpgradeableScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.ServerSettingToggleButton;
import appeng.client.gui.widgets.SettingToggleButton;
import appeng.core.Api;
import dev.bulkcloud.expansionae.ExpandedIOPortContainer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.text.ITextComponent;

public final class ExpandedIOPortScreen extends UpgradeableScreen<ExpandedIOPortContainer> {
    private final SettingToggleButton<FullnessMode> fullness;
    private final SettingToggleButton<OperationMode> operation;
    private final SettingToggleButton<RedstoneMode> redstone;

    public ExpandedIOPortScreen(ExpandedIOPortContainer container, PlayerInventory player,
            ITextComponent title, ScreenStyle style) {
        super(container, player, title, style);
        fullness = new ServerSettingToggleButton<>(Settings.FULLNESS_MODE, FullnessMode.EMPTY);
        operation = new ServerSettingToggleButton<>(Settings.OPERATION_MODE, OperationMode.EMPTY);
        redstone = new ServerSettingToggleButton<>(Settings.REDSTONE_CONTROLLED, RedstoneMode.IGNORE);
        addToLeftToolbar(fullness);
        addToLeftToolbar(redstone);
        widgets.add("operationMode", operation);
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        fullness.set(container.getFullMode());
        operation.set(container.getOperationMode());
        redstone.set(container.getRedStoneMode());
    }

    @Override
    public void drawBG(MatrixStack matrices, int offsetX, int offsetY,
            int mouseX, int mouseY, float partialTicks) {
        super.drawBG(matrices, offsetX, offsetY, mouseX, mouseY, partialTicks);
        Api.instance().definitions().items().cell1k().maybeStack(1)
                .ifPresent(stack -> drawItem(offsetX + 58, offsetY + 17, stack));
        Api.instance().definitions().blocks().drive().maybeStack(1)
                .ifPresent(stack -> drawItem(offsetX + 102, offsetY + 17, stack));
    }
}
