package com.journeymode;

import com.journeymode.research.ResearchManager;

import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JourneyMode implements ModInitializer {
	public static final String MOD_ID = "journeymode";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	/**
	 * Set on the world creation screen when the player switches Journey Mode on. The next new world
	 * to load takes it (see {@code JourneySettings.decide}).
	 */
	public static volatile boolean pendingNewWorld;

	@Override
	public void onInitialize() {
		ResearchManager.initialize();
		LOGGER.info("Journey Mode loaded");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}