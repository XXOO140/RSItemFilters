package com.rsitemfilters.filter;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Equippable;

/**
 * Item category filters, ported from Just Enough Filters' ToggledFilters.
 *
 * Each category mirrors JEF's matching logic so filtering "food", "tools",
 * "armor", "enchanted books", etc. behaves identically.
 */
public enum ItemCategory {
    FOOD(Items.APPLE, "food"),
    TOOL(Items.DIAMOND_PICKAXE, "tool"),
    ARMOR(Items.DIAMOND_CHESTPLATE, "armor"),
    ENCHANTED_BOOK(Items.ENCHANTED_BOOK, "enchanted_book"),
    FUEL(Items.COAL, "fuel"),
    BLOCK(Items.GRASS_BLOCK, "block"),
    ITEM(Items.IRON_INGOT, "item");

    private final Item iconItem;
    private final String langKey;

    ItemCategory(Item iconItem, String key) {
        this.iconItem = iconItem;
        this.langKey = "rsitemfilters.category." + key;
    }

    public Item getIconItem() {
        return iconItem;
    }

    public String getLangKey() {
        return langKey;
    }

    // Common ("c" = forge/common) tags used by JEF.
    private static final TagKey<Item> C_FOODS = commonTag("foods");
    private static final TagKey<Item> C_TOOLS = commonTag("tools");
    private static final TagKey<Item> C_HUMANOID_ARMORS = commonTag("armors/humanoid");

    private static TagKey<Item> commonTag(String path) {
        return TagKey.create(Registries.ITEM, new ResourceLocation("c", path));
    }

    /**
     * Returns true if the given stack belongs to this category.
     * Mirrors JEF's ToggledFilters matching exactly.
     */
    public boolean matches(ItemStack stack) {
        switch (this) {
            case FOOD:
                // JEF: #c:foods tag OR has FOOD component
                return stack.is(C_FOODS) || stack.has(DataComponents.FOOD);

            case TOOL:
                // JEF: TOOL component OR #c:tools tag OR
                //      (MAX_DAMAGE && maxStackSize==1 && enchantable && not armor slot)
                boolean b0 = stack.has(DataComponents.TOOL);
                boolean b1 = stack.is(C_TOOLS);
                Equippable eq = stack.get(DataComponents.EQUIPPABLE);
                boolean b2 = stack.has(DataComponents.MAX_DAMAGE)
                        && stack.getMaxStackSize() == 1
                        && stack.isEnchantable()
                        && (eq == null || !eq.slot().isArmor());
                return b0 || b1 || b2;

            case ARMOR:
                // JEF: (MAX_DAMAGE && equipable.slot.type == HUMANOID_ARMOR) OR #c:armors/humanoid tag
                Equippable eq2 = stack.get(DataComponents.EQUIPPABLE);
                boolean a0 = stack.has(DataComponents.MAX_DAMAGE)
                        && eq2 != null
                        && eq2.slot().getType() == EquipmentSlot.Type.HUMANOID_ARMOR;
                boolean a1 = stack.is(C_HUMANOID_ARMORS);
                return a0 || a1;

            case ENCHANTED_BOOK:
                return stack.is(Items.ENCHANTED_BOOK);

            case FUEL:
                // JEF: Minecraft.level.fuelValues().isFuel(stack)
                net.minecraft.client.multiplayer.ClientLevel level =
                        net.minecraft.client.Minecraft.getInstance().level;
                return level != null && level.fuelValues().isFuel(stack);

            case BLOCK:
                return stack.getItem() instanceof BlockItem;

            case ITEM:
                return !(stack.getItem() instanceof BlockItem);

            default:
                return false;
        }
    }

    public ItemStack getIconStack() {
        return new ItemStack(iconItem);
    }
}
