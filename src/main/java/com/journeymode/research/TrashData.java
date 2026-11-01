package com.journeymode.research;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.journeymode.JourneyMode;
import com.mojang.serialization.Codec;

import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** The stack sitting in each player's trash slot, saved with the world. It stays there until another item replaces it. */
public final class TrashData extends SavedData {
	private static final Codec<TrashData> CODEC = Codec
			.unboundedMap(UUIDUtil.STRING_CODEC, ItemStack.OPTIONAL_CODEC)
			.xmap(TrashData::new, data -> data.trash);

	public static final SavedDataType<TrashData> TYPE = new SavedDataType<>(
			JourneyMode.id("trash"), TrashData::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

	private final Map<UUID, ItemStack> trash = new HashMap<>();

	public TrashData() {
	}

	private TrashData(Map<UUID, ItemStack> loaded) {
		this.trash.putAll(loaded);
	}

	public static TrashData get(MinecraftServer server) {
		return server.getDataStorage().computeIfAbsent(TYPE);
	}

	/** The player's trashed stack, or an empty stack. Do not change it; use {@link #set}. */
	public ItemStack get(UUID player) {
		return this.trash.getOrDefault(player, ItemStack.EMPTY);
	}

	public void set(UUID player, ItemStack stack) {
		if (stack.isEmpty()) {
			this.trash.remove(player);
		} else {
			this.trash.put(player, stack.copy());
		}
		this.setDirty();
	}
}
