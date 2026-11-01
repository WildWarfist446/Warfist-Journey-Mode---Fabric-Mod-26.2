package com.journeymode.client;

import com.journeymode.client.menu.ResearchScreen;
import com.journeymode.menu.ModMenus;

import net.fabricmc.api.ClientModInitializer;

import net.minecraft.client.gui.screens.MenuScreens;

public class JourneyModeClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		MenuScreens.register(ModMenus.RESEARCH, ResearchScreen::new);
		ResearchKeybind.initialize();
	}
}