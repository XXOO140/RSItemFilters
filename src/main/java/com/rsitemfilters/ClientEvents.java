package com.rsitemfilters;

import com.refinedmods.refinedstorage.screen.grid.GridScreen;
import com.rsitemfilters.filter.CategoryState;
import com.rsitemfilters.filter.GridCategoryFilter;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Keeps the category filter applied while a Grid screen is open.
 *
 * <p>Grid views rebuild their visible stack list whenever the network sends an
 * update, which would silently drop a filter that was only applied once on
 * click. Re-applying on the client tick (throttled) keeps the grid consistent
 * without relying on any particular grid implementation.</p>
 */
@Mod.EventBusSubscriber(modid = RSItemFilters.MOD_ID, value = Dist.CLIENT)
public final class ClientEvents {

    private static final int CHECK_INTERVAL_TICKS = 5;

    private static int counter;

    private ClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (CategoryState.get() == null) {
            return;
        }
        if (++counter % CHECK_INTERVAL_TICKS != 0) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof GridScreen gridScreen) {
            if (GridCategoryFilter.needsFiltering(gridScreen)) {
                GridCategoryFilter.apply(gridScreen);
            }
        }
    }
}
