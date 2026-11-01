package com.journeymode.research;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.mojang.serialization.Codec;

import com.journeymode.JourneyMode;

import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** The players who have God Mode switched on, saved with the world so it stays on between sessions. */
public final class GodModeData extends SavedData {
	private static final Codec<GodModeData> CODEC = Codec.list(UUIDUtil.STRING_CODEC)
			.xmap(GodModeData::new, data -> List.copyOf(data.players));

	public static final SavedDataType<GodModeData> TYPE = new SavedDataType<>(
			JourneyMode.id("god_mode"), GodModeData::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

	private final Set<UUID> players = new HashSet<>();

	public GodModeData() {
	}

	private GodModeData(List<UUID> loaded) {
		this.players.addAll(loaded);
	}

	public static GodModeData get(MinecraftServer server) {
		return server.getDataStorage().computeIfAbsent(TYPE);
	}

	public boolean isOn(UUID player) {
		return this.players.contains(player);
	}

	/** Flips the player's setting and returns the new one. */
	public boolean toggle(UUID player) {
		boolean on = this.players.add(player);
		if (!on) {
			this.players.remove(player);
		}
		this.setDirty();
		return on;
	}
}
