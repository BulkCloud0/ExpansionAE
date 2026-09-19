package dev.bulkcloud.expansionae;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

public final class WirelessLinkToolItem extends Item {
    static final String TAG_FREQUENCY = "wirelessFrequency";
    static final String TAG_DIMENSION = "wirelessDimension";
    static final String TAG_X = "wirelessX";
    static final String TAG_Y = "wirelessY";
    static final String TAG_Z = "wirelessZ";

    public WirelessLinkToolItem(Properties properties) {
        super(properties.maxStackSize(1));
    }

    static void clearBinding(ItemStack stack) {
        CompoundNBT tag = stack.getTag();
        if (tag == null) {
            return;
        }
        tag.remove(TAG_FREQUENCY);
        tag.remove(TAG_DIMENSION);
        tag.remove(TAG_X);
        tag.remove(TAG_Y);
        tag.remove(TAG_Z);
        if (tag.isEmpty()) {
            stack.setTag(null);
        }
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<ITextComponent> tooltip,
            ITooltipFlag flag) {
        CompoundNBT tag = stack.getTag();
        if (tag != null && tag.contains(TAG_FREQUENCY)) {
            tooltip.add(new TranslationTextComponent("tooltip.expansionae.wireless_tool.bound",
                    tag.getInt(TAG_X), tag.getInt(TAG_Y), tag.getInt(TAG_Z)));
        } else {
            tooltip.add(new TranslationTextComponent("tooltip.expansionae.wireless_tool.unbound"));
        }
    }
}
