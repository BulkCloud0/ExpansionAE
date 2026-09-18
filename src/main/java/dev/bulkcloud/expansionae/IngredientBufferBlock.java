package dev.bulkcloud.expansionae;
import javax.annotation.Nullable;
import appeng.block.AEBaseTileBlock;
import appeng.container.ContainerLocator;
import appeng.container.ContainerOpener;
import appeng.util.InteractionUtil;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.World;
public final class IngredientBufferBlock extends AEBaseTileBlock<IngredientBufferTile> {
    public IngredientBufferBlock() { super(defaultProps(Material.GLASS).hardnessAndResistance(1.5F)); }
    @Override public ActionResultType onActivated(World world, BlockPos pos, PlayerEntity player, Hand hand,
            @Nullable ItemStack heldItem, BlockRayTraceResult hit) {
        if (InteractionUtil.isInAlternateUseMode(player)) return ActionResultType.PASS;
        IngredientBufferTile tile=getTileEntity(world,pos);
        if(tile!=null){
            if(!world.isRemote()) ContainerOpener.openContainer(IngredientBufferContainer.TYPE,player,ContainerLocator.forTileEntity(tile));
            return ActionResultType.func_233537_a_(world.isRemote());
        }
        return ActionResultType.PASS;
    }
}
