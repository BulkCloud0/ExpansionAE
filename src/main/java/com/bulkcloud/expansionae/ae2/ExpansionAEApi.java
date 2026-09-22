package com.bulkcloud.expansionae.ae2;

import appeng.api.AEAddon;
import appeng.api.IAEAddon;
import appeng.api.IAppEngApi;

import com.bulkcloud.expansionae.ExpansionAE;

@AEAddon
public final class ExpansionAEApi implements IAEAddon {
    private static IAppEngApi api;

    public ExpansionAEApi() {
    }

    @Override
    public void onAPIAvailable(IAppEngApi api) {
        ExpansionAEApi.api = api;
        ExpansionAE.LOGGER.info(
                "AE2 API available; {} storage channel(s) registered",
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
