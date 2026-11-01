package com.journeymode.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.inventory.Slot;

/**
 * The steampunk look for the survival inventory in a Journey Mode world: a brass-rimmed iron plate
 * with sunken slots, drawn in place of the game's inventory picture (see {@code InventoryScreenMixin})
 * in the same style as the Journey Mode panels.
 */
public final class InventoryTheme {
	/** Where the player model is drawn inside the inventory picture. */
	private static final int MODEL_X = 25;
	private static final int MODEL_Y = 7;
	private static final int MODEL_WIDTH = 51;
	private static final int MODEL_HEIGHT = 72;

	private InventoryTheme() {
	}

	/** The colour for the inventory's "Crafting" title, which the game's dark grey would not show up against. */
	public static int titleColor() {
		return ResearchPanel.TEXT_DIM;
	}

	/** Draws the inventory's background over the area the game's picture would cover. */
	public static void draw(GuiGraphicsExtractor graphics, InventoryScreen screen, int x, int y, int width, int height) {
		ResearchPanel.chamfer(graphics, x, y, width, height, 3, ResearchPanel.PANEL_OUTER);
		ResearchPanel.chamfer(graphics, x + 1, y + 1, width - 2, height - 2, 2, ResearchPanel.ACCENT);
		ResearchPanel.chamfer(graphics, x + 2, y + 2, width - 4, height - 4, 2, ResearchPanel.ACCENT_DARK);
		ResearchPanel.chamfer(graphics, x + 3, y + 3, width - 6, height - 6, 1, ResearchPanel.PANEL_INNER);
		graphics.fill(x + 4, y + 1, x + width - 4, y + 2, ResearchPanel.BRASS_LIGHT);

		// The player model sits in a sunken window, like the slots.
		ResearchPanel.recess(graphics, x + MODEL_X, y + MODEL_Y, MODEL_WIDTH, MODEL_HEIGHT);

		// Every slot of the inventory (armor, crafting, result, off hand, backpack and hotbar) gets a sunken square.
		for (Slot slot : screen.getMenu().slots) {
			ResearchPanel.recess(graphics, x + slot.x - 1, y + slot.y - 1, 18, 18);
		}

		// The crafting arrow, between the crafting grid and its result.
		for (int i = 0; i < 4; i++) {
			graphics.fill(x + 147 + i, y + 33 + i, x + 148 + i, y + 39 - i, ResearchPanel.ACCENT);
		}
		graphics.fill(x + 138, y + 35, x + 147, y + 37, ResearchPanel.ACCENT);
	}
}
