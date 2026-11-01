package com.journeymode.network;

import com.journeymode.JourneyMode;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Sent from the inventory's God Mode panel: switch God Mode on or off for the sender. */
public record GodModeTogglePayload() implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<GodModeTogglePayload> TYPE =
			new CustomPacketPayload.Type<>(JourneyMode.id("god_mode_toggle"));
	public static final StreamCodec<RegistryFriendlyByteBuf, GodModeTogglePayload> CODEC =
			StreamCodec.unit(new GodModeTogglePayload());

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
