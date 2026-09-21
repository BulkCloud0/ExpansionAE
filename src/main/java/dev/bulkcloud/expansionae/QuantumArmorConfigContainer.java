package dev.bulkcloud.expansionae;

import appeng.container.AEBaseContainer;
import appeng.container.guisync.GuiSync;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.extensions.IForgeContainerType;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 1.16.5 armor configuration menu. It mirrors the important AdvancedAE armor
 * workflow: select a worn piece, install a compatible card, toggle it and
 * uninstall it back into the player inventory.
 */
public final class QuantumArmorConfigContainer extends AEBaseContainer {
    private static final EquipmentSlotType[] ARMOR_SLOTS = {
            EquipmentSlotType.HEAD, EquipmentSlotType.CHEST,
            EquipmentSlotType.LEGS, EquipmentSlotType.FEET
    };

    public static final ContainerType<QuantumArmorConfigContainer> TYPE =
            IForgeContainerType.create((windowId, inventory, data) ->
                    new QuantumArmorConfigContainer(windowId, inventory, data.readVarInt()));

    static {
        TYPE.setRegistryName(new ResourceLocation(ExpansionAE.ID, "quantum_armor_config"));
    }

    @GuiSync(20) public int selectedArmor;
    @GuiSync(21) public int installedMask;
    @GuiSync(22) public int enabledMask;

    public QuantumArmorConfigContainer(int id, PlayerInventory player, int selectedArmor) {
        super(TYPE, id, player, null);
        this.selectedArmor = clamp(selectedArmor, 0, ARMOR_SLOTS.length - 1);

        registerClientAction("selectArmor", Integer.class,
                value -> this.selectedArmor = clamp(value == null ? 0 : value.intValue(), 0, ARMOR_SLOTS.length - 1));
        registerClientAction("installOrToggle", Integer.class, this::installOrToggleServer);
        registerClientAction("uninstall", Integer.class, this::uninstallServer);
        registerClientAction("captureAutoStock", this::captureAutoStockServer);
    }

    public EquipmentSlotType getSelectedEquipmentSlot() {
        return ARMOR_SLOTS[clamp(selectedArmor, 0, ARMOR_SLOTS.length - 1)];
    }

    public void selectArmor(int index) {
        int normalized = clamp(index, 0, ARMOR_SLOTS.length - 1);
        selectedArmor = normalized;
        if (isRemote()) sendClientAction("selectArmor", Integer.valueOf(normalized));
    }

    public boolean isInstalled(QuantumUpgradeType type) {
        return (installedMask & (1 << type.ordinal())) != 0;
    }

    public boolean isEnabled(QuantumUpgradeType type) {
        return (enabledMask & (1 << type.ordinal())) != 0;
    }

    public void installOrToggle(QuantumUpgradeType type) {
        if (type == null) return;
        if (isRemote()) sendClientAction("installOrToggle", Integer.valueOf(type.ordinal()));
        else installOrToggleServer(Integer.valueOf(type.ordinal()));
    }

    public void uninstall(QuantumUpgradeType type) {
        if (type == null) return;
        if (isRemote()) sendClientAction("uninstall", Integer.valueOf(type.ordinal()));
        else uninstallServer(Integer.valueOf(type.ordinal()));
    }

    private void installOrToggleServer(Integer ordinal) {
        QuantumUpgradeType type = byOrdinal(ordinal);
        if (type == null) return;

        ItemStack stack = getSelectedArmorStack();
        if (!(stack.getItem() instanceof QuantumArmorItem)) return;
        QuantumArmorItem armor = (QuantumArmorItem) stack.getItem();
        if (!armor.canInstall(type)) return;

        if (armor.hasUpgrade(stack, type)) {
            armor.toggleUpgrade(stack, type);
        } else {
            int cardSlot = findCard(type);
            if (cardSlot < 0) return;
            ItemStack card = getPlayerInventory().getStackInSlot(cardSlot);
            if (!armor.installUpgrade(stack, type)) return;
            if (!getPlayerInventory().player.abilities.isCreativeMode) card.shrink(1);
        }
        getPlayerInventory().markDirty();
        refreshMasks();
    }

    public void captureAutoStock() {
        if (isRemote()) sendClientAction("captureAutoStock");
        else captureAutoStockServer();
    }

    private void captureAutoStockServer() {
        ItemStack stack = getSelectedArmorStack();
        if (!(stack.getItem() instanceof QuantumArmorItem)) return;
        QuantumArmorItem armor = (QuantumArmorItem) stack.getItem();
        if (!armor.hasUpgrade(stack, QuantumUpgradeType.AUTO_STOCK)) return;
        armor.captureAutoStockTargets(stack, getPlayerInventory());
        getPlayerInventory().markDirty();
    }

    private void uninstallServer(Integer ordinal) {
        QuantumUpgradeType type = byOrdinal(ordinal);
        if (type == null) return;

        ItemStack stack = getSelectedArmorStack();
        if (!(stack.getItem() instanceof QuantumArmorItem)) return;
        QuantumArmorItem armor = (QuantumArmorItem) stack.getItem();
        if (!armor.removeUpgrade(stack, type)) return;

        Item cardItem = findCardItem(type);
        if (cardItem != null) {
            ItemStack card = new ItemStack(cardItem);
            if (!getPlayerInventory().addItemStackToInventory(card)) {
                getPlayerInventory().player.dropItem(card, false);
            }
        }
        getPlayerInventory().markDirty();
        refreshMasks();
    }

    private int findCard(QuantumUpgradeType type) {
        for (int i = 0; i < getPlayerInventory().mainInventory.size(); i++) {
            ItemStack stack = getPlayerInventory().getStackInSlot(i);
            if (stack.getItem() instanceof QuantumUpgradeItem
                    && ((QuantumUpgradeItem) stack.getItem()).getUpgradeType() == type) {
                return i;
            }
        }
        return -1;
    }

    private static Item findCardItem(QuantumUpgradeType type) {
        for (Item item : ForgeRegistries.ITEMS.getValues()) {
            if (item instanceof QuantumUpgradeItem
                    && ((QuantumUpgradeItem) item).getUpgradeType() == type) {
                return item;
            }
        }
        return null;
    }

    private ItemStack getSelectedArmorStack() {
        return getPlayerInventory().player.getItemStackFromSlot(getSelectedEquipmentSlot());
    }

    @Override
    public void detectAndSendChanges() {
        if (isServer()) refreshMasks();
        super.detectAndSendChanges();
    }

    private void refreshMasks() {
        installedMask = 0;
        enabledMask = 0;
        ItemStack stack = getSelectedArmorStack();
        if (!(stack.getItem() instanceof QuantumArmorItem)) return;
        QuantumArmorItem armor = (QuantumArmorItem) stack.getItem();
        for (QuantumUpgradeType type : QuantumUpgradeType.values()) {
            if (armor.hasUpgrade(stack, type)) installedMask |= 1 << type.ordinal();
            if (armor.isUpgradeEnabled(stack, type)) enabledMask |= 1 << type.ordinal();
        }
    }

    private static QuantumUpgradeType byOrdinal(Integer ordinal) {
        if (ordinal == null || ordinal.intValue() < 0
                || ordinal.intValue() >= QuantumUpgradeType.values().length) return null;
        return QuantumUpgradeType.values()[ordinal.intValue()];
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
