package com.bulkcloud.expansionae.ae2;

import com.bulkcloud.expansionae.ExpansionAE;
import com.bulkcloud.expansionae.feature.disk.DiskCellHandler;

import appeng.api.AEAddon;
import appeng.api.IAEAddon;
import appeng.api.IAppEngApi;

@AEAddon
public final class ExpansionAEApi implements IAEAddon {
    private static IAppEngApi api;

    public ExpansionAEApi() {
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
