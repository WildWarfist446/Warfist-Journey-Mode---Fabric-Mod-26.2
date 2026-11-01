package com.journeymode.research;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

import com.journeymode.JourneyMode;
import com.mojang.serialization.Codec;

import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** How much of each item every player has sacrificed, saved with the world. */
public final class ResearchData extends SavedData {
	private static final Codec<ResearchData> CODEC = Codec
			.unboundedMap(UUIDUtil.STRING_CODEC, Codec.unboundedMap(Identifier.CODEC, Codec.INT))
			.xmap(ResearchData::new, data -> data.progress);

	public static final SavedDataType<ResearchData> TYPE = new SavedDataType<>(
			JourneyMode.id("research"), ResearchData::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

	private final Map<UUID, Map<Identifier, Integer>> progress = new HashMap<>();

	public ResearchData() {
	}

	private ResearchData(Map<UUID, Map<Identifier, Integer>> loaded) {
		loaded.forEach((player, items) -> this.progress.put(player, new HashMap<>(items)));
	}

	public static ResearchData get(MinecraftServer server) {
		return server.getDataStorage().computeIfAbsent(TYPE);
	}

	public int progress(UUID player, Identifier item) {
		return this.progress.getOrDefault(player, Map.of()).getOrDefault(item, 0);
	}

	public boolean isResearched(UUID player, Identifier item, Item type) {
		return this.progress(player, item) >= ResearchCosts.required(type);
	}

	/** Adds sacrificed items to the player's progress, never past what the item needs. */
	public void add(UUID player, Identifier item, int amount, int required) {
		this.progress.computeIfAbsent(player, id -> new HashMap<>()).merge(item, amount, (a, b) -> Math.min(required, a + b));
		this.setDirty();
	}

	/** How many items the player has fully researched. */
	public int researchedCount(UUID player, Predicate<Identifier> isDone) {
		return (int) this.progress.getOrDefault(player, Map.of()).keySet().stream().filter(isDone).count();
	}
}