package com.journeymode.client;

import com.journeymode.JourneyMode;
import com.journeymode.network.OpenResearchPayload;
import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.KeyMapping;

/** The "Research Menu" key (default R; rebindable in Controls). */
public final class ResearchKeybind {
	public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(JourneyMode.id("journey_mode"));

	private static final KeyMapping KEY = KeyMappingHelper.registerKeyMapping(
			new KeyMapping("key.journeymode.research_menu", InputConstants.Type.KEYSYM, InputConstants.KEY_R, CATEGORY));

	private ResearchKeybind() {
	}

	public static void initialize() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (KEY.consumeClick()) {
				if (client.player != null && client.gui.screen() == null) {
					ClientPlayNetworking.send(new OpenResearchPayload());
				}
			}
		});
	}
}