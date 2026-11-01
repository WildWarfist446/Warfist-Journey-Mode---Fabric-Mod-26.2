package com.journeymode.research;

import java.util.Set;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The tabs of the researched item list. An item can belong to several tabs; every tab is checked on
 * its own. Each one matches item tags first (the vanilla and common {@code c:} tags that other mods
 * add their items to), then what the item is made of (a block, food, a tool), then a short list of
 * vanilla items that have no tag. A tag that does not exist simply matches nothing.
 */
public enum ItemCategory {
	ALL("all", Items.COMPASS),
	BLOCKS("blocks", Items.GRASS_BLOCK),
	TOOLS("tools", Items.IRON_PICKAXE),
	MATERIALS("materials", Items.DIAMOND),
	FOOD("food", Items.APPLE),
	MISC("misc", Items.REDSTONE);

	private static final Set<String> TOOL_TAGS = Set.of(
			"minecraft:pickaxes", "minecraft:axes", "minecraft:shovels", "minecraft:hoes", "minecraft:swords", "minecraft:spears",
			"minecraft:head_armor", "minecraft:chest_armor", "minecraft:leg_armor", "minecraft:foot_armor",
			"c:tools", "c:armors");
	private static final Set<String> TOOL_ITEMS = Set.of(
			"shield", "bow", "crossbow", "trident", "mace", "elytra", "fishing_rod", "flint_and_steel", "shears", "brush",
			"spyglass", "carrot_on_a_stick", "warped_fungus_on_a_stick", "totem_of_undying", "arrow", "spectral_arrow");

	private static final Set<String> MATERIAL_TAGS = Set.of(
			"c:ingots", "c:gems", "c:raw_materials", "c:nuggets", "c:dusts", "c:rods", "c:dyes", "c:slime_balls",
			"c:leathers", "c:feathers", "c:strings", "c:bones", "c:gunpowders", "c:ender_pearls", "c:eggs",
			"minecraft:coals", "minecraft:dyes", "minecraft:eggs");
	private static final Set<String> MATERIAL_ITEMS = Set.of(
			"stick", "string", "feather", "leather", "rabbit_hide", "bone", "gunpowder", "blaze_rod", "ender_pearl",
			"clay_ball", "brick", "resin_brick", "nether_brick", "flint", "paper", "slime_ball", "ink_sac", "glow_ink_sac",
			"phantom_membrane", "prismarine_shard", "prismarine_crystals", "amethyst_shard", "netherite_scrap", "nether_star",
			"shulker_shell", "turtle_scute", "armadillo_scute", "breeze_rod", "echo_shard", "disc_fragment_5", "honeycomb",
			"coal", "charcoal", "lapis_lazuli", "quartz", "redstone", "glowstone_dust", "copper_nugget");

	private static final Set<String> FOOD_TAGS = Set.of(
			"c:foods", "c:crops", "c:seeds", "c:drinks", "minecraft:villager_plantable_seeds", "minecraft:meat",
			"minecraft:fishes", "minecraft:eggs");
	private static final Set<String> FOOD_ITEMS = Set.of(
			"wheat", "sugar", "nether_wart", "blaze_powder", "glass_bottle", "fermented_spider_eye", "ghast_tear",
			"magma_cream", "water_bucket", "milk_bucket", "honey_bottle", "dragon_breath", "experience_bottle", "bowl");

	private static final Set<String> MISC_TAGS = Set.of(
			"c:redstone_dusts", "minecraft:rails", "minecraft:boats", "minecraft:chest_boats", "minecraft:creeper_drop_music_discs");
	private static final Set<String> MISC_ITEMS = Set.of(
			"redstone", "repeater", "comparator", "piston", "sticky_piston", "observer", "hopper", "dropper", "dispenser",
			"lever", "redstone_torch", "redstone_lamp", "redstone_block", "target", "tnt", "daylight_detector", "tripwire_hook",
			"note_block", "slime_block", "honey_block", "lightning_rod", "sculk_sensor", "calibrated_sculk_sensor", "crafter",
			"minecart", "chest_minecart", "furnace_minecart", "hopper_minecart", "tnt_minecart", "command_block_minecart");

	private final String key;
	private final Item icon;

	ItemCategory(String key, Item icon) {
		this.key = key;
		this.icon = icon;
	}

	/** The translation key suffix for the tab's name. */
	public String key() {
		return this.key;
	}

	/** An item drawn as the tab's icon. */
	public Item icon() {
		return this.icon;
	}

	public boolean matches(Item item) {
		Identifier id = BuiltInRegistries.ITEM.getKey(item);
		ItemStack stack = new ItemStack(item);
		return switch (this) {
			case ALL -> true;
			case BLOCKS -> item instanceof BlockItem;
			case TOOLS -> stack.has(DataComponents.TOOL) || stack.has(DataComponents.WEAPON) || tagged(item, TOOL_TAGS)
					|| named(id, TOOL_ITEMS);
			case MATERIALS -> tagged(item, MATERIAL_TAGS) || named(id, MATERIAL_ITEMS);
			case FOOD -> stack.has(DataComponents.FOOD) || stack.has(DataComponents.POTION_CONTENTS) || tagged(item, FOOD_TAGS)
					|| named(id, FOOD_ITEMS);
			// Redstone and transport, plus anything that no other tab (besides All) would show.
			case MISC -> tagged(item, MISC_TAGS) || named(id, MISC_ITEMS)
					|| !(BLOCKS.matches(item) || TOOLS.matches(item) || MATERIALS.matches(item) || FOOD.matches(item));
		};
	}

	private static boolean named(Identifier id, Set<String> paths) {
		return id.getNamespace().equals("minecraft") && paths.contains(id.getPath());
	}

	private static boolean tagged(Item item, Set<String> tags) {
		var holder = BuiltInRegistries.ITEM.wrapAsHolder(item);
		for (String tag : tags) {
			if (holder.is(TagKey.create(Registries.ITEM, Identifier.parse(tag)))) {
				return true;
			}
		}
		return false;
	}
}
