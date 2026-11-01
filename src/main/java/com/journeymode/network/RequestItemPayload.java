package com.journeymode.network;

import com.journeymode.JourneyMode;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Sent from the inventory's item list when the player clicks a researched item, like a click in
 * the creative menu's item list. The server checks the item really is researched.
 *
 * @param key    what was clicked (see {@code ResearchKeys})
 * @param button the mouse button: 0 left, 1 right, 2 middle
 * @param quick  whether shift was held
 */
public record RequestItemPayload(String key, int button, boolean quick) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<RequestItemPayload> TYPE =
			new CustomPacketPayload.Type<>(JourneyMode.id("request_item"));
	public static final StreamCodec<RegistryFriendlyByteBuf, RequestItemPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, RequestItemPayload::key,
			ByteBufCodecs.VAR_INT, RequestItemPayload::button,
			ByteBufCodecs.BOOL, RequestItemPayload::quick,
			RequestItemPayload::new).cast();

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}