package com.journeymode.client;

import java.util.ArrayList;
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
import com.journeymode.research.ResearchKeys;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/** What the server last told this player about their research. */
public final class ResearchClientState {
	/** What the player has sacrificed, by research key (see {@link ResearchKeys}). */
	private static Map<String, Integer> progress = Map.of();
	private static int researchedCount;
	/** Everything with some research done, finished or not, in the game's item order. */
	private static List<ResearchEntry> listed = List.of();
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
			int researchedBefore = researchedCount;
			progress = Map.copyOf(payload.progress());

			// Each key becomes one thing in the list. Potions and enchanted books are made from their key, and
			// anything that no longer exists (a removed mod's item, say) is left out.
			List<ResearchEntry> entries = new ArrayList<>();
			if (context.client().level != null) {
				for (Map.Entry<String, Integer> saved : progress.entrySet()) {
					ItemStack stack = ResearchKeys.stackOf(saved.getKey(), context.client().level.registryAccess());
					if (!stack.isEmpty() && saved.getValue() > 0) {
						entries.add(new ResearchEntry(saved.getKey(), stack.getItem(), stack));
					}
				}
			}
			entries.sort(Comparator.<ResearchEntry>comparingInt(entry -> BuiltInRegistries.ITEM.getId(entry.item()))
					.thenComparing(ResearchEntry::key));
			listed = List.copyOf(entries);
			researchedCount = (int) listed.stream().filter(ResearchClientState::isResearched).count();

			// The first sync after joining just loads saved progress. Any later one that adds to it
			// means items were just sacrificed: a celebration if that finished an item, a pop if not.
			boolean added = synced && total(progress) > before;
			synced = true;
			if (added) {
				if (researchedCount > researchedBefore) {
					ResearchSounds.playCompleted();
				} else {
					ResearchSounds.playProgress();
				}
			}
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			progress = Map.of();
			researchedCount = 0;
			listed = List.of();
			synced = false;
			journeyEnabled = false;
			godMode = false;
			mobSpawns = 1;
			trash = ItemStack.EMPTY;
			ResearchSounds.reset();
		});
	}

	private static int total(Map<String, Integer> progress) {
		return progress.values().stream().mapToInt(Integer::intValue).sum();
	}

	/** How much of this item (this kind of it, for potions and books) has been sacrificed. */
	public static int progress(ItemStack stack) {
		return progress.getOrDefault(ResearchKeys.of(stack), 0);
	}

	public static ItemStack trash() {
		return trash;
	}

	public static boolean isResearched(ResearchEntry entry) {
		return progress.getOrDefault(entry.key(), 0) >= ResearchCosts.required(entry.item());
	}

	/** Everything with some research done, finished or not, in the game's item order. */
	public static List<ResearchEntry> listedItems() {
		return listed;
	}

	/** How many more must be sacrificed to finish researching it, or 0 if it is done. */
	public static int remaining(ResearchEntry entry) {
		return Math.max(0, ResearchCosts.required(entry.item()) - progress.getOrDefault(entry.key(), 0));
	}
}
