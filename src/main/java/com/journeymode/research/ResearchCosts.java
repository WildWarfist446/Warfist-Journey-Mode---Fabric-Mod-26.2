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

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * How many of an item must be sacrificed before it counts as researched.
 * <p>
 * Costs come from {@code config/journeymode-costs.json}, a JSON object whose keys are either an item
 * id ({@code "minecraft:ancient_debris"}) or an item tag starting with {@code #} ({@code "#c:ingots"}),
 * and whose values are the number of items needed. An item's own entry wins over a tag entry; when
 * several tags match, the first one in the file wins. Items that do not stack always cost 1, and
 * anything not listed costs a full stack.
 * <p>
 * The server's rules are sent to each player on join (see {@code CostConfigPayload}), so the client
 * shows the same costs the server enforces.
 */
public final class ResearchCosts {
	private static final String FILE_NAME = "journeymode-costs.json";

	/** Written when no config exists yet. Tags are the usual convention tags that most mods add their items to. */
	private static final String DEFAULT_CONFIG = """
			{
			  "#c:ingots": 25,
			  "#c:raw_materials": 25,
			  "#c:ores": 25,
			  "#c:gems": 25,
			  "minecraft:coal": 25,
			  "minecraft:ancient_debris": 25,
			  "minecraft:netherite_scrap": 25,
			  "minecraft:heart_of_the_sea": 1,
			  "minecraft:nether_star": 1,
			  "minecraft:dragon_egg": 1,
			  "minecraft:dragon_head": 1,
			  "minecraft:beacon": 1,
			  "minecraft:conduit": 1,
			  "minecraft:enchanted_golden_apple": 1,
			  "minecraft:echo_shard": 1,
			  "minecraft:nautilus_shell": 1,
			  "minecraft:shulker_shell": 1,
			  "minecraft:wither_skeleton_skull": 1,
			  "minecraft:sniffer_egg": 1,
			  "minecraft:trial_key": 1,
			  "minecraft:ominous_trial_key": 1,
			  "minecraft:heavy_core": 1,
			  "minecraft:spawner": 1,
			  "minecraft:trial_spawner": 1,
			  "minecraft:vault": 1,
			  "minecraft:command_block": 1,
			  "minecraft:netherite_upgrade_smithing_template": 1,
			  "minecraft:skeleton_skull": 1,
			  "minecraft:zombie_head": 1,
			  "minecraft:creeper_head": 1,
			  "minecraft:piglin_head": 1,
			  "minecraft:player_head": 1,
			  "minecraft:sentry_armor_trim_smithing_template": 1,
			  "minecraft:dune_armor_trim_smithing_template": 1,
			  "minecraft:coast_armor_trim_smithing_template": 1,
			  "minecraft:wild_armor_trim_smithing_template": 1,
			  "minecraft:ward_armor_trim_smithing_template": 1,
			  "minecraft:eye_armor_trim_smithing_template": 1,
			  "minecraft:vex_armor_trim_smithing_template": 1,
			  "minecraft:tide_armor_trim_smithing_template": 1,
			  "minecraft:snout_armor_trim_smithing_template": 1,
			  "minecraft:rib_armor_trim_smithing_template": 1,
			  "minecraft:spire_armor_trim_smithing_template": 1,
			  "minecraft:wayfinder_armor_trim_smithing_template": 1,
			  "minecraft:shaper_armor_trim_smithing_template": 1,
			  "minecraft:silence_armor_trim_smithing_template": 1,
			  "minecraft:raiser_armor_trim_smithing_template": 1,
			  "minecraft:host_armor_trim_smithing_template": 1,
			  "minecraft:flow_armor_trim_smithing_template": 1,
			  "minecraft:bolt_armor_trim_smithing_template": 1
			}
			""";

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
			String idText = key.startsWith("#") ? key.substring(1) : key;
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
			parsed.put(key.startsWith("#") ? "#" + id : id.toString(), cost);
		}
		return parsed;
	}

	public static Map<String, Integer> rules() {
		return rules;
	}

	public static void setRules(Map<String, Integer> newRules) {
		rules = Collections.unmodifiableMap(new LinkedHashMap<>(newRules));
	}

	/** Items that do not stack cost 1; otherwise the item's own entry, then the first matching tag entry, then a full stack. */
	public static int required(Item item) {
		int stack = Math.max(1, new ItemStack(item).getMaxStackSize());
		if (stack == 1) {
			return 1;
		}
		Identifier id = BuiltInRegistries.ITEM.getKey(item);
		Integer own = rules.get(id.toString());
		if (own != null) {
			return own;
		}
		for (Map.Entry<String, Integer> rule : rules.entrySet()) {
			if (rule.getKey().startsWith("#")
					&& BuiltInRegistries.ITEM.wrapAsHolder(item).is(TagKey.create(Registries.ITEM, Identifier.parse(rule.getKey().substring(1))))) {
				return rule.getValue();
			}
		}
		return stack;
	}
}
