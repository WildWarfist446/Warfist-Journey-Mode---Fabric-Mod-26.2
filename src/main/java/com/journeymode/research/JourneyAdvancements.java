package com.journeymode.research;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.journeymode.JourneyMode;

import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Journey Mode's advancements (data/journeymode/advancement). They use the "impossible" trigger,
 * so nothing happens on its own: the mod awards each one from code when its moment comes.
 * <p>
 * The five "research everything" advancements count every vanilla item in that item list tab that a
 * survival player can get: the items in the creative inventory's tabs, leaving out the operator
 * utilities (command blocks and so on) and spawn eggs.
 */
public final class JourneyAdvancements {
	/** The name of the one criterion every Journey Mode advancement has. */
	private static final String CRITERION = "done";

	/** The advancement for researching everything in each item list tab. */
	private static final Map<ItemCategory, String> COMPLETION = Map.of(
			ItemCategory.BLOCKS, "block_head",
			ItemCategory.TOOLS, "hardware_heavy",
			ItemCategory.MATERIALS, "hardcore_crafter",
			ItemCategory.FOOD, "food_o_plenty",
			ItemCategory.MISC, "everything_else");

	/**
	 * Vanilla items that show up in the creative tabs but that survival players cannot get, so they
	 * must not count. Items whose names start with one of {@link #EXCLUDED_PREFIXES} are left out too.
	 */
	private static final Set<String> EXCLUDED = Set.of(
			"player_head", "mojang_banner_pattern", "frogspawn", "chorus_plant", "spawner", "trial_spawner");
	private static final List<String> EXCLUDED_PREFIXES = List.of("infested_");

	/** The vanilla items a survival player can get; worked out the first time it is needed. */
	private static List<Item> obtainable;

	private JourneyAdvancements() {
	}

	public static void award(ServerPlayer player, String name) {
		AdvancementHolder advancement = player.level().getServer().getAdvancements().get(JourneyMode.id(name));
		if (advancement != null) {
			player.getAdvancements().award(advancement, CRITERION);
		}
	}

	private static boolean isDone(ServerPlayer player, String name) {
		AdvancementHolder advancement = player.level().getServer().getAdvancements().get(JourneyMode.id(name));
		return advancement == null || player.getAdvancements().getOrStartProgress(advancement).isDone();
	}

	/** A player joined a world with Journey Mode on: welcome them, and catch up on anything they already researched. */
	public static void onJoin(ServerPlayer player) {
		award(player, "welcome");
		ResearchData data = ResearchData.get(player.level().getServer());
		logProgress(player, data);
		if (!data.all(player.getUUID()).isEmpty()) {
			for (Map.Entry<Identifier, Integer> entry : data.all(player.getUUID()).entrySet()) {
				Item item = BuiltInRegistries.ITEM.getOptional(entry.getKey()).orElse(null);
				if (item != null && entry.getValue() >= ResearchCosts.required(item)) {
					award(player, "researcher");
					break;
				}
			}
		}
		checkCompletion(player, null);
	}

	/** Writes, to the debug log, how many items the player has researched out of those each advancement counts. */
	private static void logProgress(ServerPlayer player, ResearchData data) {
		for (ItemCategory category : COMPLETION.keySet()) {
			long total = obtainable(player.level().getServer()).stream().filter(category::matches).count();
			long done = obtainable(player.level().getServer()).stream().filter(category::matches)
					.filter(item -> data.progress(player.getUUID(), BuiltInRegistries.ITEM.getKey(item)) >= ResearchCosts.required(item)).count();
			JourneyMode.LOGGER.debug("{} has researched {} of {} items for \"{}\"", player.getName().getString(), done, total, COMPLETION.get(category));
		}
	}

	/** An item has just been fully researched. */
	public static void onResearched(ServerPlayer player, Item item) {
		award(player, "researcher");
		checkCompletion(player, item);
	}

	/**
	 * Awards the "research everything" advancements the player has now earned. With an item given,
	 * only the tabs it belongs to are checked, since nothing else has changed.
	 */
	private static void checkCompletion(ServerPlayer player, Item justResearched) {
		MinecraftServer server = player.level().getServer();
		ResearchData data = ResearchData.get(server);
		for (Map.Entry<ItemCategory, String> entry : COMPLETION.entrySet()) {
			ItemCategory category = entry.getKey();
			if (isDone(player, entry.getValue()) || (justResearched != null && !category.matches(justResearched))) {
				continue;
			}
			boolean all = true;
			for (Item item : obtainable(server)) {
				if (category.matches(item) && data.progress(player.getUUID(), BuiltInRegistries.ITEM.getKey(item)) < ResearchCosts.required(item)) {
					all = false;
					break;
				}
			}
			if (all) {
				award(player, entry.getValue());
			}
		}
	}

	private static synchronized List<Item> obtainable(MinecraftServer server) {
		if (obtainable == null) {
			CreativeModeTabs.tryRebuildTabContents(server.getWorldData().enabledFeatures(), false, server.registryAccess());
			Set<Item> items = new LinkedHashSet<>();
			for (CreativeModeTab tab : CreativeModeTabs.allTabs()) {
				String name = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab).getPath();
				if (tab.getType() != CreativeModeTab.Type.CATEGORY || name.equals("op_blocks") || name.equals("spawn_eggs")) {
					continue;
				}
				for (ItemStack stack : tab.getDisplayItems()) {
					Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
					if (id.getNamespace().equals("minecraft") && !EXCLUDED.contains(id.getPath())
							&& EXCLUDED_PREFIXES.stream().noneMatch(id.getPath()::startsWith)) {
						items.add(stack.getItem());
					}
				}
			}
			obtainable = new ArrayList<>(items);
			logCounted(obtainable);
		}
		return obtainable;
	}

	/**
	 * Logs how many items each "research everything" advancement counts, and writes every one of them
	 * to logs/journeymode-counted-items.txt, so anything that cannot be obtained in survival can be spotted.
	 */
	private static void logCounted(List<Item> items) {
		List<String> lines = new ArrayList<>();
		for (ItemCategory category : COMPLETION.keySet()) {
			List<String> names = items.stream().filter(category::matches)
					.map(item -> BuiltInRegistries.ITEM.getKey(item).getPath()).sorted().toList();
			JourneyMode.LOGGER.info("{} counts {} items for \"{}\"", category.key(), names.size(), COMPLETION.get(category));
			lines.add("== " + category.key() + " (" + COMPLETION.get(category) + "): " + names.size() + " items");
			lines.addAll(names);
			lines.add("");
		}
		Path file = FabricLoader.getInstance().getGameDir().resolve("logs").resolve("journeymode-counted-items.txt");
		try {
			Files.createDirectories(file.getParent());
			Files.write(file, lines);
		} catch (IOException e) {
			JourneyMode.LOGGER.warn("Could not write {}", file, e);
		}
	}
}
