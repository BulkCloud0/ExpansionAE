package com.bulkcloud.expansionae.ae2;

import com.bulkcloud.expansionae.ExpansionAE;
import com.bulkcloud.expansionae.feature.disk.DiskCellHandler;
import com.bulkcloud.expansionae.feature.extendedbus.ExpansionExportBusPart;
import com.bulkcloud.expansionae.feature.extendedbus.ExpansionImportBusPart;

import appeng.api.AEAddon;
import appeng.api.IAEAddon;
import appeng.api.IAppEngApi;
import appeng.core.Api;

@AEAddon
public final class ExpansionAEApi implements IAEAddon {
    private static IAppEngApi api;

    public ExpansionAEApi() {
    }

    public static void registerPartModelsEarly() {
        Api.instance().registries().partModels().registerModels(
                ExpansionImportBusPart.MODEL_BASE,
                ExpansionExportBusPart.MODEL_BASE);
    }

    @Override
    public void onAPIAvailable(IAppEngApi api) {
        ExpansionAEApi.api = api;
        api.registries().cell().addCellHandler(DiskCellHandler.INSTANCE);
        ExpansionAE.LOGGER.info(
                "AE2 API available; registered ExpansionAE cell handlers ({} storage channels visible)",
                api.storage().storageChannels().size());
    }

    public static boolean isAvailable() {
        return api != null;
    }

    public static IAppEngApi get() {
        if (api == null) {
            throw new IllegalStateException("AE2 API is not available yet");
        }
        return api;
    }
}
