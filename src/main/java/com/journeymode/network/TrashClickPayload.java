package com.journeymode.network;

import com.journeymode.JourneyMode;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Sent when the player clicks the trash slot: put the cursor stack in it, or take the trashed stack back if the cursor is empty. */
public record TrashClickPayload() implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<TrashClickPayload> TYPE =
			new CustomPacketPayload.Type<>(JourneyMode.id("trash_click"));
	public static final StreamCodec<RegistryFriendlyByteBuf, TrashClickPayload> CODEC =
			StreamCodec.unit(new TrashClickPayload());

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
