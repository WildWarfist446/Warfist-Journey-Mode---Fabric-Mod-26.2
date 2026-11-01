package com.journeymode.network;

import com.journeymode.JourneyMode;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Sent when a player joins and whenever they switch God Mode: whether it is on for them. */
public record GodModeSyncPayload(boolean on) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<GodModeSyncPayload> TYPE =
			new CustomPacketPayload.Type<>(JourneyMode.id("god_mode_sync"));
	public static final StreamCodec<RegistryFriendlyByteBuf, GodModeSyncPayload> CODEC = ByteBufCodecs.BOOL
			.map(GodModeSyncPayload::new, GodModeSyncPayload::on)
			.cast();

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
