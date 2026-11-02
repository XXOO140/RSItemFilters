package com.rsitemfilters.mixin;

import com.refinedmods.refinedstorage.screen.grid.GridScreen;
import com.refinedmods.refinedstorage.screen.grid.view.IGridView;
import com.rsitemfilters.filter.CategoryState;
import com.rsitemfilters.filter.ItemCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Adds a row of item-category filter buttons (food / tool / armor / enchanted
 * book / fuel / block / item) to the Refined Storage Grid screen, mirroring
 * Just Enough Filters' category toggles.
 */
@Mixin(GridScreen.class)
public abstract class GridScreenMixin {

    private final List<CategoryButton> rsitemfilters$buttons = new ArrayList<>();

    @Inject(method = "onPostInit", at = @At("TAIL"), remap = false)
    private void rsitemfilters$addCategoryButtons(int x, int y, CallbackInfo ci) {
        GridScreen self = (GridScreen) (Object) this;

        rsitemfilters$buttons.clear();

        int buttonSize = 18;
        int gap = 2;
        int startX = x + 7;
        int startY = y + self.getTopHeight() - 1;

        ItemCategory[] categories = ItemCategory.values();
        for (int i = 0; i < categories.length; i++) {
            ItemCategory category = categories[i];
            int bx = startX + i * (buttonSize + gap);
            int by = startY;

            CategoryButton button = new CategoryButton(self, category, bx, by);
            rsitemfilters$buttons.add(button);
            self.addRenderableWidget(button);
        }
    }

    private class CategoryButton extends Button {
        private final GridScreen screen;
        private final ItemCategory category;

        CategoryButton(GridScreen screen, ItemCategory category, int x, int y) {
            super(x, y, 18, 18, Component.empty(), btn -> {
                CategoryState.toggle(category);
                // Re-apply filters after the category change.
                IGridView view = screen.getView();
                if (view != null) {
                    view.forceSort();
                }
            }, Button.DEFAULT_NARRATION);
            this.screen = screen;
            this.category = category;
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
            boolean active = CategoryState.isActive(category);

            // Background: highlighted when active, dark otherwise.
            int bgColor = active ? 0xFF3FA04D : 0xFF5C5C5C;
            graphics.fill(getX(), getY(), getX() + width, getY() + height, 0xFF000000);
            graphics.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, bgColor);

            // Render the category icon item.
            ItemStack icon = category.getIconStack();
            graphics.renderItem(icon, getX() + 1, getY() + 1);

            // Hover highlight.
            if (isHoveredOrFocused()) {
                graphics.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, 0x20FFFFFF);
            }
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
            renderWidget(graphics, mouseX, mouseY, partialTicks);
            if (isHoveredOrFocused()) {
                screen.renderTooltip(graphics, mouseX, mouseY,
                        net.minecraft.client.resources.language.I18n.get(category.getLangKey()));
            }
        }
    }
}
