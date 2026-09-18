package dev.bulkcloud.expansionae;

import java.io.IOException;
import java.util.Locale;

import com.mojang.blaze3d.matrix.MatrixStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.Hand;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import appeng.api.networking.IGridNode;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.parts.IPartModel;
import appeng.client.render.TesrRenderHelper;
import appeng.parts.reporting.AbstractMonitorPart;
import appeng.parts.reporting.StorageMonitorPart;
import appeng.util.ReadableNumberConverter;

/**
 * AdvancedAE-style throughput monitor adapted to AE2 8.4's item-only
 * storage-monitor API.
 */
public final class ThroughputMonitorPart extends AbstractMonitorPart implements IGridTickable {

    private enum WorkRoutine {
        TICK(1, 10),
        SECOND(20, 20),
        MINUTE(1200, 60),
        TEN_MINUTE(12000, 600);

        final int displayTicks;
        final int windowSeconds;

        WorkRoutine(int displayTicks, int windowSeconds) {
            this.displayTicks = displayTicks;
            this.windowSeconds = windowSeconds;
        }

        WorkRoutine next() {
            WorkRoutine[] values = values();
            return values[(ordinal() + 1) % values.length];
        }

        static WorkRoutine fromOrdinal(int ordinal) {
            WorkRoutine[] values = values();
            return ordinal >= 0 && ordinal < values.length ? values[ordinal] : SECOND;
        }
    }

    private final ThroughputCache cache = new ThroughputCache();
    private double lastReportedValue;
    private String lastHumanReadableValue = "-";
    private WorkRoutine workRoutine = WorkRoutine.SECOND;

    public ThroughputMonitorPart(ItemStack stack) {
        super(stack);
    }

    @Override
    public boolean onPartActivate(PlayerEntity player, Hand hand, Vector3d pos) {
        if (!isRemote() && player.getHeldItem(hand).getItem() == ExpansionAE.THROUGHPUT_MONITOR_CONFIGURATOR.get()) {
            this.workRoutine = this.workRoutine.next();
            this.cache.clear();
            this.lastReportedValue = 0.0;
            this.lastHumanReadableValue = "-";
            this.getHost().markForSave();
            this.getHost().markForUpdate();
            try {
                this.getProxy().getTick().alertDevice(this.getProxy().getNode());
            } catch (Exception ignored) {
                // The node may not be attached yet.
            }
            return true;
        }

        boolean result = super.onPartActivate(player, hand, pos);
        if (!isRemote()) {
            this.cache.clear();
            try {
                this.getProxy().getTick().wakeDevice(this.getProxy().getNode());
            } catch (Exception ignored) {
                // The node may not be attached yet.
            }
        }
        return result;
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(20, 100, !this.isActive() || this.getDisplayed() == null, true);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        if (!this.isActive() || this.getDisplayed() == null || this.getTile().getWorld() == null) {
            this.cache.clear();
            this.lastHumanReadableValue = "-";
            return TickRateModulation.SLEEP;
        }

        long tick = this.getTile().getWorld().getGameTime();
        long amount = this.getDisplayed().getStackSize();

        if (this.cache.size() == 0) {
            this.cache.push(amount, tick);
            this.lastHumanReadableValue = "-";
            this.getHost().markForUpdate();
            return TickRateModulation.SAME;
        }

        double perTick = this.cache.averagePerTick(this.workRoutine.windowSeconds, tick);
        this.lastReportedValue = perTick * this.workRoutine.displayTicks;
        this.lastHumanReadableValue = formatAmount(Math.abs(this.lastReportedValue));
        this.cache.push(amount, tick);
        this.getHost().markForUpdate();
        return TickRateModulation.SAME;
    }

    private static String formatAmount(double amount) {
        if (amount > 10.0 || amount == 0.0) {
            return ReadableNumberConverter.INSTANCE.toWideReadableForm(Math.round(amount));
        }
        return String.format(Locale.ROOT, "%.2f", amount);
    }

    @Override
    public void writeToNBT(CompoundNBT data) {
        super.writeToNBT(data);
        data.putDouble("throughputValue", this.lastReportedValue);
        data.putString("throughputText", this.lastHumanReadableValue);
        data.putInt("throughputRoutine", this.workRoutine.ordinal());
    }

    @Override
    public void readFromNBT(CompoundNBT data) {
        super.readFromNBT(data);
        this.lastReportedValue = data.getDouble("throughputValue");
        this.lastHumanReadableValue = data.getString("throughputText");
        this.workRoutine = WorkRoutine.fromOrdinal(data.getInt("throughputRoutine"));
    }

    @Override
    public void writeToStream(PacketBuffer data) throws IOException {
        super.writeToStream(data);
        data.writeDouble(this.lastReportedValue);
        data.writeString(this.lastHumanReadableValue);
        data.writeInt(this.workRoutine.ordinal());
    }

    @Override
    public boolean readFromStream(PacketBuffer data) throws IOException {
        boolean redraw = super.readFromStream(data);
        double value = data.readDouble();
        String text = data.readString(64);
        WorkRoutine routine = WorkRoutine.fromOrdinal(data.readInt());
        redraw |= Double.compare(this.lastReportedValue, value) != 0
                || !this.lastHumanReadableValue.equals(text)
                || this.workRoutine != routine;
        this.lastReportedValue = value;
        this.lastHumanReadableValue = text;
        this.workRoutine = routine;
        return redraw;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void renderDynamic(float partialTicks, MatrixStack matrixStack, IRenderTypeBuffer buffers,
            int combinedLightIn, int combinedOverlayIn) {
        super.renderDynamic(partialTicks, matrixStack, buffers, combinedLightIn, combinedOverlayIn);

        if (!this.isActive() || this.getDisplayed() == null || this.lastHumanReadableValue.isEmpty()) {
            return;
        }

        String sign = this.lastReportedValue > 0 ? "+" : this.lastReportedValue < 0 ? "-" : "";
        String suffix;
        switch (this.workRoutine) {
            case TICK:
                suffix = "/t";
                break;
            case MINUTE:
                suffix = "/m";
                break;
            case TEN_MINUTE:
                suffix = "/10m";
                break;
            case SECOND:
            default:
                suffix = "/s";
                break;
        }
        String rendered = sign + this.lastHumanReadableValue + suffix;

        matrixStack.push();
        matrixStack.translate(0.5, 0.5, 0.5);
        TesrRenderHelper.rotateToFace(matrixStack, this.getSide().getFacing(), this.getSpin());
        matrixStack.translate(0.0f, -0.23f, 0.52f);
        matrixStack.scale(1.0f / 72.0f, -1.0f / 72.0f, 1.0f / 72.0f);

        FontRenderer font = Minecraft.getInstance().fontRenderer;
        int width = font.getStringWidth(rendered);
        matrixStack.translate(-0.5f * width, 0.0f, 0.0f);
        font.renderString(rendered, 0, 0, 0xFFFFFF, false,
                matrixStack.getLast().getMatrix(), buffers, false, 0, 15728880);
        matrixStack.pop();
    }

    @Override
    public IPartModel getStaticModels() {
        return this.selectModel(
                StorageMonitorPart.MODELS_OFF,
                StorageMonitorPart.MODELS_ON,
                StorageMonitorPart.MODELS_HAS_CHANNEL,
                StorageMonitorPart.MODELS_LOCKED_OFF,
                StorageMonitorPart.MODELS_LOCKED_ON,
                StorageMonitorPart.MODELS_LOCKED_HAS_CHANNEL);
    }
}
