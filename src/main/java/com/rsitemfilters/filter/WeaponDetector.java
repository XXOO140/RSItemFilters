package com.rsitemfilters.filter;

import com.google.common.collect.Multimap;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import org.slf4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Recognises weapons: melee, ranged and modded ones.
 *
 * <p>No mod in this pack registers a usable "weapons" tag (only a handful ship
 * partial {@code forge:tools/...} tags), so class checks alone would miss most
 * modded gear. The reliable signal is the item's own attack-damage attribute:
 * vanilla gives swords {@code +3} and better, axes {@code +6} and better,
 * tridents {@code +8}, while pickaxes/hoes stay at {@code +1} and shovels at
 * {@code +1.5}. A threshold of {@code +2} therefore keeps every weapon — vanilla
 * or modded, since modded weapons are built from the same attribute system —
 * without dragging tools into the category.</p>
 *
 * <p>Bows and crossbows carry no attack-damage modifier (their damage lives in
 * the projectile), so they are matched by class and by tag.</p>
 */
public final class WeaponDetector {

    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Attack-damage bonus over the player's base 1.0 that an item must grant to
     * count as a weapon. Swords start at +3, shovels sit at +1.5.
     */
    private static final double MIN_ATTACK_DAMAGE_BONUS = 2.0;

    private static final Map<Item, Boolean> CACHE = new ConcurrentHashMap<>();

    private static final TagKey<Item>[] WEAPON_TAGS = new TagKey[]{
            forgeTag("tools/swords"),
            forgeTag("tools/bows"),
            forgeTag("tools/crossbows"),
            forgeTag("tools/tridents"),
            commonTag("tools/swords"),
            commonTag("tools/bows"),
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("minecraft", "weapons"))
    };

    private WeaponDetector() {
    }

    /** True if the stack is a weapon (melee or ranged). */
    public static boolean isWeapon(ItemStack stack) {
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
        // Only log once per item, and only for hits, to keep the log readable.
        if (result) {
            LOGGER.debug("[RSItemFilters] weapon detected: {}", item);
        }
        return result;
    }

    private static boolean compute(ItemStack stack) {
        Item item = stack.getItem();
        try {
            if (item instanceof SwordItem
                    || item instanceof TridentItem
                    || item instanceof ProjectileWeaponItem) {
                return true;
            }
            for (TagKey<Item> tag : WEAPON_TAGS) {
                if (stack.is(tag)) {
                    return true;
                }
            }
            return hasWeaponDamage(stack);
        } catch (Throwable t) {
            LOGGER.debug("[RSItemFilters] weapon check failed for {}", item, t);
            return false;
        }
    }

    private static boolean hasWeaponDamage(ItemStack stack) {
        Multimap<Attribute, AttributeModifier> modifiers =
                stack.getAttributeModifiers(EquipmentSlot.MAINHAND);
        if (modifiers.isEmpty()) {
            return false;
        }
        for (Map.Entry<Attribute, AttributeModifier> entry : modifiers.entries()) {
            if (entry.getKey() != Attributes.ATTACK_DAMAGE) {
                continue;
            }
            AttributeModifier modifier = entry.getValue();
            // Only flat additions tell us the actual hit damage; multiplied
            // modifiers are relative and would misfire on unrelated items.
            if (modifier.getOperation() == AttributeModifier.Operation.ADDITION
                    && modifier.getAmount() >= MIN_ATTACK_DAMAGE_BONUS) {
                return true;
            }
        }
        return false;
    }

    private static TagKey<Item> forgeTag(String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("forge", path));
    }

    private static TagKey<Item> commonTag(String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", path));
    }
}
