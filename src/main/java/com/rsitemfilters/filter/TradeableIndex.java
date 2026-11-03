package com.rsitemfilters.filter;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Knows which items can be sold in the "香草纪元" selling bin.
 *
 * <p>The selling mod (xiangcaomengjia) exposes sellable items through two
 * independent sources, and the tooltip shown in-game is drawn from whichever
 * one has data:</p>
 * <ul>
 *   <li>{@code SellingBinConfig.getInstance().getAllSellableItemIds()} —
 *       rules loaded from the local config file. This is the source used in
 *       single-player and when no server list has been synced.</li>
 *   <li>{@code SellingBinConfigClient.getTradeList()} — the list pushed from
 *       the server via {@code PacketTradeList}.</li>
 * </ul>
 * <p>The union of both is indexed, so the category stays correct in both
 * single-player and on a server.</p>
 *
 * <p>The mod is an optional dependency, so everything goes through reflection
 * and the feature reports itself as unavailable when it is absent. Its own
 * {@code findTrade(ItemStack)} is deliberately not used: it also requires
 * {@code stack.getCount() >= entry.count()}, so a single unit of a stackable
 * item would be reported as "not sellable".</p>
 */
public final class TradeableIndex {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String CONFIG_CLASS =
            "com.xiangcao.xiangcaomengjia.config.SellingBinConfig";
    private static final String CLIENT_CLASS =
            "com.xiangcao.xiangcaomengjia.config.SellingBinConfigClient";
    private static final String ENTRY_CLASS = CLIENT_CLASS + "$TradeEntry";

    private static boolean resolved;

    private static Method configGetInstance;
    private static Method configGetAllSellableIds;
    private static Method clientGetTradeList;
    private static Method entryItemId;

    /** Item ids (e.g. {@code minecraft:carrot}) that can currently be sold. */
    private static volatile Set<String> tradeableIds = Collections.emptySet();

    private TradeableIndex() {
    }

    /**
     * Re-reads both sellable-item sources and rebuilds the id set.
     * Cheap enough to call on a button click.
     */
    public static void refresh() {
        if (!resolve()) {
            return;
        }

        Set<String> next = new HashSet<>();

        // 1) Local config rules.
        try {
            Object config = configGetInstance.invoke(null);
            Object ids = configGetAllSellableIds.invoke(config);
            if (ids instanceof Set<?> set) {
                for (Object id : set) {
                    if (id instanceof String text && !text.isEmpty()) {
                        next.add(text);
                    }
                }
            }
        } catch (Throwable t) {
            LOGGER.warn("[RSItemFilters] reading local sell rules failed: {}", t.toString());
        }

        int localCount = next.size();

        // 2) Server-synced trade list.
        try {
            Object listObject = clientGetTradeList.invoke(null);
            if (listObject instanceof List<?> list) {
                for (Object entry : list) {
                    if (entry == null) {
                        continue;
                    }
                    Object id = entryItemId.invoke(entry);
                    if (id instanceof String text && !text.isEmpty()) {
                        next.add(text);
                    }
                }
            }
        } catch (Throwable t) {
            LOGGER.warn("[RSItemFilters] reading synced trade list failed: {}", t.toString());
        }

        tradeableIds = next;
        LOGGER.info("[RSItemFilters] sellable ids indexed: {} (local rules {}, synced +{})",
                next.size(), localCount, next.size() - localCount);
    }

    /** True when the selling mod is present (regardless of whether it has data yet). */
    public static boolean isAvailable() {
        return resolve();
    }

    public static boolean hasData() {
        return !tradeableIds.isEmpty();
    }

    /**
     * True if the stack's item appears in either sellable-item source.
     *
     * <p>Matches on item id only, deliberately ignoring any required count,
     * because the category answers "can this item be sold at all".</p>
     */
    public static boolean isTradeable(ItemStack stack) {
        Set<String> ids = tradeableIds;
        if (stack == null || stack.isEmpty() || ids.isEmpty()) {
            return false;
        }
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return key != null && ids.contains(key.toString());
    }

    private static synchronized boolean resolve() {
        if (resolved) {
            return configGetAllSellableIds != null;
        }
        resolved = true;
        try {
            Class<?> config = Class.forName(CONFIG_CLASS);
            configGetInstance = config.getMethod("getInstance");
            configGetAllSellableIds = config.getMethod("getAllSellableItemIds");

            try {
                Class<?> client = Class.forName(CLIENT_CLASS);
                Class<?> entry = Class.forName(ENTRY_CLASS);
                clientGetTradeList = client.getMethod("getTradeList");
                entryItemId = entry.getMethod("itemId");
            } catch (Throwable t) {
                // Server-synced list is optional; local rules are enough.
                clientGetTradeList = null;
                entryItemId = null;
            }

            LOGGER.info("[RSItemFilters] selling mod detected, tradeable filter enabled");
        } catch (Throwable t) {
            configGetAllSellableIds = null;
            LOGGER.info("[RSItemFilters] selling mod not present, tradeable filter disabled");
        }
        return configGetAllSellableIds != null;
    }
}
