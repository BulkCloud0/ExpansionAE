package dev.bulkcloud.expansionae.client;

import appeng.api.config.RedstoneMode;
import appeng.api.config.Settings;
import appeng.api.config.Upgrades;
import appeng.client.gui.implementations.UpgradeableScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.AETextField;
import appeng.client.gui.widgets.ServerSettingToggleButton;
import appeng.client.gui.widgets.SettingToggleButton;
import dev.bulkcloud.expansionae.ExpansionNetwork;
import dev.bulkcloud.expansionae.ModExportBusContainer;
import dev.bulkcloud.expansionae.TextFilterUpdate;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.text.ITextComponent;

public final class ModExportBusScreen extends UpgradeableScreen<ModExportBusContainer> {
    private final SettingToggleButton<RedstoneMode> redstone;
    private AETextField filter;

    public ModExportBusScreen(ModExportBusContainer container, PlayerInventory player,
            ITextComponent title, ScreenStyle style) {
        super(container, player, title, style);
        redstone = new ServerSettingToggleButton<>(Settings.REDSTONE_CONTROLLED, RedstoneMode.IGNORE);
        addToLeftToolbar(redstone);
    }

    @Override
    protected void init() {
        super.init();
        filter = new AETextField(font, guiLeft + 24, guiTop + 35, 138, 14);
        filter.setMaxStringLength(128);
        filter.setText(container.filter == null ? "" : container.filter);
        filter.setResponder(value -> ExpansionNetwork.sendToServer(
                new TextFilterUpdate(container.windowId, 0, value)));
        addListener(filter);
        setListenerDefault(filter);
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (filter != null && button == 1 && filter.isMouseOver(x, y)) {
            filter.setText("");
            return true;
        }
        return super.mouseClicked(x, y, button);
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        redstone.set(container.getRedStoneMode());
        redstone.setVisibility(container.hasUpgrade(Upgrades.REDSTONE));
        if (filter != null && !filter.isFocused() && !filter.getText().equals(container.filter)) {
            filter.setText(container.filter == null ? "" : container.filter);
        }
    }
}
