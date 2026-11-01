package com.journeymode.network;

import com.journeymode.JourneyMode;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Sent from the inventory's item list: give the player a researched item. The server checks the
 * item really is researched.
 *
 * @param item the item to give
 * @param mode {@link #STACK_TO_CURSOR}, {@link #ONE_TO_CURSOR} or {@link #STACK_TO_INVENTORY}
 */
public record RequestItemPayload(Identifier item, int mode) implements CustomPacketPayload {
	public static final int STACK_TO_CURSOR = 0;
	public static final int ONE_TO_CURSOR = 1;
	public static final int STACK_TO_INVENTORY = 2;

	public static final CustomPacketPayload.Type<RequestItemPayload> TYPE =
			new CustomPacketPayload.Type<>(JourneyMode.id("request_item"));
	public static final StreamCodec<RegistryFriendlyByteBuf, RequestItemPayload> CODEC = StreamCodec.composite(
			Identifier.STREAM_CODEC, RequestItemPayload::item,
			ByteBufCodecs.VAR_INT, RequestItemPayload::mode,
			RequestItemPayload::new).cast();

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}