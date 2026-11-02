package com.rsitemfilters.mixin;

import com.refinedmods.refinedstorage.screen.grid.stack.IGridStack;
import com.refinedmods.refinedstorage.screen.grid.stack.ItemGridStack;
import com.refinedmods.refinedstorage.screen.grid.view.GridViewImpl;
import com.rsitemfilters.filter.CategoryState;
import com.rsitemfilters.filter.ItemCategory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Predicate;

/**
 * Wraps the Grid's active filter predicate with an additional
 * item-category filter so selecting "food" only shows food items, etc.
 */
@Mixin(GridViewImpl.class)
public abstract class GridViewImplMixin {

    @Inject(method = "getActiveFilters", at = @At("RETURN"), cancellable = true, remap = false)
    private void rsitemfilters$applyCategoryFilter(CallbackInfoReturnable<Predicate<IGridStack>> cir) {
        ItemCategory category = CategoryState.get();
        if (category == null) {
            return; // no category selected, keep original filters
        }

        Predicate<IGridStack> original = cir.getReturnValue();
        Predicate<IGridStack> combined = stack -> {
            if (!original.test(stack)) {
                return false;
            }
            // Non-item stacks (fluids) always pass through when no item to judge.
            if (!(stack instanceof ItemGridStack)) {
                return false;
            }
            ItemStack itemStack = ((ItemGridStack) stack).getStack();
            return category.matches(itemStack);
        };

        cir.setReturnValue(combined);
    }
}
