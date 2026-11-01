package com.journeymode.network;

import com.journeymode.JourneyMode;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Sent from the inventory's research panel: research the stack on the player's cursor. */
public record ResearchCarriedPayload() implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<ResearchCarriedPayload> TYPE =
			new CustomPacketPayload.Type<>(JourneyMode.id("research_carried"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ResearchCarriedPayload> CODEC =
			StreamCodec.unit(new ResearchCarriedPayload());

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}