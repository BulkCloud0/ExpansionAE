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
 * Wireless Expanded Crafting Terminal backed by AE2 8.4's native wireless
 * object for security, network access, range validation and battery power.
 */
public final class WirelessExpandedCraftingTerminalContainer extends ExpandedCraftingTerminalContainer {
    public static final ContainerType<WirelessExpandedCraftingTerminalContainer> TYPE = ContainerTypeBuilder
            .create(WirelessExpandedCraftingTerminalContainer::new, WirelessTerminalGuiObject.class)
            .requirePermission(SecurityPermissions.CRAFT)
            .build("expansionae_wireless_crafting_terminal");

    private final WirelessTerminalGuiObject wireless;
    private final int inventorySlot;
    private int powerTicks;

    public WirelessExpandedCraftingTerminalContainer(int id, PlayerInventory player,
            WirelessTerminalGuiObject wireless) {
        super(TYPE, id, player, wireless, new WirelessCraftingInventoryHost(wireless.getItemStack()));
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
