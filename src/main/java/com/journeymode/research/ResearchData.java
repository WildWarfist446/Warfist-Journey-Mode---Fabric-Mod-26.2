package com.journeymode.research;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.journeymode.JourneyMode;
import com.mojang.serialization.Codec;

import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** How much of each item every player has sacrificed, saved with the world. */
public final class ResearchData extends SavedData {
	private static final Codec<ResearchData> CODEC = Codec
			.unboundedMap(UUIDUtil.STRING_CODEC, Codec.unboundedMap(Codec.STRING, Codec.INT))
			.xmap(ResearchData::new, data -> data.progress);

	public static final SavedDataType<ResearchData> TYPE = new SavedDataType<>(
			JourneyMode.id("research"), ResearchData::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

	/** What each player has sacrificed, by research key (see {@link ResearchKeys}). */
	private final Map<UUID, Map<String, Integer>> progress = new HashMap<>();

	public ResearchData() {
	}

	private ResearchData(Map<UUID, Map<String, Integer>> loaded) {
		loaded.forEach((player, items) -> this.progress.put(player, new HashMap<>(items)));
	}

	public static ResearchData get(MinecraftServer server) {
		return server.getDataStorage().computeIfAbsent(TYPE);
	}

	public int progress(UUID player, String key) {
		return this.progress.getOrDefault(player, Map.of()).getOrDefault(key, 0);
	}

	/** A copy of everything the player has sacrificed so far. */
	public Map<String, Integer> all(UUID player) {
		return new HashMap<>(this.progress.getOrDefault(player, Map.of()));
	}

	/** Adds sacrificed items to the player's progress, never past what the item needs. */
	public void add(UUID player, String key, int amount, int required) {
		this.progress.computeIfAbsent(player, id -> new HashMap<>()).merge(key, amount, (a, b) -> Math.min(required, a + b));
		this.setDirty();
	}
}