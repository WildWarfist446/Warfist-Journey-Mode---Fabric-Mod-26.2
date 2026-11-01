package com.journeymode.client;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.journeymode.network.ResearchSyncPayload;
import com.journeymode.research.ResearchCosts;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

/** What the server last told this player about their research. */
public final class ResearchClientState {
	private static Map<Identifier, Integer> progress = Map.of();
	private static List<Item> researched = List.of();

	private ResearchClientState() {
	}

	public static void initialize() {
		ClientPlayNetworking.registerGlobalReceiver(ResearchSyncPayload.TYPE, (payload, context) -> {
			progress = Map.copyOf(payload.progress());
			researched = progress.entrySet().stream()
					.map(entry -> BuiltInRegistries.ITEM.getOptional(entry.getKey()).filter(item -> entry.getValue() >= ResearchCosts.required(item)))
					.flatMap(java.util.Optional::stream)
					.sorted(Comparator.comparingInt(BuiltInRegistries.ITEM::getId))
					.toList();
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			progress = Map.of();
			researched = List.of();
		});
	}

	public static int progress(Item item) {
		return progress.getOrDefault(BuiltInRegistries.ITEM.getKey(item), 0);
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