package com.journeymode.client;

import com.journeymode.client.mixin.AbstractContainerScreenAccessor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

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

	/** Whether the steampunk look applies to the recipe book: in a Journey Mode world, beside the survival inventory only. */
	public static boolean themesRecipeBook() {
		return ResearchClientState.journeyEnabled() && Minecraft.getInstance().gui.screen() instanceof InventoryScreen;
	}

	/** Draws the recipe book's background over the area the game's picture would cover. */
	public static void drawRecipeBook(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
		ResearchPanel.chamfer(graphics, x, y, width, height, 3, ResearchPanel.PANEL_OUTER);
		ResearchPanel.chamfer(graphics, x + 1, y + 1, width - 2, height - 2, 2, ResearchPanel.ACCENT);
		ResearchPanel.chamfer(graphics, x + 2, y + 2, width - 4, height - 4, 2, ResearchPanel.ACCENT_DARK);
		ResearchPanel.chamfer(graphics, x + 3, y + 3, width - 6, height - 6, 1, ResearchPanel.PANEL_INNER);
		graphics.fill(x + 4, y + 1, x + width - 4, y + 2, ResearchPanel.BRASS_LIGHT);
		// The recipes sit in a sunken window: a grid of 5 x 4 cells of 25 pixels, starting at (11, 31).
		ResearchPanel.recess(graphics, x + 9, y + 29, 129, 104);
	}

	/**
	 * A recipe's box in the recipe book, in place of the game's grey slot picture. {@code sprite} says which
	 * kind it is: recipes you can craft now stand out as raised brass plates, the rest are sunken, and a
	 * recipe with several variants gets a small brass mark in its corner.
	 */
	public static void drawRecipeSlot(GuiGraphicsExtractor graphics, Identifier sprite, int x, int y, int width, int height) {
		String name = sprite.getPath();
		if (name.contains("uncraftable")) {
			ResearchPanel.recess(graphics, x, y, width, height);
		} else {
			ResearchPanel.chamfer(graphics, x, y, width, height, 1, ResearchPanel.PANEL_OUTER);
			ResearchPanel.chamfer(graphics, x + 1, y + 1, width - 2, height - 2, 1, ResearchPanel.ACCENT);
			graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, ResearchPanel.ACCENT_DARK);
			graphics.fill(x + 2, y + 1, x + width - 2, y + 2, ResearchPanel.BRASS_LIGHT);
		}
		if (name.contains("many")) {
			graphics.fill(x + width - 6, y + height - 4, x + width - 3, y + height - 3, ResearchPanel.BRASS_LIGHT);
			graphics.fill(x + width - 6, y + height - 6, x + width - 3, y + height - 5, ResearchPanel.BRASS_LIGHT);
		}
	}

	/** A category tab beside the recipe book: a raised brass plate while selected, a dark iron plate otherwise. */
	public static void drawRecipeTab(GuiGraphicsExtractor graphics, Identifier sprite, int x, int y, int width, int height) {
		boolean selected = sprite.getPath().contains("selected");
		ResearchPanel.chamfer(graphics, x, y, width, height, 2, ResearchPanel.PANEL_OUTER);
		ResearchPanel.chamfer(graphics, x + 1, y + 1, width - 2, height - 2, 1, ResearchPanel.ACCENT);
		graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, selected ? ResearchPanel.ACCENT_DARK : ResearchPanel.PANEL_INNER);
		graphics.fill(x + 2, y + 1, x + width - 2, y + 2, ResearchPanel.BRASS_LIGHT);
	}

	/**
	 * Draws a steampunk plate with a book icon over the game's recipe book button, which still does
	 * the clicking underneath. It looks pressed while the recipe book is open.
	 */
	public static void drawRecipeButton(Minecraft client, InventoryScreen screen, GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		for (var widget : net.fabricmc.fabric.api.client.screen.v1.Screens.getWidgets(screen)) {
			if (widget instanceof ImageButton button) {
				AbstractContainerScreenAccessor a = (AbstractContainerScreenAccessor) screen;
				boolean open = a.journeymode$leftPos() > (screen.width - a.journeymode$imageWidth()) / 2;
				int x = button.getX();
				int y = button.getY();
				int w = button.getWidth();
				int h = button.getHeight();
				ResearchPanel.chamfer(graphics, x, y, w, h, 2, ResearchPanel.PANEL_OUTER);
				ResearchPanel.chamfer(graphics, x + 1, y + 1, w - 2, h - 2, 1, ResearchPanel.ACCENT);
				graphics.fill(x + 2, y + 2, x + w - 2, y + h - 2, open ? ResearchPanel.ACCENT_DARK : ResearchPanel.PANEL_INNER);
				graphics.fill(x + 2, y + 1, x + w - 2, y + 2, ResearchPanel.BRASS_LIGHT);
				graphics.fakeItem(new ItemStack(Items.KNOWLEDGE_BOOK), x + (w - 16) / 2, y + (h - 16) / 2);
				if (mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h) {
					graphics.fill(x + 2, y + 2, x + w - 2, y + h - 2, 0x40FFFFFF);
				}
			}
		}
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

		// A cog on each side of the "Crafting" title, which starts at x 97, y 8 and is about 38 pixels wide.
		ResearchPanel.gear(graphics, x + 87, y + 6);
		ResearchPanel.gear(graphics, x + 141, y + 6);

		// The crafting arrow, between the crafting grid and its result.
		for (int i = 0; i < 4; i++) {
			graphics.fill(x + 147 + i, y + 33 + i, x + 148 + i, y + 39 - i, ResearchPanel.ACCENT);
		}
		graphics.fill(x + 138, y + 35, x + 147, y + 37, ResearchPanel.ACCENT);
	}
}
