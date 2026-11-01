package com.journeymode.network;

import com.journeymode.JourneyMode;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Sent to a player when they join: whether Journey Mode is on in this world. */
public record JourneyStatePayload(boolean enabled) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<JourneyStatePayload> TYPE =
			new CustomPacketPayload.Type<>(JourneyMode.id("journey_state"));
	public static final StreamCodec<RegistryFriendlyByteBuf, JourneyStatePayload> CODEC = ByteBufCodecs.BOOL
			.map(JourneyStatePayload::new, JourneyStatePayload::enabled)
			.cast();

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
