package com.bulkcloud.expansionae.feature.extendedprovider;

import java.util.EnumSet;

import net.minecraft.block.BlockState;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraftforge.items.IItemHandler;

import com.bulkcloud.expansionae.core.registry.ExpansionAETileEntities;

import appeng.api.config.SecurityPermissions;
import appeng.api.implementations.tiles.ICraftingMachine;
import appeng.api.networking.GridFlags;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.crafting.ICraftingProviderHelper;
import appeng.api.networking.events.MENetworkCraftingPatternChange;
import appeng.core.Api;
import appeng.me.GridAccessException;
import appeng.tile.grid.AENetworkTileEntity;
import appeng.tile.inventory.AppEngInternalInventory;
import appeng.util.InventoryAdaptor;
import appeng.util.inv.IAEAppEngInventory;
import appeng.util.inv.InvOperation;

public final class PatternProvider36TileEntity
        extends AENetworkTileEntity
        implements ICraftingProvider, IAEAppEngInventory {
    public static final int PATTERN_SLOTS = 36;

    private final AppEngInternalInventory patterns =
            new AppEngInternalInventory(this, PATTERN_SLOTS, 1);

    public PatternProvider36TileEntity() {
        super(ExpansionAETileEntities.PATTERN_PROVIDER_36.get());
        this.getProxy().setFlags(GridFlags.REQUIRE_CHANNEL);
        this.getProxy().setIdlePowerUsage(1.0);
        this.getProxy().setValidSides(EnumSet.allOf(Direction.class));
    }

    public IItemHandler getPatterns() {
        return this.patterns;
    }

    @Override
    public void read(BlockState state, CompoundNBT data) {
        super.read(state, data);
        this.patterns.readFromNBT(data, "patterns");
    }

    @Override
    public CompoundNBT write(CompoundNBT data) {
        super.write(data);
        this.patterns.writeToNBT(data, "patterns");
        return data;
    }

    @Override
    public void onChangeInventory(
            IItemHandler inventory,
            int slot,
            InvOperation operation,
            ItemStack removedStack,
            ItemStack newStack) {
        if (inventory != this.patterns || this.isRemote()) {
            return;
        }

        try {
            this.getProxy().getGrid().postEvent(
                    new MENetworkCraftingPatternChange(
                            this,
                            this.getProxy().getNode()));
        } catch (GridAccessException ignored) {
        }
    }

    @Override
    public boolean isRemote() {
        return this.world == null || this.world.isRemote();
    }

    @Override
    public void provideCrafting(ICraftingProviderHelper helper) {
        if (this.world == null || !this.getProxy().isActive()) {
            return;
        }

        for (int slot = 0; slot < PATTERN_SLOTS; slot++) {
            ItemStack encoded = this.patterns.getStackInSlot(slot);
            if (encoded.isEmpty()) {
                continue;
            }

            ICraftingPatternDetails details =
                    Api.instance().crafting().decodePattern(encoded, this.world);
            if (details != null) {
                helper.addCraftingOption(this, details);
            }
        }
    }

    @Override
    public boolean pushPattern(
            ICraftingPatternDetails patternDetails,
            CraftingInventory table) {
        if (this.world == null
                || !this.getProxy().isActive()
                || !this.containsPattern(patternDetails)) {
            return false;
        }

        for (Direction direction : Direction.values()) {
            TileEntity target = this.world.getTileEntity(this.pos.offset(direction));
            if (target == null) {
                continue;
            }

            if (target instanceof ICraftingMachine) {
                ICraftingMachine machine = (ICraftingMachine) target;
                if (machine.acceptsPlans()
                        && machine.pushPattern(
                                patternDetails,
                                table,
                                direction.getOpposite())) {
                    return true;
                }
                continue;
            }

            InventoryAdaptor adaptor =
                    InventoryAdaptor.getAdaptor(target, direction.getOpposite());
            if (adaptor == null || !acceptsAll(adaptor, table)) {
                continue;
            }

            for (int slot = 0; slot < table.getSizeInventory(); slot++) {
                ItemStack stack = table.getStackInSlot(slot);
                if (!stack.isEmpty()) {
                    ItemStack remainder = adaptor.addItems(stack.copy());
                    if (!remainder.isEmpty()) {
                        throw new IllegalStateException(
                                "Pattern Provider destination changed after successful simulation");
                    }
                }
            }
            return true;
        }

        return false;
    }

    @Override
    public boolean isBusy() {
        return !this.getProxy().isActive();
    }

    private boolean containsPattern(ICraftingPatternDetails details) {
        ItemStack encoded = details.getPattern();
        for (int slot = 0; slot < PATTERN_SLOTS; slot++) {
            ItemStack candidate = this.patterns.getStackInSlot(slot);
            if (!candidate.isEmpty()
                    && ItemStack.areItemStacksEqual(candidate, encoded)) {
                return true;
            }
        }
        return false;
    }

    private static boolean acceptsAll(
            InventoryAdaptor adaptor,
            CraftingInventory table) {
        for (int slot = 0; slot < table.getSizeInventory(); slot++) {
            ItemStack stack = table.getStackInSlot(slot);
            if (!stack.isEmpty()
                    && !adaptor.simulateAdd(stack.copy()).isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
