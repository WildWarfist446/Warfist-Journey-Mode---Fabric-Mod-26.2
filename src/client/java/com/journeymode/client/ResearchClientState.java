package com.journeymode.client;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.journeymode.network.CostConfigPayload;
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
	/** Whether the server has sent progress since this player joined. */
	private static boolean synced;
	/** The stack in this player's trash slot, as the server last told us. */
	private static ItemStack trash = ItemStack.EMPTY;

	private ResearchClientState() {
	}

	public static void initialize() {
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

	/** How many different items are fully researched. */
	public static int researchedTotal() {
		return researched.size();
	}
}