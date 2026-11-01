package com.journeymode.research;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** How many of an item must be sacrificed before it counts as researched. */
public final class ResearchCosts {
	private ResearchCosts() {
	}

	/** A full stack: 1 for tools and armor, 16 for ender pearls, 64 for blocks and most materials. */
	public static int required(Item item) {
		return Math.max(1, new ItemStack(item).getMaxStackSize());
	}
}