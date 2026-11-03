package com.rsitemfilters.filter;

import com.mojang.logging.LogUtils;
import com.refinedmods.refinedstorage.screen.grid.GridScreen;
import com.refinedmods.refinedstorage.screen.grid.stack.IGridStack;
import com.refinedmods.refinedstorage.screen.grid.stack.ItemGridStack;
import com.refinedmods.refinedstorage.screen.grid.view.IGridView;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import java.util.List;

/**
 * Applies the selected category filter to a Grid screen.
 *
 * <p>Implementation note: Refined Storage exposes the rendered stack list
 * through {@link IGridView#getStacks()}, and every implementation (RS's own
 * {@code GridViewImpl} as well as replacement views such as the Unified Grid
 * from rs_integration) returns its live backing list. Filtering that list
 * in place therefore works no matter which view implementation is in use,
 * which a Mixin into a single concrete class would not guarantee.</p>
 */
public final class GridCategoryFilter {

    private static final Logger LOGGER = LogUtils.getLogger();

    private GridCategoryFilter() {
    }

    /**
     * Resolves the underlying ItemStack without assuming a concrete
     * {@link IGridStack} implementation.
     */
    public static ItemStack itemOf(IGridStack stack) {
        if (stack == null) {
            return null;
        }
        if (stack instanceof ItemGridStack itemGridStack) {
            return itemGridStack.getStack();
        }
        Object ingredient = stack.getIngredient();
        return ingredient instanceof ItemStack itemStack ? itemStack : null;
    }

    /**
     * Trims the grid's visible stacks down to the selected category.
     *
     * @return true if the list actually changed
     */
    public static boolean apply(GridScreen screen) {
        ItemCategory category = CategoryState.get();
        if (category == null || screen == null) {
            return false;
        }

        IGridView view = screen.getView();
        if (view == null) {
            return false;
        }

        List<IGridStack> stacks = view.getStacks();
        if (stacks == null || stacks.isEmpty()) {
            return false;
        }

        try {
            int before = stacks.size();
            // Non-item stacks (fluids etc.) are always kept.
            boolean changed = stacks.removeIf(stack -> {
                ItemStack itemStack = itemOf(stack);
                return itemStack != null && !category.matches(itemStack);
            });

            if (changed) {
                screen.updateScrollbar();
                LOGGER.info("[RSItemFilters] filtered cat={} view={} {} -> {}",
                        category, view.getClass().getSimpleName(), before, stacks.size());
            }
            return changed;
        } catch (UnsupportedOperationException e) {
            LOGGER.warn("[RSItemFilters] grid stack list is immutable for {}, cannot filter",
                    view.getClass().getName());
            return false;
        }
    }

    /**
     * Cheap check used by the tick hook: does the current list still contain
     * something that should be hidden?
     */
    public static boolean needsFiltering(GridScreen screen) {
        ItemCategory category = CategoryState.get();
        if (category == null || screen == null) {
            return false;
        }
        IGridView view = screen.getView();
        if (view == null) {
            return false;
        }
        List<IGridStack> stacks = view.getStacks();
        if (stacks == null || stacks.isEmpty()) {
            return false;
        }
        for (IGridStack stack : stacks) {
            ItemStack itemStack = itemOf(stack);
            if (itemStack != null && !category.matches(itemStack)) {
                return true;
            }
        }
        return false;
    }
}
