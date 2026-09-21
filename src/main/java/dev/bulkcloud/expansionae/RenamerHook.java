package dev.bulkcloud.expansionae;

import appeng.container.ContainerLocator;
import appeng.container.ContainerOpener;
import appeng.helpers.ICustomNameObject;
import appeng.items.tools.quartz.QuartzCuttingKnifeItem;
import appeng.tile.AEBaseTileEntity;
import appeng.tile.networking.CableBusTileEntity;
import appeng.util.InteractionUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ExpansionAE.ID)
public final class RenamerHook {
    private RenamerHook() {}

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        PlayerEntity player = event.getPlayer();
        World world = event.getWorld();
        if (player == null || player.isSpectator() || event.getHand() != Hand.MAIN_HAND) return;
        if (InteractionUtil.isInAlternateUseMode(player)) return;
        if (!(player.getHeldItem(event.getHand()).getItem() instanceof QuartzCuttingKnifeItem)) return;

        BlockRayTraceResult hit = event.getHitVec();
        BlockPos pos = hit.getPos();
        net.minecraft.tileentity.TileEntity tile = world.getTileEntity(pos);
        ICustomNameObject target = null;
        ContainerLocator locator = null;

        if (tile instanceof CableBusTileEntity) {
            Vector3d worldHit = hit.getHitVec();
            Vector3d localHit = worldHit.subtract(pos.getX(), pos.getY(), pos.getZ());
            appeng.api.parts.SelectedPart selected = ((CableBusTileEntity) tile).selectPart(localHit);
            if (selected.part instanceof ICustomNameObject) {
                target = (ICustomNameObject) selected.part;
                locator = ContainerLocator.forPart(selected.part);
            }
        } else if (tile instanceof AEBaseTileEntity && tile instanceof ICustomNameObject) {
            target = (ICustomNameObject) tile;
            locator = ContainerLocator.forTileEntitySide(tile, hit.getFace());
        }

        if (target == null || locator == null) return;

        if (!world.isRemote) {
            ContainerOpener.openContainer(RenamerContainer.TYPE, player, locator);
        }

        event.setCanceled(true);
        event.setCancellationResult(ActionResultType.func_233537_a_(world.isRemote));
    }
}
