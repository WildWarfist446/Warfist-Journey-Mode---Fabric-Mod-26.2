package com.journeymode.client;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractFurnaceScreen;
import net.minecraft.client.gui.screens.inventory.BrewingStandScreen;
import net.minecraft.client.gui.screens.inventory.CartographyTableScreen;
import net.minecraft.client.gui.screens.inventory.CrafterScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.client.gui.screens.inventory.GrindstoneScreen;
import net.minecraft.client.gui.screens.inventory.ItemCombinerScreen;
import net.minecraft.client.gui.screens.inventory.LoomScreen;
import net.minecraft.client.gui.screens.inventory.StonecutterScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.Slot;

/**
 * The steampunk look for the game's other crafting menus (crafting table, furnaces, anvil, smithing table, stonecutter,
 * loom, cartography table, grindstone, enchanting table, brewing stand and crafter) in a Journey Mode world. Like the
 * inventory (see {@link InventoryTheme}), the game's grey picture is replaced by a brass-rimmed iron plate with sunken
 * slots; the parts that move (progress arrows, flames, buttons) are still drawn by the game on top.
 */
public final class MenuTheme {
	private MenuTheme() {
	}

	public static void initialize() {
		// The crafting table and furnaces have a recipe book button, which gets the same plate as the inventory's.
		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			if (hasRecipeBook(screen)) {
				ScreenEvents.afterExtract(screen).register((s, graphics, mouseX, mouseY, delta) -> {
					if (ResearchClientState.journeyEnabled()) {
						InventoryTheme.drawRecipeButton(client, (AbstractContainerScreen<?>) screen, graphics, mouseX, mouseY);
					}
				});
			}
		});
	}

	/** Whether the screen is one of the menus with a recipe book beside it. */
	public static boolean hasRecipeBook(Screen screen) {
		return screen instanceof CraftingScreen || screen instanceof AbstractFurnaceScreen;
	}

	/** Whether this screen is one of the menus drawn in the steampunk look, in a Journey Mode world. */
	public static boolean themes(Object screen) {
		return ResearchClientState.journeyEnabled() && (screen instanceof CraftingScreen || screen instanceof AbstractFurnaceScreen
				|| screen instanceof ItemCombinerScreen<?> || screen instanceof StonecutterScreen || screen instanceof LoomScreen
				|| screen instanceof CartographyTableScreen || screen instanceof GrindstoneScreen
				|| screen instanceof EnchantmentScreen || screen instanceof BrewingStandScreen || screen instanceof CrafterScreen);
	}

	/** Whether the screen the player is looking at right now is drawn in the steampunk look. */
	public static boolean themesCurrent() {
		return themes(Minecraft.getInstance().gui.screen());
	}

	/** Draws the menu's background over the area the game's picture would cover. */
	public static void draw(GuiGraphicsExtractor graphics, AbstractContainerScreen<?> screen, int x, int y, int width, int height) {
		ResearchPanel.chamfer(graphics, x, y, width, height, 3, ResearchPanel.PANEL_OUTER);
		ResearchPanel.chamfer(graphics, x + 1, y + 1, width - 2, height - 2, 2, ResearchPanel.ACCENT);
		ResearchPanel.chamfer(graphics, x + 2, y + 2, width - 4, height - 4, 2, ResearchPanel.ACCENT_DARK);
		ResearchPanel.chamfer(graphics, x + 3, y + 3, width - 6, height - 6, 1, ResearchPanel.PANEL_INNER);
		graphics.fill(x + 4, y + 1, x + width - 4, y + 2, ResearchPanel.BRASS_LIGHT);

		// Areas the game draws its own widgets in sit in sunken windows, like the slots.
		if (screen instanceof StonecutterScreen) {
			ResearchPanel.recess(graphics, x + 51, y + 13, 66, 56);
			ResearchPanel.recess(graphics, x + 118, y + 14, 14, 56);
		} else if (screen instanceof LoomScreen) {
			ResearchPanel.recess(graphics, x + 59, y + 12, 58, 58);
			ResearchPanel.recess(graphics, x + 118, y + 12, 14, 58);
		} else if (screen instanceof EnchantmentScreen) {
			ResearchPanel.recess(graphics, x + 59, y + 13, 110, 59);
		}

		for (Slot slot : screen.getMenu().slots) {
			if (slot.isActive()) {
				ResearchPanel.recess(graphics, x + slot.x - 1, y + slot.y - 1, 18, 18);
			}
		}

		// An arrow pointing at the result slot, where the game's picture had one.
		AbstractContainerMenu menu = screen.getMenu();
		int result = menu instanceof CraftingMenu ? 0
				: menu instanceof AbstractFurnaceMenu || menu instanceof GrindstoneMenu || menu instanceof CartographyTableMenu ? 2
				: menu instanceof ItemCombinerMenu combiner ? combiner.getResultSlot() : -1;
		if (result >= 0 && result < menu.slots.size()) {
			Slot slot = menu.slots.get(result);
			int tip = x + slot.x - 3;
			int length = menu instanceof AbstractFurnaceMenu || menu instanceof CraftingMenu ? 24 : 13;
			int middle = y + slot.y + 8;
			graphics.fill(tip - length, middle - 1, tip - 4, middle + 1, ResearchPanel.ACCENT);
			for (int i = 0; i < 4; i++) {
				graphics.fill(tip - 4 + i, middle - 3 + i, tip - 3 + i, middle + 3 - i, ResearchPanel.ACCENT);
			}
		}
	}
}
