package com.rsitemfilters.mixin;

import com.refinedmods.refinedstorage.screen.grid.GridScreen;
import com.refinedmods.refinedstorage.screen.grid.view.IGridView;
import com.rsitemfilters.filter.CategoryState;
import com.rsitemfilters.filter.GridCategoryFilter;
import com.rsitemfilters.filter.ItemCategory;
import com.mojang.logging.LogUtils;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds a column of item-category filter buttons (food / tool / armor /
 * enchanted book / fuel / block / item) to the Refined Storage Grid screen.
 */
@Mixin(GridScreen.class)
public abstract class GridScreenMixin {

    private static final Logger LOGGER = LogUtils.getLogger();

    @Inject(method = "onPostInit", at = @At("TAIL"), remap = false)
    private void rsitemfilters$addCategoryButtons(int x, int y, CallbackInfo ci) {
        GridScreen self = (GridScreen) (Object) this;

        int buttonWidth = 48;
        int buttonHeight = 16;
        int gap = 2;
        int startX = x + 227 + 4;
        int startY = y + 6;

        ItemCategory[] categories = ItemCategory.values();
        for (int i = 0; i < categories.length; i++) {
            ItemCategory category = categories[i];
            int bx = startX;
            int by = startY + i * (buttonHeight + gap);

            Button button = self.addButton(bx, by, buttonWidth, buttonHeight,
                    Component.translatable(category.getLangKey()),
                    true, true, btn -> {
                        CategoryState.toggle(category);
                        boolean active = CategoryState.isActive(category);
                        // Visual feedback: prefix a check mark while active.
                        btn.setMessage(Component.literal(active ? "✓ " : "")
                                .append(Component.translatable(category.getLangKey())));

                        // Rebuild the view first (this restores the full,
                        // un-filtered list), then trim it to the category.
                        // Works for RS's own GridViewImpl as well as replacement
                        // views such as rs_integration's UnifiedGridView.
                        IGridView view = self.getView();
                        if (view != null) {
                            view.forceSort();
                        }
                        GridCategoryFilter.apply(self);
                        LOGGER.info("[RSItemFilters] clicked {} -> active={}", category, active);
                    });
        }
        LOGGER.info("[RSItemFilters] Added {} category buttons to GridScreen", categories.length);
    }
}
