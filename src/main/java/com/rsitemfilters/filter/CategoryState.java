package com.rsitemfilters.filter;

/**
 * Holds the currently selected item category across the Grid screens.
 * This is client-side only state, reset to NONE when no category is active.
 */
public class CategoryState {
    private static ItemCategory current = null;

    private CategoryState() {
    }

    public static void set(ItemCategory category) {
        current = category;
    }

    public static ItemCategory get() {
        return current;
    }

    public static void toggle(ItemCategory category) {
        if (current == category) {
            current = null;
        } else {
            current = category;
        }
    }

    public static boolean isActive(ItemCategory category) {
        return current == category;
    }

    public static void clear() {
        current = null;
    }
}
