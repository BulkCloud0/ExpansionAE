package dev.bulkcloud.expansionae;

import java.util.Map;

import appeng.api.parts.IPart;
import appeng.api.parts.PartItemStack;
import appeng.api.parts.SelectedPart;
import appeng.api.util.AEPartLocation;
import appeng.parts.automation.ExportBusPart;
import appeng.parts.automation.ImportBusPart;
import appeng.parts.misc.InterfacePart;
import appeng.parts.reporting.InterfaceTerminalPart;
import appeng.tile.misc.InterfaceTileEntity;
import appeng.tile.networking.CableBusTileEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.state.Property;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

/**
 * In-place conversion tool adapted from ExtendedAE's upgrade items.
 *
 * <p>AE2 8.4 still represents both interfaces and pattern providers with the
 * same Interface block/part, so the selected upgrade determines which
 * ExpansionAE variant is created.</p>
 */
public final class UpgradeItem extends Item {
    public enum Target {
        INTERFACE,
        PATTERN_PROVIDER,
        IO_BUS,
        PATTERN_TERMINAL
    }

    private final Target target;

    public UpgradeItem(Properties properties, Target target) {
        super(properties);
        this.target = target;
    }

    @Override
    public ActionResultType onItemUse(ItemUseContext context) {
        World world = context.getWorld();
        BlockPos pos = context.getPos();
        TileEntity tile = world.getTileEntity(pos);
        if (tile == null) {
            return ActionResultType.PASS;
        }

        if (tile instanceof CableBusTileEntity) {
            CableBusTileEntity cable = (CableBusTileEntity) tile;
            Vector3d hit = context.getHitVec();
            Vector3d localHit = new Vector3d(
                    hit.x - pos.getX(),
                    hit.y - pos.getY(),
                    hit.z - pos.getZ());
            SelectedPart selected = cable.selectPart(localHit);
            Item targetItem = getPartTarget(selected.part);
            if (targetItem == null) {
                return ActionResultType.PASS;
            }
            if (world.isRemote) {
                return ActionResultType.SUCCESS;
            }
            if (!replacePart(cable, selected, targetItem, context)) {
                return ActionResultType.FAIL;
            }
            consume(context);
            return ActionResultType.SUCCESS;
        }

        Block targetBlock = getBlockTarget(tile);
        if (targetBlock == null) {
            return ActionResultType.PASS;
        }
        if (world.isRemote) {
            return ActionResultType.SUCCESS;
        }
        if (!replaceBlock(world, pos, tile, targetBlock)) {
            return ActionResultType.FAIL;
        }
        consume(context);
        return ActionResultType.SUCCESS;
    }

    private Item getPartTarget(IPart part) {
        if (part == null) {
            return null;
        }

        switch (target) {
            case INTERFACE:
                return part.getClass() == InterfacePart.class ? ExpansionAE.INTERFACE_PART.get() : null;
            case PATTERN_PROVIDER:
                return part.getClass() == InterfacePart.class ? ExpansionAE.PROVIDER_PART.get() : null;
            case IO_BUS:
                if (part.getClass() == ImportBusPart.class) {
                    return ExpansionAE.IMPORT_BUS.get();
                }
                if (part.getClass() == ExportBusPart.class) {
                    return ExpansionAE.EXPORT_BUS.get();
                }
                return null;
            case PATTERN_TERMINAL:
                return part.getClass() == InterfaceTerminalPart.class ? ExpansionAE.PATTERN_TERMINAL.get() : null;
            default:
                return null;
        }
    }

    private Block getBlockTarget(TileEntity tile) {
        if (tile.getClass() != InterfaceTileEntity.class) {
            return null;
        }

        switch (target) {
            case INTERFACE:
                return ExpansionAE.INTERFACE.get();
            case PATTERN_PROVIDER:
                return ExpansionAE.PROVIDER.get();
            default:
                return null;
        }
    }

    private boolean replacePart(CableBusTileEntity cable, SelectedPart selected, Item targetItem,
            ItemUseContext context) {
        IPart oldPart = selected.part;
        if (oldPart == null || selected.side == null) {
            return false;
        }

        CompoundNBT data = new CompoundNBT();
        oldPart.writeToNBT(data);
        ItemStack restoreStack = oldPart.getItemStack(PartItemStack.BREAK);
        AEPartLocation side = selected.side;

        cable.removePart(side, true);
        AEPartLocation placedSide = cable.addPart(new ItemStack(targetItem), side,
                context.getPlayer(), context.getHand());

        if (placedSide == null) {
            cable.addPart(restoreStack, side, context.getPlayer(), context.getHand());
            return false;
        }

        IPart replacement = cable.getPart(placedSide);
        if (replacement == null) {
            cable.removePart(placedSide, false);
            cable.addPart(restoreStack, side, context.getPlayer(), context.getHand());
            return false;
        }

        replacement.readFromNBT(data);
        cable.getCableBus().markForSave();
        cable.getCableBus().markForUpdate();
        return true;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private boolean replaceBlock(World world, BlockPos pos, TileEntity oldTile, Block targetBlock) {
        CompoundNBT data = oldTile.write(new CompoundNBT());
        BlockState oldState = world.getBlockState(pos);
        BlockState newState = targetBlock.getDefaultState();

        for (Map.Entry<Property<?>, Comparable<?>> entry : oldState.getValues().entrySet()) {
            Property property = entry.getKey();
            if (newState.hasProperty(property)) {
                try {
                    newState = newState.with(property, entry.getValue());
                } catch (IllegalArgumentException ignored) {
                    // A property with the same identity but incompatible value is simply not copied.
                }
            }
        }

        if (!world.setBlockState(pos, newState, 3)) {
            return false;
        }

        TileEntity replacement = world.getTileEntity(pos);
        if (replacement == null) {
            world.setBlockState(pos, oldState, 3);
            return false;
        }

        replacement.read(newState, data);
        replacement.markDirty();
        world.notifyBlockUpdate(pos, oldState, newState, 3);
        return true;
    }

    private static void consume(ItemUseContext context) {
        PlayerEntity player = context.getPlayer();
        if (player == null || !player.abilities.isCreativeMode) {
            context.getItem().shrink(1);
        }
    }
}
