package com.journeymode.network;

import com.journeymode.JourneyMode;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Sent from the inventory's time slider: set the time to one of the four named times.
 *
 * @param stop 0 for day, 1 for noon, 2 for night, 3 for midnight
 */
public record TimeSetPayload(int stop) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<TimeSetPayload> TYPE =
			new CustomPacketPayload.Type<>(JourneyMode.id("time_set"));
	public static final StreamCodec<RegistryFriendlyByteBuf, TimeSetPayload> CODEC = ByteBufCodecs.VAR_INT
			.map(TimeSetPayload::new, TimeSetPayload::stop)
			.cast();

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
