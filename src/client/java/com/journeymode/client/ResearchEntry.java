package com.journeymode.client;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * One thing in the item list: an item, or one kind of an item (a particular potion, or a book with a
 * particular enchantment).
 *
 * @param key   what it is researched under (see {@code ResearchKeys})
 * @param item  the item
 * @param stack one of it, as it is shown in the list and given out
 */
public record ResearchEntry(String key, Item item, ItemStack stack) {
}
