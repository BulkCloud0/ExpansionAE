package dev.bulkcloud.expansionae.client;

import appeng.client.gui.implementations.UpgradeableScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.ProgressBar;
import appeng.client.gui.widgets.ProgressBar.Direction;
import dev.bulkcloud.expansionae.CircuitCutterConfigUpdate;
import dev.bulkcloud.expansionae.CircuitCutterContainer;
import dev.bulkcloud.expansionae.ExpansionNetwork;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

public final class CircuitCutterScreen extends UpgradeableScreen<CircuitCutterContainer> {
    private final ProgressBar progress;
    private final Button autoExport;

    public CircuitCutterScreen(CircuitCutterContainer container, PlayerInventory player,
            ITextComponent title, ScreenStyle style) {
        super(container, player, title, style);
        progress = new ProgressBar(container, style.getImage("progressBar"), Direction.VERTICAL);
        widgets.add("progressBar", progress);
        autoExport = addToLeftToolbar(new Button(0, 0, 112, 20,
                new TranslationTextComponent("gui.expansionae.circuit_cutter.auto_export_off"),
                button -> {
                    boolean value = !container.isAutoExport();
                    container.autoExport = value;
                    ExpansionNetwork.sendToServer(new CircuitCutterConfigUpdate(container.getHostPos(), value));
                }));
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        autoExport.setMessage(new TranslationTextComponent(container.isAutoExport()
                ? "gui.expansionae.circuit_cutter.auto_export_on"
                : "gui.expansionae.circuit_cutter.auto_export_off"));
    }
}
