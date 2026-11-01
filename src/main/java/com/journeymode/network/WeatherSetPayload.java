package com.journeymode.network;

import com.journeymode.JourneyMode;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Sent from the inventory's weather slider: set the weather.
 *
 * @param stop 0 for clear, 1 for rain, 2 for thunder
 */
public record WeatherSetPayload(int stop) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<WeatherSetPayload> TYPE =
			new CustomPacketPayload.Type<>(JourneyMode.id("weather_set"));
	public static final StreamCodec<RegistryFriendlyByteBuf, WeatherSetPayload> CODEC = ByteBufCodecs.VAR_INT
			.map(WeatherSetPayload::new, WeatherSetPayload::stop)
			.cast();

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
