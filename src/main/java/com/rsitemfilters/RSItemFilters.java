package com.rsitemfilters;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod(RSItemFilters.MOD_ID)
public class RSItemFilters {
    public static final String MOD_ID = "rsitemfilters";
    public static final Logger LOGGER = LogUtils.getLogger();

    public RSItemFilters() {
        // Force-load the lazily-loaded Refined Storage view class so that the
        // Mixin subsystem can locate it when the "client" mixin config is
        // applied during early game startup. Without this, GridViewImpl is only
        // loaded the first time a Grid screen is opened — which is too late,
        // and GridViewImplMixin gets silently skipped.
        try {
            Class.forName("com.refinedmods.refinedstorage.screen.grid.view.GridViewImpl");
            LOGGER.info("[RSItemFilters] Preloaded GridViewImpl for mixin targeting");
        } catch (ClassNotFoundException e) {
            LOGGER.warn("[RSItemFilters] GridViewImpl not found at startup: {}", e.getMessage());
        }
    }
}
