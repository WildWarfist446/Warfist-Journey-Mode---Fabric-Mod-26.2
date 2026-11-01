package com.journeymode.research;

import com.journeymode.JourneyMode;
import com.journeymode.menu.ResearchMenu;
import com.journeymode.network.OpenResearchPayload;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleMenuProvider;

/** Registers the packet that opens the research menu. */
public final class ResearchManager {
	private static final Component TITLE = Component.translatable("container." + JourneyMode.MOD_ID + ".research");

	private ResearchManager() {
	}

	public static void initialize() {
		PayloadTypeRegistry.serverboundPlay().register(OpenResearchPayload.TYPE, OpenResearchPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(OpenResearchPayload.TYPE, (payload, context) ->
				context.player().openMenu(new SimpleMenuProvider(
						(containerId, inventory, player) -> new ResearchMenu(containerId, inventory), TITLE)));
	}
}