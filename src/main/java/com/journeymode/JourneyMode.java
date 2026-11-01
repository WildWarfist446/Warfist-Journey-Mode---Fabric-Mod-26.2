package com.journeymode;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JourneyMode implements ModInitializer {
	public static final String MOD_ID = "journeymode";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Journey Mode loaded");
	}
}