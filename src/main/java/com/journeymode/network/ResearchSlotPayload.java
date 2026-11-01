package com.journeymode.network;

import com.journeymode.JourneyMode;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Sent when a player shift-clicks an inventory slot while the research panel is open: research the stack in that menu slot. */
public record ResearchSlotPayload(int slot) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<ResearchSlotPayload> TYPE =
			new CustomPacketPayload.Type<>(JourneyMode.id("research_slot"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ResearchSlotPayload> CODEC = ByteBufCodecs.VAR_INT
			.map(ResearchSlotPayload::new, ResearchSlotPayload::slot)
			.cast();

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
