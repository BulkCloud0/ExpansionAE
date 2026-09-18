package dev.bulkcloud.expansionae;

import java.util.Collections;

import appeng.api.parts.IPart;
import appeng.api.parts.PartItemStack;
import appeng.api.parts.SelectedPart;
import appeng.parts.misc.InterfacePart;
import appeng.tile.misc.InterfaceTileEntity;
import appeng.tile.networking.CableBusTileEntity;
import appeng.tile.storage.DriveTileEntity;
import appeng.util.Platform;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

/**
 * Backport of ExtendedAE's ME Packing Tape.
 *
 * <p>The 1.16.5 port intentionally uses the upstream default device set:
 * AE2/ExpansionAE interfaces and providers plus the native AE2 drive. Third
 * party block entities are not serialized.</p>
 */
public final class PackingTapeItem extends Item {
    public PackingTapeItem(Properties properties) {
        super(properties);
    }

    @Override
    public ActionResultType onItemUse(ItemUseContext context) {
        PlayerEntity player = context.getPlayer();
        if (player == null || !player.isSneaking()) {
            return ActionResultType.PASS;
        }

        World world = context.getWorld();
        BlockPos pos = context.getPos();
        TileEntity tile = world.getTileEntity(pos);
        if (tile == null) {
            return ActionResultType.PASS;
        }

        CompoundNBT packageTag;

        if (tile instanceof CableBusTileEntity) {
            CableBusTileEntity cable = (CableBusTileEntity) tile;
            Vector3d hit = context.getHitVec();
            SelectedPart selected = cable.selectPart(new Vector3d(
                    hit.x - pos.getX(),
                    hit.y - pos.getY(),
                    hit.z - pos.getZ()));

            if (!isPackablePart(selected.part)) {
                return ActionResultType.PASS;
            }

            ItemStack partStack = selected.part.getItemStack(PartItemStack.BREAK);
            ResourceLocation itemId = partStack.getItem().getRegistryName();
            if (itemId == null) {
                return ActionResultType.FAIL;
            }

            if (world.isRemote) {
                return ActionResultType.SUCCESS;
            }

            packageTag = new CompoundNBT();
            packageTag.putBoolean("part", true);
            packageTag.putString("id", itemId.toString());
            CompoundNBT partData = new CompoundNBT();
            selected.part.writeToNBT(partData);
            packageTag.put("ctx", partData);
            cable.removePart(selected.side, false);
        } else {
            if (!isPackableTile(tile)) {
                return ActionResultType.PASS;
            }

            BlockState state = world.getBlockState(pos);
            ResourceLocation blockId = state.getBlock().getRegistryName();
            if (blockId == null) {
                return ActionResultType.FAIL;
            }

            if (world.isRemote) {
                return ActionResultType.SUCCESS;
            }

            packageTag = new CompoundNBT();
            packageTag.putBoolean("part", false);
            packageTag.putString("block_id", blockId.toString());
            packageTag.put("state", NBTUtil.writeBlockState(state));
            packageTag.put("ctx", tile.write(new CompoundNBT()));
            world.removeBlock(pos, false);
        }

        ItemStack packed = new ItemStack(ExpansionAE.PACKED_DEVICE.get());
        packed.setTag(packageTag);
        Platform.spawnDrops(world, pos, Collections.singletonList(packed));
        context.getItem().damageItem(1, player, entity -> entity.sendBreakAnimation(context.getHand()));
        return ActionResultType.SUCCESS;
    }

    private static boolean isPackablePart(IPart part) {
        return part != null && (part.getClass() == InterfacePart.class || part instanceof ExpandedInterfacePart);
    }

    private static boolean isPackableTile(TileEntity tile) {
        return tile.getClass() == InterfaceTileEntity.class
                || tile.getClass() == DriveTileEntity.class
                || tile instanceof ExpandedInterfaceTile;
    }
}
