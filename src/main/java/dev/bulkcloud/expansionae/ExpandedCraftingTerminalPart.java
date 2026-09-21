package dev.bulkcloud.expansionae;

import java.util.Arrays;
import java.util.List;

import appeng.api.config.SecurityPermissions;
import appeng.api.parts.IPartModel;
import appeng.container.me.items.ItemTerminalContainer;
import appeng.items.parts.PartModels;
import appeng.parts.PartModel;
import appeng.parts.reporting.CraftingTerminalPart;
import appeng.tile.inventory.AppEngInternalInventory;
import appeng.util.Platform;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.items.IItemHandler;

/**
 * ExtendedAE crafting terminal adapted to the AE2 8.4 terminal stack.
 *
 * Minecraft 1.16.5 has the pre-template smithing table, so SMITHING uses two
 * inputs. Crafting, smithing, stonecutting and anvil inventories are persisted
 * independently and share the ME terminal view.
 */
public final class ExpandedCraftingTerminalPart extends CraftingTerminalPart implements ExpandedCraftingInventoryHost {
    @PartModels
    public static final ResourceLocation MODEL_OFF =
            new ResourceLocation(ExpansionAE.ID, "part/ex_crafting_terminal_off");
    @PartModels
    public static final ResourceLocation MODEL_ON =
            new ResourceLocation(ExpansionAE.ID, "part/ex_crafting_terminal_on");

    public static final List<ResourceLocation> MODELS = Arrays.asList(MODEL_OFF, MODEL_ON);
    public static final IPartModel MODELS_OFF =
            new PartModel(MODEL_BASE, MODEL_OFF, MODEL_STATUS_OFF);
    public static final IPartModel MODELS_ON =
            new PartModel(MODEL_BASE, MODEL_ON, MODEL_STATUS_ON);
    public static final IPartModel MODELS_HAS_CHANNEL =
            new PartModel(MODEL_BASE, MODEL_ON, MODEL_STATUS_HAS_CHANNEL);

    private final AppEngInternalInventory smithingGrid = new AppEngInternalInventory(this, 2);
    private final AppEngInternalInventory stonecuttingGrid = new AppEngInternalInventory(this, 1);
    private final AppEngInternalInventory anvilGrid = new AppEngInternalInventory(this, 2);
    private ExpandedCraftingMode mode = ExpandedCraftingMode.CRAFTING;

    public ExpandedCraftingTerminalPart(ItemStack stack) {
        super(stack);
    }

    public ExpandedCraftingMode getCraftingMode() {
        return mode;
    }

    public void setCraftingMode(ExpandedCraftingMode mode) {
        if (mode == null || this.mode == mode) return;
        this.mode = mode;
        getHost().markForSave();
    }

    public IItemHandler getModeInventory(ExpandedCraftingMode requestedMode) {
        switch (requestedMode) {
            case SMITHING:
                return smithingGrid;
            case STONECUTTING:
                return stonecuttingGrid;
            case ANVIL:
                return anvilGrid;
            case CRAFTING:
            default:
                return super.getInventoryByName("crafting");
        }
    }

    @Override
    public IItemHandler getInventoryByName(String name) {
        if ("smithing".equals(name)) return smithingGrid;
        if ("stonecutting".equals(name)) return stonecuttingGrid;
        if ("anvil".equals(name)) return anvilGrid;
        return super.getInventoryByName(name);
    }

    @Override
    public ContainerType<?> getContainerType(PlayerEntity player) {
        if (Platform.checkPermissions(player, this, SecurityPermissions.CRAFT, false)) {
            return ExpandedCraftingTerminalContainer.TYPE;
        }
        return ItemTerminalContainer.TYPE;
    }

    @Override
    public void getDrops(List<ItemStack> drops, boolean wrenched) {
        super.getDrops(drops, wrenched);
        addDrops(smithingGrid, drops);
        addDrops(stonecuttingGrid, drops);
        addDrops(anvilGrid, drops);
    }

    private static void addDrops(IItemHandler inventory, List<ItemStack> drops) {
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) drops.add(stack.copy());
        }
    }

    @Override
    public void readFromNBT(CompoundNBT data) {
        super.readFromNBT(data);
        smithingGrid.readFromNBT(data, "exSmithingGrid");
        stonecuttingGrid.readFromNBT(data, "exStonecuttingGrid");
        anvilGrid.readFromNBT(data, "exAnvilGrid");
        mode = ExpandedCraftingMode.byOrdinal(data.getInt("exCraftingMode"));
    }

    @Override
    public void writeToNBT(CompoundNBT data) {
        super.writeToNBT(data);
        smithingGrid.writeToNBT(data, "exSmithingGrid");
        stonecuttingGrid.writeToNBT(data, "exStonecuttingGrid");
        anvilGrid.writeToNBT(data, "exAnvilGrid");
        data.putInt("exCraftingMode", mode.ordinal());
    }

    @Override
    public IPartModel getStaticModels() {
        return selectModel(MODELS_OFF, MODELS_ON, MODELS_HAS_CHANNEL);
    }
}
