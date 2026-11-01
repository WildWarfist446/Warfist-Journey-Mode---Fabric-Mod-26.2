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

/**
 * Each player's Mob Spawns setting, saved with the world: 0 stops hostile mobs spawning around
 * them, 1 is normal, and up to 10 spawns more of them.
 */
public final class MobSpawnData extends SavedData {
	public static final int MIN = 0;
	public static final int NORMAL = 1;
	public static final int MAX = 10;

	private static final Codec<MobSpawnData> CODEC = Codec
			.unboundedMap(UUIDUtil.STRING_CODEC, Codec.INT)
			.xmap(MobSpawnData::new, data -> data.multipliers);

	public static final SavedDataType<MobSpawnData> TYPE = new SavedDataType<>(
			JourneyMode.id("mob_spawns"), MobSpawnData::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

	private final Map<UUID, Integer> multipliers = new HashMap<>();

	public MobSpawnData() {
	}

	private MobSpawnData(Map<UUID, Integer> loaded) {
		this.multipliers.putAll(loaded);
	}

	public static MobSpawnData get(MinecraftServer server) {
		return server.getDataStorage().computeIfAbsent(TYPE);
	}

	public int get(UUID player) {
		return Math.clamp(this.multipliers.getOrDefault(player, NORMAL), MIN, MAX);
	}

	public void set(UUID player, int multiplier) {
		int clamped = Math.clamp(multiplier, MIN, MAX);
		if (clamped == NORMAL) {
			this.multipliers.remove(player);
		} else {
			this.multipliers.put(player, clamped);
		}
		this.setDirty();
	}
}
