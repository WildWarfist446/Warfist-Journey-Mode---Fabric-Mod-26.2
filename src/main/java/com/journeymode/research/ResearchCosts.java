package com.journeymode.research;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.journeymode.JourneyMode;

import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * How many of an item must be sacrificed before it counts as researched.
 * <p>
 * The mod has its own balance built in ({@link #BUILT_IN}), and {@code config/journeymode-costs.json}
 * is applied on top of it. The file is a JSON object whose keys are either an item id
 * ({@code "minecraft:ancient_debris"}) or an item tag starting with {@code #} ({@code "#c:ingots"}),
 * and whose values are the number of items needed. An item's own entry wins over a tag entry; when
 * several tags match, the first one in the file wins. Items that do not stack always cost 1, and
 * anything not listed costs a full stack.
 * <p>
 * The server's rules are sent to each player on join (see {@code CostConfigPayload}), so the client
 * shows the same costs the server enforces.
 */
public final class ResearchCosts {
	private static final String FILE_NAME = "journeymode-costs.json";

	/** Written when no config exists yet. Anything listed in it changes the built-in costs. */
	private static final String DEFAULT_CONFIG = "{}\n";

	/** The pseudo tag in the config (and in {@link #BUILT_IN}) that matches every item that can be eaten. */
	private static final String FOOD_KEY = "#journeymode:food";

	/**
	 * The mod's own balance. The config file is applied on top of it: an entry there beats a built-in one
	 * that matches the same way, and items it does not mention keep the built-in cost. A built-in cost is
	 * never more than one full stack of the item (so food that stacks to 16 costs 16, not 32).
	 */
	private static final Map<String, Integer> BUILT_IN = builtIn();

	private static Map<String, Integer> builtIn() {
		Map<String, Integer> m = new LinkedHashMap<>();
		// Every item from The Crown of Chaos: one is enough. This comes first so nothing below can raise the cost.
		m.put("crownofchaos:*", 1);
		// Metals, ores and gems.
		m.put("#c:ingots", 25);
		m.put("#c:raw_materials", 25);
		m.put("#c:ores", 25);
		m.put("#c:gems", 25);
		m.put("minecraft:coal", 25);
		m.put("minecraft:ancient_debris", 25);
		m.put("minecraft:netherite_scrap", 25);
		// Rare items, mob heads and armor trim templates: one is enough.
		m.put("minecraft:heart_of_the_sea", 1);
		m.put("minecraft:nether_star", 1);
		m.put("minecraft:dragon_egg", 1);
		m.put("minecraft:dragon_head", 1);
		m.put("minecraft:beacon", 1);
		m.put("minecraft:conduit", 1);
		m.put("minecraft:enchanted_golden_apple", 1);
		m.put("minecraft:echo_shard", 1);
		m.put("minecraft:ominous_bottle", 1);
		m.put("minecraft:tipped_arrow", 1);
		// Blocks that make sound: one is enough.
		m.put("minecraft:jukebox", 1);
		m.put("minecraft:note_block", 1);
		m.put("minecraft:bell", 1);
		// Utility items that act like tools, copper golem statues, cauldrons and Waystones (when that mod is installed): one is enough.
		m.put("minecraft:compass", 1);
		m.put("minecraft:recovery_compass", 1);
		m.put("minecraft:clock", 1);
		m.put("minecraft:map", 1);
		m.put("minecraft:filled_map", 1);
		m.put("minecraft:bucket", 1);
		m.put("minecraft:lead", 1);
		m.put("minecraft:name_tag", 1);
		m.put("minecraft:cauldron", 1);
		m.put("minecraft:copper_golem_statue", 1);
		m.put("minecraft:exposed_copper_golem_statue", 1);
		m.put("minecraft:weathered_copper_golem_statue", 1);
		m.put("minecraft:oxidized_copper_golem_statue", 1);
		m.put("minecraft:waxed_copper_golem_statue", 1);
		m.put("minecraft:waxed_exposed_copper_golem_statue", 1);
		m.put("minecraft:waxed_weathered_copper_golem_statue", 1);
		m.put("minecraft:waxed_oxidized_copper_golem_statue", 1);
		m.put("waystones:waystone", 1);
		m.put("waystones:andesite_waystone", 1);
		m.put("waystones:mossy_andesite_waystone", 1);
		m.put("waystones:sandstone_waystone", 1);
		m.put("waystones:deepslate_waystone", 1);
		m.put("waystones:blackstone_waystone", 1);
		m.put("waystones:end_stone_waystone", 1);
		m.put("waystones:mud_bricks_waystone", 1);
		m.put("waystones:prismarine_waystone", 1);
		m.put("waystones:purpur_waystone", 1);
		m.put("waystones:red_nether_bricks_waystone", 1);
		m.put("minecraft:nautilus_shell", 1);
		m.put("minecraft:shulker_shell", 1);
		m.put("minecraft:wither_skeleton_skull", 1);
		m.put("minecraft:sniffer_egg", 1);
		m.put("minecraft:trial_key", 1);
		m.put("minecraft:ominous_trial_key", 1);
		m.put("minecraft:heavy_core", 1);
		m.put("minecraft:spawner", 1);
		m.put("minecraft:trial_spawner", 1);
		m.put("minecraft:vault", 1);
		m.put("minecraft:command_block", 1);
		m.put("minecraft:netherite_upgrade_smithing_template", 1);
		m.put("minecraft:skeleton_skull", 1);
		m.put("minecraft:zombie_head", 1);
		m.put("minecraft:creeper_head", 1);
		m.put("minecraft:piglin_head", 1);
		m.put("minecraft:player_head", 1);
		m.put("minecraft:sentry_armor_trim_smithing_template", 1);
		m.put("minecraft:dune_armor_trim_smithing_template", 1);
		m.put("minecraft:coast_armor_trim_smithing_template", 1);
		m.put("minecraft:wild_armor_trim_smithing_template", 1);
		m.put("minecraft:ward_armor_trim_smithing_template", 1);
		m.put("minecraft:eye_armor_trim_smithing_template", 1);
		m.put("minecraft:vex_armor_trim_smithing_template", 1);
		m.put("minecraft:tide_armor_trim_smithing_template", 1);
		m.put("minecraft:snout_armor_trim_smithing_template", 1);
		m.put("minecraft:rib_armor_trim_smithing_template", 1);
		m.put("minecraft:spire_armor_trim_smithing_template", 1);
		m.put("minecraft:wayfinder_armor_trim_smithing_template", 1);
		m.put("minecraft:shaper_armor_trim_smithing_template", 1);
		m.put("minecraft:silence_armor_trim_smithing_template", 1);
		m.put("minecraft:raiser_armor_trim_smithing_template", 1);
		m.put("minecraft:host_armor_trim_smithing_template", 1);
		m.put("minecraft:flow_armor_trim_smithing_template", 1);
		m.put("minecraft:bolt_armor_trim_smithing_template", 1);
		// Anything used to craft, or that opens a menu to change or make blocks: one is enough. The mod's own tag lists the
		// vanilla ones; other mods' crafting tables and furnaces are caught by the common tags, or can be added to it.
		m.put("#journeymode:crafting_stations", 1);
		m.put("#c:player_workstations/crafting_tables", 1);
		m.put("#c:player_workstations/furnaces", 1);
		// Food: half a stack.
		m.put("#journeymode:food", 32);
		// Plants, crops, seeds and saplings: half a stack.
		m.put("#minecraft:saplings", 32);
		m.put("#minecraft:flowers", 32);
		m.put("#c:crops", 32);
		m.put("#c:seeds", 32);
		m.put("#minecraft:villager_plantable_seeds", 32);
		m.put("minecraft:wheat_seeds", 32);
		m.put("minecraft:beetroot_seeds", 32);
		m.put("minecraft:melon_seeds", 32);
		m.put("minecraft:pumpkin_seeds", 32);
		m.put("minecraft:torchflower_seeds", 32);
		m.put("minecraft:pitcher_pod", 32);
		m.put("minecraft:wheat", 32);
		m.put("minecraft:carrot", 32);
		m.put("minecraft:potato", 32);
		m.put("minecraft:beetroot", 32);
		m.put("minecraft:nether_wart", 32);
		m.put("minecraft:cocoa_beans", 32);
		m.put("minecraft:sugar_cane", 32);
		m.put("minecraft:bamboo", 32);
		m.put("minecraft:cactus", 32);
		m.put("minecraft:kelp", 32);
		m.put("minecraft:seagrass", 32);
		m.put("minecraft:sea_pickle", 32);
		m.put("minecraft:lily_pad", 32);
		m.put("minecraft:vine", 32);
		m.put("minecraft:short_grass", 32);
		m.put("minecraft:tall_grass", 32);
		m.put("minecraft:fern", 32);
		m.put("minecraft:large_fern", 32);
		m.put("minecraft:dead_bush", 32);
		m.put("minecraft:brown_mushroom", 32);
		m.put("minecraft:red_mushroom", 32);
		m.put("minecraft:crimson_fungus", 32);
		m.put("minecraft:warped_fungus", 32);
		m.put("minecraft:crimson_roots", 32);
		m.put("minecraft:warped_roots", 32);
		m.put("minecraft:nether_sprouts", 32);
		m.put("minecraft:weeping_vines", 32);
		m.put("minecraft:twisting_vines", 32);
		m.put("minecraft:glow_lichen", 32);
		m.put("minecraft:azalea", 32);
		m.put("minecraft:flowering_azalea", 32);
		m.put("minecraft:spore_blossom", 32);
		m.put("minecraft:big_dripleaf", 32);
		m.put("minecraft:small_dripleaf", 32);
		m.put("minecraft:hanging_roots", 32);
		m.put("minecraft:sweet_berries", 32);
		m.put("minecraft:glow_berries", 32);
		m.put("minecraft:chorus_flower", 32);
		m.put("minecraft:melon", 32);
		m.put("minecraft:pumpkin", 32);
		return Collections.unmodifiableMap(m);
	}

	/** Item ids and "#tag" ids, in file order, mapped to their cost. */
	private static Map<String, Integer> rules = Map.of();

	private ResearchCosts() {
	}

	/** Reads the config file, writing the default one first if it does not exist. */
	public static void load() {
		Path path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
		try {
			if (Files.notExists(path)) {
				try (Writer writer = Files.newBufferedWriter(path)) {
					writer.write(DEFAULT_CONFIG);
				}
			}
			try (Reader reader = Files.newBufferedReader(path)) {
				setRules(parse(JsonParser.parseReader(reader)));
			}
		} catch (IOException | RuntimeException e) {
			JourneyMode.LOGGER.error("Could not read {}, using full stack costs", path, e);
			setRules(Map.of());
		}
	}

	private static Map<String, Integer> parse(JsonElement json) {
		Map<String, Integer> parsed = new LinkedHashMap<>();
		if (!json.isJsonObject()) {
			JourneyMode.LOGGER.error("{} must be a JSON object", FILE_NAME);
			return parsed;
		}
		JsonObject object = json.getAsJsonObject();
		for (String key : object.keySet()) {
			// "modid:*" stands for every item from that mod.
			boolean wildcard = key.endsWith(":*");
			String idText = wildcard ? key.substring(0, key.length() - 1) + "all" : key.startsWith("#") ? key.substring(1) : key;
			Identifier id = Identifier.tryParse(idText);
			int cost;
			try {
				cost = object.get(key).getAsInt();
			} catch (RuntimeException e) {
				cost = 0;
			}
			if (id == null || cost < 1) {
				JourneyMode.LOGGER.warn("Ignoring invalid entry \"{}\" in {}: needs an id and a number of at least 1", key, FILE_NAME);
				continue;
			}
			parsed.put(wildcard ? id.getNamespace() + ":*" : key.startsWith("#") ? "#" + id : id.toString(), cost);
		}
		return parsed;
	}

	public static Map<String, Integer> rules() {
		return rules;
	}

	public static void setRules(Map<String, Integer> newRules) {
		rules = Collections.unmodifiableMap(new LinkedHashMap<>(newRules));
	}

	/**
	 * Items that do not stack cost 1. Otherwise, in order: the item's own entry in the config, its own
	 * built-in cost, the first matching tag entry in the config, the first matching built-in tag, and
	 * finally a full stack.
	 */
	public static int required(Item item) {
		int stack = Math.max(1, new ItemStack(item).getMaxStackSize());
		if (stack == 1) {
			return 1;
		}
		String id = BuiltInRegistries.ITEM.getKey(item).toString();
		Integer own = rules.get(id);
		if (own != null) {
			return own;
		}
		Integer builtInOwn = BUILT_IN.get(id);
		if (builtInOwn != null) {
			return Math.min(builtInOwn, stack);
		}
		Integer tagged = firstTagMatch(rules, item);
		if (tagged != null) {
			return tagged;
		}
		Integer builtInTagged = firstTagMatch(BUILT_IN, item);
		return builtInTagged != null ? Math.min(builtInTagged, stack) : stack;
	}

	/** The cost of the first "#tag" or "modid:*" rule, in order, that the item matches, or null. */
	private static Integer firstTagMatch(Map<String, Integer> candidates, Item item) {
		for (Map.Entry<String, Integer> rule : candidates.entrySet()) {
			String key = rule.getKey();
			if (key.endsWith(":*")) {
				if (BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(key.substring(0, key.length() - 2))) {
					return rule.getValue();
				}
			} else if (key.startsWith("#") && matchesTag(item, key.substring(1))) {
				return rule.getValue();
			}
		}
		return null;
	}

	private static boolean matchesTag(Item item, String tag) {
		if (("#" + tag).equals(FOOD_KEY)) {
			return new ItemStack(item).has(DataComponents.FOOD);
		}
		return BuiltInRegistries.ITEM.wrapAsHolder(item).is(TagKey.create(Registries.ITEM, Identifier.parse(tag)));
	}
}
