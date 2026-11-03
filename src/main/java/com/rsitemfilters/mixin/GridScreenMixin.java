package com.rsitemfilters.mixin;

import com.refinedmods.refinedstorage.screen.grid.GridScreen;
import com.refinedmods.refinedstorage.screen.grid.view.IGridView;
import com.rsitemfilters.filter.CategoryState;
import com.rsitemfilters.filter.ItemCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds a row of item-category filter buttons (food / tool / armor / enchanted
 * book / fuel / block / item) to the Refined Storage Grid screen, mirroring
 * Just Enough Filters' category toggles.
 */
@Mixin(GridScreen.class)
public abstract class GridScreenMixin {

    @Inject(method = "onPostInit", at = @At("TAIL"), remap = false)
    private void rsitemfilters$addCategoryButtons(int x, int y, CallbackInfo ci) {
        GridScreen self = (GridScreen) (Object) this;

        int buttonSize = 18;
        int gap = 2;
        int startX = x + 7;
        int startY = y + self.getTopHeight() - 1;

        ItemCategory[] categories = ItemCategory.values();
        for (int i = 0; i < categories.length; i++) {
            ItemCategory category = categories[i];
            int bx = startX + i * (buttonSize + gap);

            self.addButton(bx, startY, buttonSize, buttonSize,
                    Component.translatable(category.getLangKey()),
                    true, true, btn -> {
                        CategoryState.toggle(category);
                        IGridView view = self.getView();
                        if (view != null) {
                            view.forceSort();
                        }
                    });
        }
    }
}
