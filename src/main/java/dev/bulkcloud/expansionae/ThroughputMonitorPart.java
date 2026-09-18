package dev.bulkcloud.expansionae;

import java.io.IOException;
import java.util.Locale;

import appeng.api.networking.IGridNode;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.parts.IPartModel;
import appeng.api.storage.data.IAEItemStack;
import appeng.parts.reporting.AbstractMonitorPart;
import appeng.parts.reporting.StorageMonitorPart;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.Hand;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.TranslationTextComponent;

public final class ThroughputMonitorPart extends AbstractMonitorPart implements IGridTickable {
    private enum Mode {
        TICK(1, 10, "tick"),
        SECOND(20, 20, "second"),
        MINUTE(1200, 60, "minute");

        final int multiplier;
        final int sampleSeconds;
        final String key;
        Mode(int multiplier, int sampleSeconds, String key) {
            this.multiplier = multiplier;
            this.sampleSeconds = sampleSeconds;
            this.key = key;
        }
        Mode next() { return values()[(ordinal() + 1) % values().length]; }
    }

    private final ThroughputCache cache = new ThroughputCache();
    private Mode mode = Mode.SECOND;
    private double lastThroughput;
    private long sampleClock;

    public ThroughputMonitorPart(ItemStack stack) {
        super(stack);
        getProxy().setFlags();
        getProxy().setIdlePowerUsage(1.0 / 16.0);
    }

    @Override
    public boolean onPartActivate(PlayerEntity player, Hand hand, Vector3d pos) {
        if (!isRemote() && player.getHeldItem(hand).getItem() == ExpansionAE.THROUGHPUT_MONITOR_CONFIGURATOR.get()) {
            mode = mode.next();
            cache.clear();
            getHost().markForSave();
            getHost().markForUpdate();
            player.sendStatusMessage(new TranslationTextComponent("chat.expansionae.throughput.mode." + mode.key), true);
            return true;
        }
        if (!isRemote() && player.getHeldItem(hand).isEmpty() && getDisplayed() != null) {
            player.sendStatusMessage(new TranslationTextComponent("chat.expansionae.throughput.value",
                    format(lastThroughput)), true);
        }
        return super.onPartActivate(player, hand, pos);
    }

    private static String format(double value) {
        double abs = Math.abs(value);
        String amount = abs >= 100 ? Long.toString(Math.round(abs))
                : String.format(Locale.ROOT, "%.2f", abs);
        return (value > 0 ? "+" : value < 0 ? "-" : "") + amount;
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(20, 20, getDisplayed() == null, false);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        IAEItemStack displayed = getDisplayed();
        if (displayed == null || !getProxy().isActive()) {
            cache.clear();
            lastThroughput = 0;
            return TickRateModulation.SLEEP;
        }

        sampleClock += Math.max(1, ticksSinceLastCall);
        cache.push(displayed.getStackSize(), sampleClock);
        if (cache.size() > 1) {
            lastThroughput = cache.averagePerTick(sampleClock, mode.sampleSeconds) * mode.multiplier;
        }
        getHost().markForUpdate();
        return TickRateModulation.SAME;
    }

    @Override
    public void readFromNBT(CompoundNBT data) {
        super.readFromNBT(data);
        int ordinal = data.getInt("throughputMode");
        if (ordinal >= 0 && ordinal < Mode.values().length) mode = Mode.values()[ordinal];
        lastThroughput = data.getDouble("throughputValue");
    }

    @Override
    public void writeToNBT(CompoundNBT data) {
        super.writeToNBT(data);
        data.putInt("throughputMode", mode.ordinal());
        data.putDouble("throughputValue", lastThroughput);
    }

    @Override
    public void writeToStream(PacketBuffer data) throws IOException {
        super.writeToStream(data);
        data.writeInt(mode.ordinal());
        data.writeDouble(lastThroughput);
    }

    @Override
    public boolean readFromStream(PacketBuffer data) throws IOException {
        boolean redraw = super.readFromStream(data);
        int ordinal = data.readInt();
        Mode newMode = ordinal >= 0 && ordinal < Mode.values().length ? Mode.values()[ordinal] : Mode.SECOND;
        double newThroughput = data.readDouble();
        redraw |= newMode != mode || Double.compare(newThroughput, lastThroughput) != 0;
        mode = newMode;
        lastThroughput = newThroughput;
        return redraw;
    }

    @Override
    public IPartModel getStaticModels() {
        return selectModel(StorageMonitorPart.MODELS_OFF, StorageMonitorPart.MODELS_ON,
                StorageMonitorPart.MODELS_HAS_CHANNEL, StorageMonitorPart.MODELS_LOCKED_OFF,
                StorageMonitorPart.MODELS_LOCKED_ON, StorageMonitorPart.MODELS_LOCKED_HAS_CHANNEL);
    }
}
