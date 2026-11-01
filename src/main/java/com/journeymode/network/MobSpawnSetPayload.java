package com.journeymode.network;

import com.journeymode.JourneyMode;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Sent from the inventory's Mob Spawns slider, and back to the player when they join or it changes.
 *
 * @param multiplier the setting, from 0 (none) to 10 (1 is normal)
 */
public record MobSpawnSetPayload(int multiplier) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<MobSpawnSetPayload> TYPE =
			new CustomPacketPayload.Type<>(JourneyMode.id("mob_spawns"));
	public static final StreamCodec<RegistryFriendlyByteBuf, MobSpawnSetPayload> CODEC = ByteBufCodecs.VAR_INT
			.map(MobSpawnSetPayload::new, MobSpawnSetPayload::multiplier)
			.cast();

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
