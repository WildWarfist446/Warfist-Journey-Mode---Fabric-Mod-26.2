package com.journeymode.network;

import com.journeymode.JourneyMode;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

/** Sent when a player joins and whenever their trash slot changes: the stack in it, empty if none. */
public record TrashSyncPayload(ItemStack stack) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<TrashSyncPayload> TYPE =
			new CustomPacketPayload.Type<>(JourneyMode.id("trash_sync"));
	public static final StreamCodec<RegistryFriendlyByteBuf, TrashSyncPayload> CODEC = ItemStack.OPTIONAL_STREAM_CODEC
			.map(TrashSyncPayload::new, TrashSyncPayload::stack)
			.cast();

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
