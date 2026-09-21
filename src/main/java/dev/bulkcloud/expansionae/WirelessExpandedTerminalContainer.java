package dev.bulkcloud.expansionae;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.config.SecurityPermissions;
import appeng.container.implementations.ContainerTypeBuilder;
import appeng.core.AEConfig;
import appeng.helpers.WirelessTerminalGuiObject;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.ContainerType;

/**
 * Wireless counterpart of the Expanded Pattern Access Terminal. The host is
 * AE2's native WirelessTerminalGuiObject, so security, WAP range and network
 * identity stay compatible with AE2 8.4.
 */
public final class WirelessExpandedTerminalContainer extends ExpandedTerminalContainer {
    public static final ContainerType<WirelessExpandedTerminalContainer> TYPE = ContainerTypeBuilder
            .create(WirelessExpandedTerminalContainer::new, WirelessTerminalGuiObject.class)
            .requirePermission(SecurityPermissions.BUILD)
            .build("expansionae_wireless_pattern_terminal");

    private final WirelessTerminalGuiObject wireless;
    private final int inventorySlot;
    private int powerTicks;

    public WirelessExpandedTerminalContainer(int id, PlayerInventory player,
            WirelessTerminalGuiObject wireless) {
        super(TYPE, id, player, wireless);
        this.wireless = wireless;
        this.inventorySlot = wireless.getInventorySlot();
        lockPlayerInventorySlot(inventorySlot);
    }

    @Override
    public void detectAndSendChanges() {
        if (isServer()) {
            if (!ensureGuiItemIsInSlot(wireless, inventorySlot) || !wireless.rangeCheck()) {
                setValidContainer(false);
                return;
            }

            powerTicks++;
            if (powerTicks >= 10) {
                double drain = AEConfig.instance().wireless_getDrainRate(wireless.getRange()) * powerTicks;
                double extracted = wireless.extractAEPower(drain, Actionable.MODULATE, PowerMultiplier.CONFIG);
                powerTicks = 0;
                if (extracted + 0.001 < drain) {
                    setValidContainer(false);
                    return;
                }
            }
        }

        super.detectAndSendChanges();
    }
}
