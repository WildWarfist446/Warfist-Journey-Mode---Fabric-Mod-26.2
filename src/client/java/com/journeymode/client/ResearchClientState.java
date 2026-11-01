package com.journeymode.client;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.journeymode.network.CostConfigPayload;
import com.journeymode.network.GodModeSyncPayload;
import com.journeymode.network.JourneyStatePayload;
import com.journeymode.network.MobSpawnSetPayload;
import com.journeymode.network.ResearchSyncPayload;
import com.journeymode.network.TrashSyncPayload;
import com.journeymode.research.ResearchCosts;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** What the server last told this player about their research. */
public final class ResearchClientState {
	private static Map<Identifier, Integer> progress = Map.of();
	private static List<Item> researched = List.of();
	/** The fully researched items and those that have been started but not finished, in the game's item order. */
	private static List<Item> listed = List.of();
	/** Whether the server has sent progress since this player joined. */
	private static boolean synced;
	private static boolean journeyEnabled;
	private static boolean godMode;
	private static int mobSpawns = 1;
	/** The stack in this player's trash slot, as the server last told us. */
	private static ItemStack trash = ItemStack.EMPTY;

	private ResearchClientState() {
	}

	/** Whether the world this player is in has Journey Mode on. */
	public static boolean journeyEnabled() {
		return journeyEnabled;
	}

	/** This player's Mob Spawns setting, from 1 (normal) to 10. */
	public static int mobSpawns() {
		return mobSpawns;
	}

	/** Whether God Mode is on for this player. */
	public static boolean godMode() {
		return godMode;
	}

	public static void initialize() {
		ClientPlayNetworking.registerGlobalReceiver(MobSpawnSetPayload.TYPE, (payload, context) -> mobSpawns = payload.multiplier());
		ClientPlayNetworking.registerGlobalReceiver(GodModeSyncPayload.TYPE, (payload, context) -> godMode = payload.on());
		ClientPlayNetworking.registerGlobalReceiver(JourneyStatePayload.TYPE, (payload, context) -> journeyEnabled = payload.enabled());
		ClientPlayNetworking.registerGlobalReceiver(TrashSyncPayload.TYPE, (payload, context) -> trash = payload.stack());
		ClientPlayNetworking.registerGlobalReceiver(CostConfigPayload.TYPE, (payload, context) ->
				ResearchCosts.setRules(payload.rules()));
		ClientPlayNetworking.registerGlobalReceiver(ResearchSyncPayload.TYPE, (payload, context) -> {
			int before = total(progress);
			int researchedBefore = researched.size();
			progress = Map.copyOf(payload.progress());
			researched = progress.entrySet().stream()
					.map(entry -> BuiltInRegistries.ITEM.getOptional(entry.getKey()).filter(item -> entry.getValue() >= ResearchCosts.required(item)))
					.flatMap(java.util.Optional::stream)
					.sorted(Comparator.comparingInt(BuiltInRegistries.ITEM::getId))
					.toList();

			// The first sync after joining just loads saved progress. Any later one that adds to it
			// means items were just sacrificed: a celebration if that finished an item, a pop if not.
			boolean added = synced && total(progress) > before;
			synced = true;
			if (added) {
				if (researched.size() > researchedBefore) {
					ResearchSounds.playCompleted();
				} else {
					ResearchSounds.playProgress();
				}
			}
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			progress = Map.of();
			researched = List.of();
			synced = false;
			journeyEnabled = false;
			godMode = false;
			mobSpawns = 1;
			trash = ItemStack.EMPTY;
			ResearchSounds.reset();
		});
	}

	private static int total(Map<Identifier, Integer> progress) {
		return progress.values().stream().mapToInt(Integer::intValue).sum();
	}

	public static int progress(Item item) {
		return progress.getOrDefault(BuiltInRegistries.ITEM.getKey(item), 0);
	}

	public static ItemStack trash() {
		return trash;
	}

	public static boolean isResearched(Item item) {
		return progress(item) >= ResearchCosts.required(item);
	}

	/** Every fully researched item, in the game's item order. */
	public static List<Item> researchedItems() {
		return researched;
	}

	/** Every item with some research done, finished or not, in the game's item order. */
	public static List<Item> listedItems() {
		return listed;
	}

	/** How many more of the item must be sacrificed to finish researching it, or 0 if it is done. */
	public static int remaining(Item item) {
		return Math.max(0, ResearchCosts.required(item) - progress(item));
	}

}