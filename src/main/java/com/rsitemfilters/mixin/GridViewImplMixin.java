package com.rsitemfilters.mixin;

import com.mojang.logging.LogUtils;
import com.refinedmods.refinedstorage.screen.grid.GridScreen;
import com.refinedmods.refinedstorage.screen.grid.stack.IGridStack;
import com.refinedmods.refinedstorage.screen.grid.stack.ItemGridStack;
import com.refinedmods.refinedstorage.screen.grid.view.GridViewImpl;
import com.rsitemfilters.filter.CategoryState;
import com.rsitemfilters.filter.ItemCategory;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Applies the item-category filter to the Grid's visible stack list.
 *
 * <p>Two complementary hooks are used on purpose:</p>
 * <ul>
 *   <li>{@code getActiveFilters} - wraps RS's own search predicate so the
 *       filter participates in RS's normal filter/sort pipeline.</li>
 *   <li>{@code forceSort} (TAIL) - directly trims the private {@code stacks}
 *       display list. This is the guaranteed path: it does not depend on how
 *       RS or third-party mods (e.g. rs_integration's Unified Grid) build the
 *       predicate, nor on the concrete {@link IGridStack} implementation
 *       actually being an {@link ItemGridStack}.</li>
 * </ul>
 */
@Mixin(GridViewImpl.class)
public abstract class GridViewImplMixin {

    private static final Logger LOGGER = LogUtils.getLogger();

    // Refined Storage classes are not obfuscated, so shadow members must opt
    // out of remapping (otherwise Mixin cannot resolve the mapping).
    @Shadow(remap = false)
    private List<IGridStack> stacks;

    @Shadow(remap = false)
    private GridScreen screen;

    /**
     * Extracts the underlying ItemStack in an implementation-agnostic way, so
     * wrapper stacks added by other mods still resolve correctly.
     */
    private static ItemStack rsitemfilters$asItemStack(IGridStack stack) {
        if (stack instanceof ItemGridStack itemGridStack) {
            return itemGridStack.getStack();
        }
        Object ingredient = stack.getIngredient();
        if (ingredient instanceof ItemStack itemStack) {
            return itemStack;
        }
        return null;
    }

    @Inject(method = "forceSort", at = @At("TAIL"), remap = false)
    private void rsitemfilters$filterAfterSort(CallbackInfo ci) {
        ItemCategory category = CategoryState.get();
        if (category == null) {
            return;
        }

        List<IGridStack> current = this.stacks;
        if (current == null || current.isEmpty()) {
            LOGGER.info("[RSItemFilters] forceSort tail: list empty, nothing to filter");
            return;
        }

        List<IGridStack> kept = new ArrayList<>(current.size());
        int removed = 0;
        String sample = null;
        for (IGridStack stack : current) {
            ItemStack itemStack = rsitemfilters$asItemStack(stack);
            if (sample == null) {
                sample = stack.getClass().getName() + " / "
                        + (itemStack == null ? "<no item>" : itemStack.getItem().toString());
            }
            if (itemStack == null || category.matches(itemStack)) {
                kept.add(stack);
            } else {
                removed++;
            }
        }

        this.stacks = kept;
        LOGGER.info("[RSItemFilters] forceSort filter cat={} kept={} removed={} sample={}",
                category, kept.size(), removed, sample);

        // The scrollbar is refreshed inside forceSort before this hook runs, so
        // refresh it again now that the list has been trimmed.
        if (this.screen != null) {
            this.screen.updateScrollbar();
        }
    }

    @Inject(method = "getActiveFilters", at = @At("RETURN"), cancellable = true, remap = false)
    private void rsitemfilters$applyCategoryFilter(CallbackInfoReturnable<Predicate<IGridStack>> cir) {
        ItemCategory category = CategoryState.get();
        if (category == null) {
            return;
        }

        Predicate<IGridStack> original = cir.getReturnValue();
        if (original == null) {
            return;
        }

        LOGGER.info("[RSItemFilters] getActiveFilters wrapped for {}", category);

        cir.setReturnValue(stack -> {
            if (!original.test(stack)) {
                return false;
            }
            ItemStack itemStack = rsitemfilters$asItemStack(stack);
            // Non-item stacks (fluids, etc.) always pass through.
            return itemStack == null || category.matches(itemStack);
        });
    }
}
