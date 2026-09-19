package dev.bulkcloud.expansionae.client;

import java.util.Collections;
import java.util.List;

import appeng.api.config.ActionItems;
import appeng.api.config.CopyMode;
import appeng.api.config.FuzzyMode;
import appeng.api.config.Settings;
import appeng.api.config.Upgrades;
import appeng.api.implementations.items.IUpgradeModule;
import appeng.client.gui.Icon;
import appeng.client.gui.implementations.UpgradeableScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.ActionButton;
import appeng.client.gui.widgets.SettingToggleButton;
import appeng.client.gui.widgets.ToggleButton;
import appeng.core.localization.GuiText;
import dev.bulkcloud.expansionae.ExpansionNetwork;
import dev.bulkcloud.expansionae.PortableWorkbenchAction;
import dev.bulkcloud.expansionae.PortableWorkbenchContainer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.items.IItemHandler;

public final class PortableWorkbenchScreen extends UpgradeableScreen<PortableWorkbenchContainer> {
    private final ToggleButton copyMode;
    private final SettingToggleButton<FuzzyMode> fuzzyMode;

    public PortableWorkbenchScreen(PortableWorkbenchContainer container, PlayerInventory playerInventory,
            ITextComponent title, ScreenStyle style) {
        super(container, playerInventory, title, style);

        fuzzyMode = addToLeftToolbar(
                new SettingToggleButton<FuzzyMode>(Settings.FUZZY_MODE, FuzzyMode.IGNORE_ALL,
                        this::toggleFuzzyMode));
        addToLeftToolbar(new ActionButton(ActionItems.WRENCH,
                button -> send(PortableWorkbenchAction.PARTITION, 0)));
        addToLeftToolbar(new ActionButton(ActionItems.CLOSE,
                button -> send(PortableWorkbenchAction.CLEAR, 0)));
        copyMode = addToLeftToolbar(new ToggleButton(Icon.COPY_MODE_ON, Icon.COPY_MODE_OFF,
                GuiText.CopyMode.text(), GuiText.CopyModeDesc.text(),
                button -> send(PortableWorkbenchAction.COPY_MODE, 0)));
    }

    @Override
    protected List<ITextComponent> getCompatibleUpgrades() {
        ItemStack cell = container.getWorkbenchItem();
        return cell.isEmpty() ? Collections.emptyList() : super.getCompatibleUpgrades(cell.getItem());
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        copyMode.setState(container.getCopyMode() == CopyMode.CLEAR_ON_REMOVE);

        boolean hasFuzzy = false;
        IItemHandler upgrades = container.getCellUpgradeInventory();
        for (int i = 0; i < upgrades.getSlots(); i++) {
            ItemStack stack = upgrades.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof IUpgradeModule
                    && ((IUpgradeModule) stack.getItem()).getType(stack) == Upgrades.FUZZY) {
                hasFuzzy = true;
                break;
            }
        }
        fuzzyMode.set(container.getFuzzyMode());
        fuzzyMode.setVisibility(hasFuzzy);
    }

    private void toggleFuzzyMode(SettingToggleButton<FuzzyMode> button, boolean backwards) {
        FuzzyMode value = button.getNextValue(backwards);
        send(PortableWorkbenchAction.FUZZY, value.ordinal());
    }

    private void send(int action, int value) {
        ExpansionNetwork.sendToServer(new PortableWorkbenchAction(container.windowId, action, value));
    }
}
