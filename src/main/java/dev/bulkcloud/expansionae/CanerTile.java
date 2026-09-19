package dev.bulkcloud.expansionae;

import java.util.EnumSet;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.networking.IGridNode;
import appeng.api.networking.energy.IEnergyGrid;
import appeng.api.networking.energy.IEnergySource;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.util.AECableType;
import appeng.api.util.AEPartLocation;
import appeng.me.GridAccessException;
import appeng.tile.grid.AENetworkPowerTileEntity;
import appeng.tile.inventory.AppEngInternalInventory;
import appeng.util.inv.InvOperation;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.IItemHandler;

/**
 * 1.16.5 fluid-capability implementation of ExtendedAE's ME Canner.
 */
public final class CanerTile extends AENetworkPowerTileEntity implements IGridTickable {
    public static final int POWER_MAXIMUM_AMOUNT = 3200;
    public static final int POWER_USAGE = 80;
    public static final int TANK_CAPACITY = 64000;

    private final AppEngInternalInventory container = new AppEngInternalInventory(this, 1);
    private final FluidTank tank;
    private final LazyOptional<IFluidHandler> fluidCapability;
    private CanerMode mode = CanerMode.FILL;

    public CanerTile(TileEntityType<?> type) {
        super(type);
        container.setMaxStackSize(0, 1);
        setInternalMaxPower(POWER_MAXIMUM_AMOUNT);
        setPowerSides(EnumSet.of(Direction.UP, Direction.DOWN));
        getProxy().setValidSides(EnumSet.of(Direction.UP, Direction.DOWN));
        getProxy().setIdlePowerUsage(0.0);
        tank = new FluidTank(TANK_CAPACITY) {
            @Override protected void onContentsChanged() { changed(); }
        };
        fluidCapability = LazyOptional.of(() -> tank);
    }

    @Override
    public AECableType getCableConnectionType(AEPartLocation dir) {
        return AECableType.COVERED;
    }

    @Nonnull
    @Override
    public IItemHandler getInternalInventory() {
        return container;
    }

    public IFluidHandler getFluidHandler() { return tank; }
    public int getFluidAmount() { return tank.getFluidAmount(); }
    public CanerMode getMode() { return mode; }

    public void setMode(CanerMode mode) {
        if (mode != null && this.mode != mode) {
            this.mode = mode;
            saveChanges();
            markForUpdate();
            wake();
        }
    }

    @Override
    public void onChangeInventory(IItemHandler inv, int slot, InvOperation operation,
            ItemStack removed, ItemStack added) {
        changed();
    }

    private void changed() {
        saveChanges();
        markForUpdate();
        wake();
    }

    private void wake() {
        try {
            getProxy().getTick().wakeDevice(getProxy().getNode());
        } catch (GridAccessException ignored) {
        }
    }

    @Nullable
    private IFluidHandlerItem getItemHandler(ItemStack stack) {
        if (stack.isEmpty()) return null;
        return stack.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY).orElse(null);
    }

    private boolean hasJob() {
        ItemStack stack = container.getStackInSlot(0);
        if (stack.isEmpty()) return false;
        IFluidHandlerItem handler = getItemHandler(stack.copy());
        if (handler == null) return false;
        if (mode == CanerMode.FILL) {
            FluidStack stored = tank.getFluid();
            return !stored.isEmpty() && handler.fill(stored.copy(), IFluidHandler.FluidAction.SIMULATE) > 0;
        }
        return !handler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE).isEmpty();
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(1, 20, !hasJob(), true);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        boolean changed = mode == CanerMode.FILL ? fillContainer() : emptyContainer();
        return changed ? TickRateModulation.URGENT
                : (hasJob() ? TickRateModulation.IDLE : TickRateModulation.SLEEP);
    }

    private boolean fillContainer() {
        FluidStack stored = tank.getFluid();
        ItemStack stack = container.getStackInSlot(0);
        if (stored.isEmpty() || stack.isEmpty()) return false;

        ItemStack working = stack.copy();
        IFluidHandlerItem handler = getItemHandler(working);
        if (handler == null) return false;
        int amount = handler.fill(stored.copy(), IFluidHandler.FluidAction.SIMULATE);
        if (amount <= 0 || !consumePower()) return false;

        FluidStack drained = tank.drain(amount, IFluidHandler.FluidAction.EXECUTE);
        int accepted = handler.fill(drained, IFluidHandler.FluidAction.EXECUTE);
        if (accepted < drained.getAmount()) {
            FluidStack refund = drained.copy();
            refund.setAmount(drained.getAmount() - accepted);
            tank.fill(refund, IFluidHandler.FluidAction.EXECUTE);
        }
        container.setStackInSlot(0, handler.getContainer().copy());
        return accepted > 0;
    }

    private boolean emptyContainer() {
        ItemStack stack = container.getStackInSlot(0);
        if (stack.isEmpty()) return false;

        ItemStack working = stack.copy();
        IFluidHandlerItem handler = getItemHandler(working);
        if (handler == null) return false;
        FluidStack available = handler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
        if (available.isEmpty()) return false;

        int accepted = tank.fill(available, IFluidHandler.FluidAction.SIMULATE);
        if (accepted <= 0 || !consumePower()) return false;
        FluidStack request = available.copy();
        request.setAmount(accepted);
        FluidStack drained = handler.drain(request, IFluidHandler.FluidAction.EXECUTE);
        if (drained.isEmpty()) return false;

        tank.fill(drained, IFluidHandler.FluidAction.EXECUTE);
        container.setStackInSlot(0, handler.getContainer().copy());
        return true;
    }

    private boolean consumePower() {
        double available = extractAEPower(POWER_USAGE, Actionable.SIMULATE, PowerMultiplier.CONFIG);
        IEnergySource source = this;
        if (available + 0.001 < POWER_USAGE) {
            try {
                IEnergyGrid grid = getProxy().getEnergy();
                available = grid.extractAEPower(POWER_USAGE, Actionable.SIMULATE, PowerMultiplier.CONFIG);
                source = grid;
            } catch (GridAccessException ignored) {
                return false;
            }
        }
        if (available + 0.001 < POWER_USAGE) return false;
        source.extractAEPower(POWER_USAGE, Actionable.MODULATE, PowerMultiplier.CONFIG);
        return true;
    }

    @Override
    public CompoundNBT write(CompoundNBT data) {
        super.write(data);
        data.put("tank", tank.writeToNBT(new CompoundNBT()));
        data.putByte("canerMode", (byte) mode.ordinal());
        return data;
    }

    @Override
    public void read(BlockState state, CompoundNBT data) {
        super.read(state, data);
        if (data.contains("tank")) tank.readFromNBT(data.getCompound("tank"));
        int ordinal = data.getByte("canerMode");
        mode = ordinal >= 0 && ordinal < CanerMode.values().length
                ? CanerMode.values()[ordinal] : CanerMode.FILL;
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) return fluidCapability.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void remove() {
        super.remove();
        fluidCapability.invalidate();
    }
}
