package com.journeymode.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

import com.journeymode.research.ResearchCosts;

/** The little cog drawn on items in the inventory that have not been fully researched yet. */
public final class UnresearchedMarker {
	private static final String[] COG = {
			".X.X.",
			"XXXXX",
			".X.X.",
			"XXXXX",
			".X.X."};

	private UnresearchedMarker() {
	}

	/** Whether this stack should carry the cog. */
	public static boolean applies(ItemStack stack) {
		return !stack.isEmpty() && ResearchClientState.journeyEnabled()
				&& ResearchClientState.progress(stack) < ResearchCosts.required(stack.getItem());
	}

	/** Draws the cog over the top left corner of the item whose top left corner is at (x, y). */
	public static void draw(GuiGraphicsExtractor graphics, int x, int y) {
		for (int row = 0; row < COG.length; row++) {
			for (int col = 0; col < COG[row].length(); col++) {
				if (COG[row].charAt(col) == 'X') {
					int px = x + col;
					int py = y + row;
					graphics.fill(px, py, px + 1, py + 1, row + col < 4 ? ResearchPanel.ACCENT : ResearchPanel.ACCENT_DARK);
				}
			}
		}
	}
}
