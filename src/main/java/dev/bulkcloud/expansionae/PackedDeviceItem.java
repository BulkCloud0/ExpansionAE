package dev.bulkcloud.expansionae;

import java.util.List;

import javax.annotation.Nullable;

import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.parts.IPartItem;
import appeng.api.util.AEPartLocation;
import appeng.core.Api;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.registries.ForgeRegistries;

/** Restores a device captured with {@link PackingTapeItem}. */
public final class PackedDeviceItem extends Item {
    public PackedDeviceItem(Properties properties) {
        super(properties);
    }

    @Override
    public ActionResultType onItemUse(ItemUseContext context) {
        ItemStack packageStack = context.getItem();
        CompoundNBT packageTag = packageStack.getTag();
        if (!isValid(packageTag)) {
            return ActionResultType.FAIL;
        }

        World world = context.getWorld();
        if (world.isRemote) {
            return ActionResultType.SUCCESS;
        }

        if (packageTag.getBoolean("part")) {
            return placePart(context, packageTag);
        }
        return placeBlock(context, packageTag);
    }

    private ActionResultType placePart(ItemUseContext context, CompoundNBT packageTag) {
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(packageTag.getString("id")));
        if (!(item instanceof IPartItem)) {
            return ActionResultType.FAIL;
        }

        World world = context.getWorld();
        BlockPos clickedPos = context.getPos();
        Direction face = context.getFace();
        TileEntity tile = world.getTileEntity(clickedPos);
        IPartHost host = tile instanceof IPartHost ? (IPartHost) tile : null;
        AEPartLocation side;
        boolean createdHost = false;

        if (host != null) {
            side = AEPartLocation.fromFacing(face);
        } else {
            BlockPos hostPos = clickedPos.offset(face);
            BlockState atTarget = world.getBlockState(hostPos);
            if (!atTarget.isAir(world, hostPos) && !atTarget.getMaterial().isReplaceable()) {
                return ActionResultType.PASS;
            }

            Block multipart = Api.instance().definitions().blocks().multiPart().block();
            if (!world.setBlockState(hostPos, multipart.getDefaultState(), 3)) {
                return ActionResultType.FAIL;
            }

            tile = world.getTileEntity(hostPos);
            if (!(tile instanceof IPartHost)) {
                world.removeBlock(hostPos, false);
                return ActionResultType.FAIL;
            }
            host = (IPartHost) tile;
            side = AEPartLocation.fromFacing(face.getOpposite());
            createdHost = true;
        }

        ItemStack partStack = new ItemStack(item);
        if (!host.canAddPart(partStack, side)) {
            if (createdHost) {
                world.removeBlock(host.getTile().getPos(), false);
            }
            return ActionResultType.PASS;
        }

        AEPartLocation placedSide = host.addPart(partStack, side, context.getPlayer(), context.getHand());
        if (placedSide == null) {
            if (createdHost) {
                world.removeBlock(host.getTile().getPos(), false);
            }
            return ActionResultType.FAIL;
        }

        IPart part = host.getPart(placedSide);
        if (part == null) {
            host.removePart(placedSide, false);
            return ActionResultType.FAIL;
        }

        part.readFromNBT(packageTag.getCompound("ctx"));
        host.markForSave();
        host.markForUpdate();
        consume(context);
        return ActionResultType.SUCCESS;
    }

    private ActionResultType placeBlock(ItemUseContext context, CompoundNBT packageTag) {
        ResourceLocation blockId = new ResourceLocation(packageTag.getString("block_id"));
        Block block = ForgeRegistries.BLOCKS.getValue(blockId);
        if (block == null) {
            return ActionResultType.FAIL;
        }

        World world = context.getWorld();
        BlockPos clickedPos = context.getPos();
        BlockState clickedState = world.getBlockState(clickedPos);
        BlockPos placePos = clickedState.getMaterial().isReplaceable()
                ? clickedPos
                : clickedPos.offset(context.getFace());

        BlockState replaced = world.getBlockState(placePos);
        if (!replaced.isAir(world, placePos) && !replaced.getMaterial().isReplaceable()) {
            return ActionResultType.PASS;
        }

        BlockState storedState = NBTUtil.readBlockState(packageTag.getCompound("state"));
        if (storedState.getBlock() != block || !storedState.isValidPosition(world, placePos)) {
            return ActionResultType.FAIL;
        }

        if (!world.setBlockState(placePos, storedState, 3)) {
            return ActionResultType.FAIL;
        }

        TileEntity tile = world.getTileEntity(placePos);
        if (tile == null) {
            world.setBlockState(placePos, replaced, 3);
            return ActionResultType.FAIL;
        }

        CompoundNBT tileData = packageTag.getCompound("ctx").copy();
        tileData.putInt("x", placePos.getX());
        tileData.putInt("y", placePos.getY());
        tileData.putInt("z", placePos.getZ());
        tile.read(storedState, tileData);
        tile.markDirty();
        world.notifyBlockUpdate(placePos, replaced, storedState, 3);
        consume(context);
        return ActionResultType.SUCCESS;
    }

    private static void consume(ItemUseContext context) {
        PlayerEntity player = context.getPlayer();
        if (player == null || !player.abilities.isCreativeMode) {
            context.getItem().shrink(1);
        }
    }

    private static boolean isValid(@Nullable CompoundNBT tag) {
        if (tag == null || !tag.contains("part") || !tag.contains("ctx")) {
            return false;
        }
        return tag.getBoolean("part")
                ? tag.contains("id")
                : tag.contains("block_id") && tag.contains("state");
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<ITextComponent> tooltip,
            ITooltipFlag flag) {
        CompoundNBT tag = stack.getTag();
        if (!isValid(tag)) {
            tooltip.add(new TranslationTextComponent("tooltip.expansionae.packaged_device.invalid"));
            return;
        }

        ITextComponent name = null;
        if (tag.getBoolean("part")) {
            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(tag.getString("id")));
            if (item != null) {
                name = new ItemStack(item).getDisplayName();
            }
        } else {
            Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(tag.getString("block_id")));
            if (block != null && block.asItem() != net.minecraft.item.Items.AIR) {
                name = new ItemStack(block.asItem()).getDisplayName();
            }
        }

        if (name == null) {
            tooltip.add(new TranslationTextComponent("tooltip.expansionae.packaged_device.invalid"));
        } else {
            tooltip.add(new TranslationTextComponent("tooltip.expansionae.packaged_device", name));
        }
    }
}
