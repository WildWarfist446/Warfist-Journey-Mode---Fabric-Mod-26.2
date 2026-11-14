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

	private static final String[] CREATE_ONE = {
			"adjustable_chain_gearshift",
			"analog_lever",
			"andesite_table_cloth",
			"attribute_filter",
			"basin",
			"black_postbox",
			"black_seat",
			"black_table_cloth",
			"black_toolbox",
			"black_valve_handle",
			"blaze_burner",
			"blaze_cake",
			"blaze_cake_base",
			"blue_postbox",
			"blue_seat",
			"blue_table_cloth",
			"blue_toolbox",
			"blue_valve_handle",
			"brass_hand",
			"brass_table_cloth",
			"brown_postbox",
			"brown_seat",
			"brown_table_cloth",
			"brown_toolbox",
			"brown_valve_handle",
			"cardboard_boots",
			"cardboard_chestplate",
			"cardboard_helmet",
			"cardboard_leggings",
			"cardboard_package_10x12",
			"cardboard_package_10x8",
			"cardboard_package_12x10",
			"cardboard_package_12x12",
			"cardboard_sword",
			"cart_assembler",
			"chain_conveyor",
			"chest_minecart_contraption",
			"chocolate_bucket",
			"clipboard",
			"clockwork_bearing",
			"clutch",
			"content_observer",
			"contraption_controls",
			"controls",
			"copper_backtank",
			"copper_diving_boots",
			"copper_diving_helmet",
			"copper_table_cloth",
			"copper_valve_handle",
			"copycat_panel",
			"copycat_step",
			"crafter_slot_cover",
			"crafting_blueprint",
			"creative_blaze_cake",
			"creative_crate",
			"creative_fluid_tank",
			"creative_motor",
			"crushing_wheel",
			"cuckoo_clock",
			"cyan_postbox",
			"cyan_seat",
			"cyan_table_cloth",
			"cyan_toolbox",
			"cyan_valve_handle",
			"deployer",
			"depot",
			"desk_bell",
			"display_board",
			"display_link",
			"electron_tube",
			"elevator_contact",
			"elevator_pulley",
			"empty_blaze_burner",
			"empty_schematic",
			"encased_chain_drive",
			"encased_fan",
			"experience_block",
			"extendo_grip",
			"factory_gauge",
			"filter",
			"fluid_tank",
			"fluid_valve",
			"flywheel",
			"furnace_minecart_contraption",
			"gantry_carriage",
			"gantry_shaft",
			"gearbox",
			"gearshift",
			"goggles",
			"gray_postbox",
			"gray_seat",
			"gray_table_cloth",
			"gray_toolbox",
			"gray_valve_handle",
			"green_postbox",
			"green_seat",
			"green_table_cloth",
			"green_toolbox",
			"green_valve_handle",
			"hand_crank",
			"handheld_worldshaper",
			"haunted_bell",
			"honey_bucket",
			"hose_pulley",
			"incomplete_precision_mechanism",
			"incomplete_track",
			"item_drain",
			"item_hatch",
			"item_vault",
			"large_water_wheel",
			"light_blue_postbox",
			"light_blue_seat",
			"light_blue_table_cloth",
			"light_blue_toolbox",
			"light_blue_valve_handle",
			"light_gray_postbox",
			"light_gray_seat",
			"light_gray_table_cloth",
			"light_gray_toolbox",
			"light_gray_valve_handle",
			"lime_postbox",
			"lime_seat",
			"lime_table_cloth",
			"lime_toolbox",
			"lime_valve_handle",
			"linear_chassis",
			"linked_controller",
			"magenta_postbox",
			"magenta_seat",
			"magenta_table_cloth",
			"magenta_toolbox",
			"magenta_valve_handle",
			"mechanical_arm",
			"mechanical_bearing",
			"mechanical_crafter",
			"mechanical_drill",
			"mechanical_harvester",
			"mechanical_mixer",
			"mechanical_piston",
			"mechanical_plough",
			"mechanical_press",
			"mechanical_pump",
			"mechanical_roller",
			"mechanical_saw",
			"millstone",
			"minecart_contraption",
			"minecart_coupling",
			"mysterious_cuckoo_clock",
			"netherite_backtank",
			"netherite_diving_boots",
			"netherite_diving_helmet",
			"nixie_tube",
			"nozzle",
			"orange_postbox",
			"orange_seat",
			"orange_table_cloth",
			"orange_toolbox",
			"orange_valve_handle",
			"package_filter",
			"package_frogport",
			"packager",
			"peculiar_bell",
			"pink_postbox",
			"pink_seat",
			"pink_table_cloth",
			"pink_toolbox",
			"pink_valve_handle",
			"portable_fluid_interface",
			"portable_storage_interface",
			"potato_cannon",
			"powered_latch",
			"powered_toggle_latch",
			"precision_mechanism",
			"pulse_extender",
			"pulse_repeater",
			"pulse_timer",
			"purple_postbox",
			"purple_seat",
			"purple_table_cloth",
			"purple_toolbox",
			"purple_valve_handle",
			"radial_chassis",
			"railway_casing",
			"rare_creeper_package",
			"rare_darcy_package",
			"rare_evan_package",
			"rare_jinx_package",
			"rare_kryppers_package",
			"rare_simi_package",
			"rare_starlotte_package",
			"rare_thunder_package",
			"rare_up_package",
			"rare_vector_package",
			"red_postbox",
			"red_seat",
			"red_table_cloth",
			"red_toolbox",
			"red_valve_handle",
			"redstone_contact",
			"redstone_link",
			"redstone_requester",
			"refined_radiance_casing",
			"repackager",
			"rope_pulley",
			"rotation_speed_controller",
			"schedule",
			"schematic",
			"schematic_and_quill",
			"schematic_table",
			"schematicannon",
			"secondary_linear_chassis",
			"sequenced_gearshift",
			"shadow_steel_casing",
			"shopping_list",
			"smart_chute",
			"smart_fluid_pipe",
			"speedometer",
			"spout",
			"steam_engine",
			"steam_whistle",
			"sticker",
			"sticky_mechanical_piston",
			"stock_link",
			"stock_ticker",
			"stockpile_switch",
			"stressometer",
			"sturdy_sheet",
			"super_glue",
			"track_observer",
			"track_signal",
			"track_station",
			"train_door",
			"train_trapdoor",
			"transmitter",
			"turntable",
			"unprocessed_obsidian_sheet",
			"vertical_gearbox",
			"wand_of_symmetry",
			"water_wheel",
			"weighted_ejector",
			"white_postbox",
			"white_seat",
			"white_table_cloth",
			"white_toolbox",
			"white_valve_handle",
			"windmill_bearing",
			"wrench",
			"yellow_postbox",
			"yellow_seat",
			"yellow_table_cloth",
			"yellow_toolbox",
			"yellow_valve_handle"
	};

	private static final String[] CREATE_SIXTEEN = {
			"acacia_window",
			"acacia_window_pane",
			"andesite_bars",
			"andesite_casing",
			"andesite_door",
			"andesite_encased_shaft",
			"andesite_funnel",
			"andesite_ladder",
			"andesite_scaffolding",
			"andesite_tunnel",
			"bamboo_window",
			"bamboo_window_pane",
			"belt_connector",
			"birch_window",
			"birch_window_pane",
			"bound_cardboard_block",
			"brass_bars",
			"brass_casing",
			"brass_door",
			"brass_encased_shaft",
			"brass_funnel",
			"brass_ladder",
			"brass_scaffolding",
			"brass_sheet",
			"brass_tunnel",
			"cardboard",
			"cardboard_block",
			"cherry_window",
			"cherry_window_pane",
			"chute",
			"cogwheel",
			"controller_rail",
			"copper_bars",
			"copper_casing",
			"copper_door",
			"copper_ladder",
			"copper_scaffolding",
			"copper_sheet",
			"crimson_window",
			"crimson_window_pane",
			"dark_oak_window",
			"dark_oak_window_pane",
			"fluid_pipe",
			"framed_glass",
			"framed_glass_door",
			"framed_glass_pane",
			"framed_glass_trapdoor",
			"golden_sheet",
			"horizontal_framed_glass",
			"horizontal_framed_glass_pane",
			"industrial_iron_block",
			"industrial_iron_window",
			"industrial_iron_window_pane",
			"iron_sheet",
			"jungle_window",
			"jungle_window_pane",
			"large_cogwheel",
			"mangrove_window",
			"mangrove_window_pane",
			"metal_bracket",
			"metal_girder",
			"oak_window",
			"oak_window_pane",
			"ornate_iron_window",
			"ornate_iron_window_pane",
			"piston_extension_pole",
			"polished_rose_quartz",
			"rose_quartz_block",
			"rose_quartz_lamp",
			"rose_quartz_tiles",
			"sail_frame",
			"shaft",
			"small_rose_quartz_tiles",
			"spruce_window",
			"spruce_window_pane",
			"tiled_glass",
			"tiled_glass_pane",
			"track",
			"vertical_framed_glass",
			"vertical_framed_glass_pane",
			"warped_window",
			"warped_window_pane",
			"weathered_iron_block",
			"weathered_iron_window",
			"weathered_iron_window_pane",
			"white_sail",
			"wooden_bracket"
	};

	private static final String[] CREATE_TWENTY_FIVE = {
			"andesite_alloy",
			"andesite_alloy_block",
			"brass_block",
			"crushed_raw_aluminum",
			"crushed_raw_copper",
			"crushed_raw_gold",
			"crushed_raw_iron",
			"crushed_raw_lead",
			"crushed_raw_nickel",
			"crushed_raw_osmium",
			"crushed_raw_platinum",
			"crushed_raw_quicksilver",
			"crushed_raw_silver",
			"crushed_raw_tin",
			"crushed_raw_uranium",
			"crushed_raw_zinc",
			"raw_zinc_block",
			"rose_quartz",
			"zinc_block"
	};

	/** The natural stones Create adds, each with the pieces listed in {@link #CREATE_STONE_FORMS}. */
	private static final String[] CREATE_STONES = {"andesite", "asurine", "calcite", "crimsite", "deepslate", "diorite", "dripstone", "granite", "limestone", "ochrum", "scorchia", "scoria", "tuff", "veridium"};
	private static final String[] CREATE_STONE_FORMS = {
			"%s", "%s_pillar", "layered_%s", "cut_%s", "cut_%s_slab", "cut_%s_stairs", "cut_%s_wall",
			"polished_cut_%s", "polished_cut_%s_slab", "polished_cut_%s_stairs", "polished_cut_%s_wall",
			"cut_%s_bricks", "cut_%s_brick_slab", "cut_%s_brick_stairs", "cut_%s_brick_wall",
			"small_%s_bricks", "small_%s_brick_slab", "small_%s_brick_stairs", "small_%s_brick_wall"};

	/** The weathering stages of Create's copper shingles and tiles, with and without wax. */
	private static final String[] CREATE_COPPER_AGES = {
			"", "exposed_", "weathered_", "oxidized_", "waxed_", "waxed_exposed_", "waxed_weathered_", "waxed_oxidized_"};
	private static final String[] CREATE_COPPER_FORMS = {
			"copper_shingles", "copper_shingle_slab", "copper_shingle_stairs", "copper_tiles", "copper_tile_slab", "copper_tile_stairs"};

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
		// Obsidian: what the smallest Nether portal takes (a 4 by 5 frame without its corners).
		m.put("minecraft:obsidian", 10);
		// Spell Engine and Wizards: spell scrolls, books and the spell binding table.
		m.put("spell_engine:spell_scroll", 1);
		m.put("spell_engine:spell_book", 1);
		m.put("spell_engine:spell_binding", 1);
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
		m.put("minecraft:beehive", 1);
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
		// Create (Create Fly): the machines, mechanisms and other hard-to-make items, one each. Items that come in the 16 dye colours
		// (seats, toolboxes, postboxes, valve handles, table cloths) are one each too, so no colour is researched on its own.
		for (String path : CREATE_ONE) {
			m.put("create:" + path, 1);
		}
		// Plain building parts and common metal sheets: a quarter of a stack.
		for (String path : CREATE_SIXTEEN) {
			m.put("create:" + path, 16);
		}
		// Raw and crushed materials, alloys and metal blocks.
		for (String path : CREATE_TWENTY_FIVE) {
			m.put("create:" + path, 25);
		}
		// The decorative stone families (cut, polished, bricks, layered, pillars, slabs, stairs and walls): half a stack.
		for (String stone : CREATE_STONES) {
			for (String form : CREATE_STONE_FORMS) {
				m.put("create:" + form.replace("%s", stone), 32);
			}
		}
		// The copper shingles and tiles in every weathering: a quarter of a stack.
		for (String age : CREATE_COPPER_AGES) {
			for (String form : CREATE_COPPER_FORMS) {
				m.put("create:" + age + form, 16);
			}
		}
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
