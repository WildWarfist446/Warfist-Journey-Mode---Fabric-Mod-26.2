package com.journeymode.client;

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

	private ResearchClientState() {
	}

	public static void initialize() {
		ClientPlayNetworking.registerGlobalReceiver(ResearchSyncPayload.TYPE, (payload, context) -> progress = Map.copyOf(payload.progress()));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> progress = Map.of());
	}

	public static int progress(Item item) {
		return progress.getOrDefault(BuiltInRegistries.ITEM.getKey(item), 0);
	}

	public static boolean isResearched(Item item) {
		return progress(item) >= ResearchCosts.required(item);
	}

	/** How many different items are fully researched. */
	public static int researchedTotal() {
		return (int) progress.entrySet().stream()
				.filter(entry -> BuiltInRegistries.ITEM.getOptional(entry.getKey())
						.map(item -> entry.getValue() >= ResearchCosts.required(item)).orElse(false))
				.count();
	}
}