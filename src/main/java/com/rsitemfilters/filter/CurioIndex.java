package com.rsitemfilters.filter;

import com.mojang.logging.LogUtils;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Recognises items that can be equipped in an accessory ("curio") slot.
 *
 * <p>Curios decides which items fit a slot through item tags declared as
 * {@code data/curios/tags/items/<slot>.json}, so any item carrying a tag in the
 * {@code curios} namespace is equippable — rings, necklaces, belts, charms,
 * back items, hands, head, body, feet and anything a mod registers itself.
 * Reading the tags directly keeps this module free of a compile-time Curios
 * dependency and, more importantly, keeps working when a pack ships a Curios
 * build whose {@code CuriosApi} facade is not wired up (the copy in this pack
 * only logs "Missing Curios API implementation!" and returns empty maps, which
 * would make an API-based check filter everything out).</p>
 *
 * <p>Items that only implement {@code ICurioItem} without being tagged are
 * caught by a reflective {@code instanceof} fallback. Results are cached per
 * item, so the tick hook can call {@link #isCurio(ItemStack)} on every stack of
 * the grid without extra cost.</p>
 */
public final class CurioIndex {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String CURIOS_MOD_ID = "curios";
    private static final String CURIO_ITEM_INTERFACE =
            "top.theillusivec4.curios.api.type.capability.ICurioItem";

    private static final Map<Item, Boolean> CACHE = new ConcurrentHashMap<>();

    private static Boolean available;
    private static boolean interfaceResolved;
    private static Class<?> curioItemInterface;

    private CurioIndex() {
    }

    /** True when the Curios API mod is installed (regardless of any items). */
    public static boolean isAvailable() {
        Boolean cached = available;
        if (cached != null) {
            return cached;
        }
        boolean present;
        try {
            present = ModList.get().isLoaded(CURIOS_MOD_ID);
        } catch (Throwable t) {
            present = false;
        }
        available = present;
        LOGGER.info("[RSItemFilters] curios {} -> accessory filter {}",
                present ? "detected" : "absent", present ? "enabled" : "disabled");
        return present;
    }

    /** True if the stack can be worn in an accessory slot. */
    public static boolean isCurio(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        Item item = stack.getItem();
        Boolean cached = CACHE.get(item);
        if (cached != null) {
            return cached;
        }
        boolean result = compute(stack);
        CACHE.put(item, result);
        return result;
    }

    private static boolean compute(ItemStack stack) {
        try {
            // 1) Data-driven check: any tag in the "curios" namespace is a
            //    slot tag, which is exactly how Curios itself resolves slots.
            if (stack.getTags().anyMatch(CurioIndex::isCuriosTag)) {
                return true;
            }
            // 2) Fallback: items that implement ICurioItem but rely on the
            //    capability instead of (or in addition to) a slot tag.
            Class<?> iface = curioItemInterface();
            return iface != null && iface.isInstance(stack.getItem());
        } catch (Throwable t) {
            LOGGER.debug("[RSItemFilters] curio check failed for {}", stack.getItem(), t);
            return false;
        }
    }

    private static boolean isCuriosTag(TagKey<Item> tag) {
        return CURIOS_MOD_ID.equals(tag.location().getNamespace());
    }

    private static synchronized Class<?> curioItemInterface() {
        if (!interfaceResolved) {
            interfaceResolved = true;
            try {
                curioItemInterface = Class.forName(CURIO_ITEM_INTERFACE);
            } catch (Throwable ignored) {
                curioItemInterface = null;
            }
        }
        return curioItemInterface;
    }
}
