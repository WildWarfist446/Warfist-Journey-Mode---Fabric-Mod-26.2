package com.journeymode.network;

import com.journeymode.JourneyMode;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Sent from the client when the player presses the research menu key. */
public record OpenResearchPayload() implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<OpenResearchPayload> TYPE =
			new CustomPacketPayload.Type<>(JourneyMode.id("open_research"));
	public static final StreamCodec<RegistryFriendlyByteBuf, OpenResearchPayload> CODEC =
			StreamCodec.unit(new OpenResearchPayload());

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}