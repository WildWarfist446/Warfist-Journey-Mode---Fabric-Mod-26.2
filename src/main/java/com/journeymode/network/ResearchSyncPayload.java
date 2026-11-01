package com.journeymode.network;

import java.util.HashMap;
import java.util.Map;

import com.journeymode.JourneyMode;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Sent to a player when they join and whenever their research changes: how much of each item they have sacrificed. */
public record ResearchSyncPayload(Map<Identifier, Integer> progress) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<ResearchSyncPayload> TYPE =
			new CustomPacketPayload.Type<>(JourneyMode.id("research_sync"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ResearchSyncPayload> CODEC = ByteBufCodecs
			.map(HashMap<Identifier, Integer>::new, Identifier.STREAM_CODEC, ByteBufCodecs.VAR_INT)
			.map(ResearchSyncPayload::new, payload -> new HashMap<>(payload.progress()))
			.cast();

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}