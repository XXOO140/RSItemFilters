package com.rsitemfilters.filter;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.common.ForgeHooks;

/**
 * Item category filters, adapted for Minecraft 1.20.1 (Forge 47).
 *
 * Each category approximates JEF's "food / tools / armor / enchanted books /
 * fuel / blocks / items" matching, but uses the 1.20.1 API surface (the
 * upstream JEF targets 1.21's data-component API, which does not exist here).
 */
public enum ItemCategory {
    FOOD(Items.APPLE, "food"),
    TOOL(Items.DIAMOND_PICKAXE, "tool"),
    ARMOR(Items.DIAMOND_CHESTPLATE, "armor"),
    ENCHANTED_BOOK(Items.ENCHANTED_BOOK, "enchanted_book"),
    FUEL(Items.COAL, "fuel"),
    BLOCK(Items.GRASS_BLOCK, "block"),
    ITEM(Items.IRON_INGOT, "item"),
    /**
     * Items that can be worn in an accessory slot (Curios). Hidden from the
     * UI when Curios is absent.
     */
    ACCESSORY(Items.AMETHYST_SHARD, "accessory"),
    /**
     * Items the selling bin accepts, read from the economy mod's synced
     * trade list. Hidden from the UI when that mod is absent.
     */
    TRADEABLE(Items.EMERALD, "tradeable");

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

    // Common ("c" = forge/common) tags, matching JEF's CTags.
    private static final TagKey<Item> C_FOODS = commonTag("foods");
    private static final TagKey<Item> C_TOOLS = commonTag("tools");
    private static final TagKey<Item> C_ARMORS = commonTag("armors");

    private static TagKey<Item> commonTag(String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", path));
    }

    /**
     * Returns true if the given stack belongs to this category.
     */
    public boolean matches(ItemStack stack) {
        Item item = stack.getItem();
        switch (this) {
            case FOOD:
                // Tag OR edible item.
                return stack.is(C_FOODS) || item.isEdible();

            case TOOL:
                // Tool/weapon tiered items (pickaxe/axe/shovel/sword/hoe) OR tag.
                return item instanceof TieredItem || stack.is(C_TOOLS);

            case ARMOR:
                // Armor item OR armor tag.
                return item instanceof ArmorItem || stack.is(C_ARMORS);

            case ENCHANTED_BOOK:
                return stack.is(Items.ENCHANTED_BOOK);

            case FUEL:
                // 1.20.1 fuel check: positive burn time via ForgeHooks.
                return ForgeHooks.getBurnTime(stack, RecipeType.SMELTING) > 0;

            case BLOCK:
                return item instanceof BlockItem;

            case ITEM:
                return !(item instanceof BlockItem);

            case TRADEABLE:
                // Items accepted by the selling bin (see TradeableIndex).
                return TradeableIndex.isTradeable(stack);

            case ACCESSORY:
                // Items wearable in a Curios slot (see CurioIndex).
                return CurioIndex.isCurio(stack);

            default:
                return false;
        }
    }

    public ItemStack getIconStack() {
        return new ItemStack(iconItem);
    }
}
