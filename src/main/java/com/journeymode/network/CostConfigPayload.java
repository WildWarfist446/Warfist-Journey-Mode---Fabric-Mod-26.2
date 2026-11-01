package com.journeymode.network;

import java.util.LinkedHashMap;
import java.util.Map;

import com.journeymode.JourneyMode;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Sent to a player when they join: the server's research cost rules, in order (item ids and "#tag" ids to costs). */
public record CostConfigPayload(Map<String, Integer> rules) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<CostConfigPayload> TYPE =
			new CustomPacketPayload.Type<>(JourneyMode.id("cost_config"));
	public static final StreamCodec<RegistryFriendlyByteBuf, CostConfigPayload> CODEC = ByteBufCodecs
			.map(LinkedHashMap<String, Integer>::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_INT)
			.map(CostConfigPayload::new, payload -> new LinkedHashMap<>(payload.rules()))
			.cast();

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
