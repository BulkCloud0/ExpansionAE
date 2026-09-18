package dev.bulkcloud.expansionae;

import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import appeng.api.implementations.tiles.ISegmentedInventory;
import appeng.tile.AEBaseTileEntity;
import appeng.tile.inventory.AppEngInternalInventory;
import appeng.util.inv.IAEAppEngInventory;
import appeng.util.inv.InvOperation;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

/** 1.16.5 adaptation of ExtendedAE's generic Ingredient Buffer. */
public final class IngredientBufferTile extends AEBaseTileEntity
        implements IAEAppEngInventory, ISegmentedInventory {
    public static final int ITEM_SLOTS = 36;
    public static final int FLUID_CAPACITY = 64000;
    private final AppEngInternalInventory items = new AppEngInternalInventory(this, ITEM_SLOTS);
    private final FluidTank fluid = new FluidTank(FLUID_CAPACITY) {
        @Override protected void onContentsChanged() {
            IngredientBufferTile.this.saveChanges();
            IngredientBufferTile.this.markForUpdate();
        }
    };
    private LazyOptional<IItemHandler> itemCapability = LazyOptional.of(() -> items);
    private LazyOptional<IFluidHandler> fluidCapability = LazyOptional.of(() -> fluid);

    public IngredientBufferTile(TileEntityType<?> type) { super(type); }
    public IItemHandler getItemInventory() { return items; }
    public IFluidHandler getFluidInventory() { return fluid; }
    public FluidStack getFluid() { return fluid.getFluid(); }
    @Override public boolean canBeRotated() { return false; }

    @Override public CompoundNBT write(CompoundNBT data) {
        super.write(data);
        items.writeToNBT(data, "items");
        data.put("fluid", fluid.writeToNBT(new CompoundNBT()));
        return data;
    }

    @Override public void read(BlockState state, CompoundNBT data) {
        super.read(state, data);
        items.readFromNBT(data, "items");
        if (data.contains("fluid")) fluid.readFromNBT(data.getCompound("fluid"));
    }

    @Override public void getDrops(World world, BlockPos pos, List<ItemStack> drops) {
        for (ItemStack stack : items) if (!stack.isEmpty()) drops.add(stack.copy());
    }

    @Override public IItemHandler getInventoryByName(String name) {
        return "buffer".equals(name) || "items".equals(name) ? items : null;
    }

    @Override public void onChangeInventory(IItemHandler inv, int slot, InvOperation operation,
            ItemStack removedStack, ItemStack newStack) {
        saveChanges();
        markForUpdate();
    }

    @Nonnull @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, @Nullable Direction side) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return itemCapability.cast();
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) return fluidCapability.cast();
        return super.getCapability(capability, side);
    }

    @Override public void invalidateCaps() {
        super.invalidateCaps();
        itemCapability.invalidate();
        fluidCapability.invalidate();
    }
}
