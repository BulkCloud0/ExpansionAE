package dev.bulkcloud.expansionae;

import appeng.api.config.FuzzyMode;
import appeng.api.config.LevelType;
import appeng.api.config.RedstoneMode;
import appeng.api.config.SecurityPermissions;
import appeng.api.config.Settings;
import appeng.api.config.YesNo;
import appeng.container.SlotSemantic;
import appeng.container.guisync.GuiSync;
import appeng.container.implementations.ContainerTypeBuilder;
import appeng.container.implementations.UpgradeableContainer;
import appeng.container.slot.FakeTypeOnlySlot;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.ContainerType;
import net.minecraftforge.items.IItemHandler;

public final class ThresholdLevelEmitterContainer extends UpgradeableContainer {
    public static final ContainerType<ThresholdLevelEmitterContainer> TYPE = ContainerTypeBuilder
            .create(ThresholdLevelEmitterContainer::new, ThresholdLevelEmitter.class)
            .requirePermission(SecurityPermissions.BUILD)
            .withInitialData((host, buffer) -> {
                buffer.writeVarLong(host.getUpperValue());
                buffer.writeVarLong(host.getLowerValue());
            }, (host, container, buffer) -> {
                container.upperValue = buffer.readVarLong();
                container.lowerValue = buffer.readVarLong();
            })
            .build("expansionae_threshold_level_emitter");

    private final ThresholdLevelEmitter host;

    @GuiSync(7)
    public long upperValue;
    @GuiSync(8)
    public long lowerValue;
    @GuiSync(9)
    public LevelType levelType;
    @GuiSync(10)
    public YesNo craftingMode;

    public ThresholdLevelEmitterContainer(int id, PlayerInventory player, ThresholdLevelEmitter host) {
        super(TYPE, id, player, host);
        this.host = host;
        registerClientAction("setUpperValue", Long.class, this::setUpperValue);
        registerClientAction("setLowerValue", Long.class, this::setLowerValue);
    }

    @Override
    protected void setupConfig() {
        setupUpgrades();
        IItemHandler config = getUpgradeable().getInventoryByName("config");
        addSlot(new FakeTypeOnlySlot(config, 0), SlotSemantic.CONFIG);
    }

    @Override
    protected boolean supportCapacity() {
        return false;
    }

    @Override
    public int availableUpgrades() {
        return 1;
    }

    public void setUpperValue(long value) {
        value = Math.max(0, value);
        if (isRemote()) {
            if (value != upperValue) {
                upperValue = value;
                sendClientAction("setUpperValue", value);
            }
        } else {
            host.setUpperValue(value);
            upperValue = host.getUpperValue();
        }
    }

    public void setLowerValue(long value) {
        value = Math.max(0, value);
        if (isRemote()) {
            if (value != lowerValue) {
                lowerValue = value;
                sendClientAction("setLowerValue", value);
            }
        } else {
            host.setLowerValue(value);
            lowerValue = host.getLowerValue();
        }
    }

    @Override
    public void detectAndSendChanges() {
        verifyPermissions(SecurityPermissions.BUILD, false);
        if (isServer()) {
            upperValue = host.getUpperValue();
            lowerValue = host.getLowerValue();
            levelType = (LevelType) host.getConfigManager().getSetting(Settings.LEVEL_TYPE);
            craftingMode = (YesNo) host.getConfigManager().getSetting(Settings.CRAFT_VIA_REDSTONE);
            setFuzzyMode((FuzzyMode) host.getConfigManager().getSetting(Settings.FUZZY_MODE));
            setRedStoneMode((RedstoneMode) host.getConfigManager().getSetting(Settings.REDSTONE_EMITTER));
        }
        standardDetectAndSendChanges();
    }

    public LevelType getLevelType() {
        return levelType;
    }

    @Override
    public YesNo getCraftingMode() {
        return craftingMode;
    }

    @Override
    public void setCraftingMode(YesNo mode) {
        craftingMode = mode;
    }
}
