package com.journeymode.menu;

import com.journeymode.JourneyMode;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

public final class ModMenus {
	public static final MenuType<ResearchMenu> RESEARCH = Registry.register(BuiltInRegistries.MENU,
			JourneyMode.id("research"), new MenuType<>(ResearchMenu::new, FeatureFlags.VANILLA_SET));

	private ModMenus() {
	}

	/** Loads this class so the menu types are registered. */
	public static void initialize() {
	}
}