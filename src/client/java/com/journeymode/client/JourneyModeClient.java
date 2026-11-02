package com.journeymode.client;

import net.fabricmc.api.ClientModInitializer;

public class JourneyModeClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ResearchClientState.initialize();
		ResearchSounds.initialize();
		ResearchPanel.initialize();
		MenuTheme.initialize();
	}
}