package com.journeymode.client;

import java.util.List;

import com.journeymode.client.mixin.AbstractContainerScreenAccessor;
import com.journeymode.network.RequestItemPayload;
import com.journeymode.network.ResearchCarriedPayload;
import com.journeymode.research.ResearchCosts;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The Journey Mode panels beside the survival inventory, on the left so they stay clear of JEI on
 * the right. Two buttons on the inventory's left edge each show or hide a panel (only one is
 * open at a time):
 * <ul>
 *   <li><b>Research</b>: with an item on the cursor, click the panel to sacrifice it; the panel
 *   shows how far along that item is.</li>
 *   <li><b>Items</b>: a scrolling list of everything researched. Left-click takes a stack onto
 *   the cursor, right-click takes one, shift-click puts a stack straight in the inventory.</li>
 * </ul>
 * Drawn with flat fills, no textures.
 */
public final class ResearchPanel {
	private enum Mode {
		NONE, RESEARCH, ITEMS
	}

	private static final int PANEL_OUTER = 0xF0182028;
	private static final int PANEL_INNER = 0xF0232F3B;
	private static final int ACCENT = 0xFF5BC0EB;
	private static final int ACCENT_DARK = 0xFF2F6F8F;
	private static final int DONE = 0xFF7BD389;
	private static final int TEXT = 0xFFE6EEF5;
	private static final int TEXT_DIM = 0xFF93A4B3;
	private static final int SLOT_BORDER = 0xFF3C5266;
	private static final int SLOT_FILL = 0xFF111820;
	private static final int HOVER = 0x40FFFFFF;

	private static final int BUTTON_SIZE = 20;
	private static final int BUTTON_GAP = 2;
	private static final int BUTTON_SPACING = 24;
	private static final int PANEL_GAP = 4;
	/** The recipe book's width, including its border. */
	private static final int RECIPE_BOOK_WIDTH = 148;
	private static final int PANEL_WIDTH = 124;
	private static final int RESEARCH_HEIGHT = 112;
	private static final int ITEMS_HEIGHT = 150;

	private static final int GRID_COLUMNS = 6;
	private static final int GRID_ROWS = 5;
	private static final int CELL = 18;
	private static final int GRID_OFFSET_X = 8;
	private static final int GRID_OFFSET_Y = 22;

	private static final String KEY = "gui.journeymode.research.";

	private static Mode mode = Mode.NONE;
	/** The first visible row of the item list. */
	private static int scrollRow;

	private ResearchPanel() {
	}

	public static void initialize() {
		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			if (!(screen instanceof InventoryScreen inventory)) {
				return;
			}
			ScreenEvents.afterExtract(screen).register((s, graphics, mouseX, mouseY, delta) ->
					extract(client, inventory, graphics, mouseX, mouseY));
			ScreenMouseEvents.allowMouseClick(screen).register((s, event) ->
					!click(client, inventory, event.x(), event.y(), event.button(), event.hasShiftDown()));
			ScreenMouseEvents.allowMouseScroll(screen).register((s, mouseX, mouseY, horizontal, vertical) ->
					!scroll(inventory, mouseX, mouseY, vertical));
		});
	}

	// ---------------------------------------------------------------- geometry

	/**
	 * The left edge of everything on the inventory's left: the inventory itself, or the recipe book
	 * when it is open (it shifts the inventory right, and sits just left of it).
	 */
	private static int leftEdge(AbstractContainerScreen<?> screen) {
		AbstractContainerScreenAccessor a = (AbstractContainerScreenAccessor) screen;
		int centered = (screen.width - a.journeymode$imageWidth()) / 2;
		boolean recipeBookOpen = a.journeymode$leftPos() > centered;
		return a.journeymode$leftPos() - (recipeBookOpen ? RECIPE_BOOK_WIDTH : 0);
	}

	private static int buttonX(AbstractContainerScreen<?> screen) {
		return leftEdge(screen) - BUTTON_GAP - BUTTON_SIZE;
	}

	/** The top of the button for a mode: research first, the item list under it. */
	private static int buttonY(AbstractContainerScreen<?> screen, Mode forMode) {
		return ((AbstractContainerScreenAccessor) screen).journeymode$topPos() + 4 + (forMode == Mode.ITEMS ? BUTTON_SPACING : 0);
	}

	private static int panelX(AbstractContainerScreen<?> screen) {
		return buttonX(screen) - PANEL_GAP - PANEL_WIDTH;
	}

	private static int panelY(AbstractContainerScreen<?> screen) {
		return ((AbstractContainerScreenAccessor) screen).journeymode$topPos();
	}

	private static int panelHeight() {
		return mode == Mode.ITEMS ? ITEMS_HEIGHT : RESEARCH_HEIGHT;
	}

	private static boolean inside(double mx, double my, int x, int y, int w, int h) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}

	/** The slot-sized spot in the research panel where the cursor stack is shown and dropped. */
	private static int dropX(AbstractContainerScreen<?> screen) {
		return panelX(screen) + (PANEL_WIDTH - 18) / 2;
	}

	private static int dropY(AbstractContainerScreen<?> screen) {
		return panelY(screen) + 22;
	}

	private static int gridX(AbstractContainerScreen<?> screen) {
		return panelX(screen) + GRID_OFFSET_X;
	}

	private static int gridY(AbstractContainerScreen<?> screen) {
		return panelY(screen) + GRID_OFFSET_Y;
	}

	private static int totalRows() {
		return (ResearchClientState.researchedItems().size() + GRID_COLUMNS - 1) / GRID_COLUMNS;
	}

	private static int maxScrollRow() {
		return Math.max(0, totalRows() - GRID_ROWS);
	}

	/** The index into the researched items of the grid cell under the mouse, or -1. */
	private static int itemAt(AbstractContainerScreen<?> screen, double mx, double my) {
		int gx = gridX(screen);
		int gy = gridY(screen);
		if (!inside(mx, my, gx, gy, GRID_COLUMNS * CELL, GRID_ROWS * CELL)) {
			return -1;
		}
		int index = (scrollRow + (int) (my - gy) / CELL) * GRID_COLUMNS + (int) (mx - gx) / CELL;
		return index < ResearchClientState.researchedItems().size() ? index : -1;
	}

	// ---------------------------------------------------------------- drawing

	private static void extract(Minecraft client, InventoryScreen screen, GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		Font font = client.font;
		scrollRow = Math.clamp(scrollRow, 0, maxScrollRow());
		drawButton(font, screen, graphics, mouseX, mouseY, Mode.RESEARCH, "R", "title");
		drawButton(font, screen, graphics, mouseX, mouseY, Mode.ITEMS, "I", "items_title");
		if (mode == Mode.NONE) {
			return;
		}

		int px = panelX(screen);
		int py = panelY(screen);
		int height = panelHeight();
		graphics.fill(px, py, px + PANEL_WIDTH, py + height, PANEL_OUTER);
		graphics.fill(px + 3, py + 3, px + PANEL_WIDTH - 3, py + height - 3, PANEL_INNER);
		graphics.outline(px, py, PANEL_WIDTH, height, ACCENT);
		graphics.outline(px + 2, py + 2, PANEL_WIDTH - 4, height - 4, ACCENT_DARK);
		graphics.centeredText(font, Component.translatable(KEY + (mode == Mode.ITEMS ? "items_title" : "title")),
				px + PANEL_WIDTH / 2, py + 7, ACCENT);

		if (mode == Mode.RESEARCH) {
			extractResearch(font, screen, graphics, mouseX, mouseY, px, py);
		} else {
			extractItems(font, screen, graphics, mouseX, mouseY, px, py);
		}
	}

	/** A toggle button: a flat square with a placeholder mark until the logos are made. */
	private static void drawButton(Font font, InventoryScreen screen, GuiGraphicsExtractor graphics, int mouseX, int mouseY,
			Mode forMode, String mark, String tooltipKey) {
		int bx = buttonX(screen);
		int by = buttonY(screen, forMode);
		boolean active = mode == forMode;
		graphics.fill(bx, by, bx + BUTTON_SIZE, by + BUTTON_SIZE, active ? ACCENT_DARK : PANEL_INNER);
		graphics.outline(bx, by, BUTTON_SIZE, BUTTON_SIZE, ACCENT);
		graphics.centeredText(font, mark, bx + BUTTON_SIZE / 2, by + 6, active ? TEXT : ACCENT);
		if (inside(mouseX, mouseY, bx, by, BUTTON_SIZE, BUTTON_SIZE)) {
			graphics.fill(bx, by, bx + BUTTON_SIZE, by + BUTTON_SIZE, HOVER);
			graphics.setTooltipForNextFrame(font, Component.translatable(KEY + tooltipKey), mouseX, mouseY);
		}
	}

	private static void extractResearch(Font font, InventoryScreen screen, GuiGraphicsExtractor graphics, int mouseX, int mouseY,
			int px, int py) {
		// The drop spot.
		ItemStack carried = screen.getMenu().getCarried();
		int dx = dropX(screen);
		int dy = dropY(screen);
		graphics.outline(dx - 3, dy - 3, 24, 24, ACCENT_DARK);
		graphics.fill(dx - 1, dy - 1, dx + 19, dy + 19, SLOT_BORDER);
		graphics.fill(dx, dy, dx + 18, dy + 18, SLOT_FILL);
		if (!carried.isEmpty()) {
			graphics.fakeItem(carried, dx + 1, dy + 1);
		}
		if (inside(mouseX, mouseY, dx, dy, 18, 18) && !carried.isEmpty()) {
			graphics.fill(dx, dy, dx + 18, dy + 18, HOVER);
		}

		// Progress for the item on the cursor.
		int barX = px + 12;
		int barY = py + 52;
		int barWidth = PANEL_WIDTH - 24;
		graphics.fill(barX - 1, barY - 1, barX + barWidth + 1, barY + 9, SLOT_BORDER);
		graphics.fill(barX, barY, barX + barWidth, barY + 8, SLOT_FILL);
		int textY = barY + 13;
		if (carried.isEmpty()) {
			int y = textY;
			for (FormattedCharSequence line : font.split(Component.translatable(KEY + "hint"), PANEL_WIDTH - 16)) {
				graphics.centeredText(font, line, px + PANEL_WIDTH / 2, y, TEXT_DIM);
				y += 10;
			}
		} else {
			int needed = ResearchCosts.required(carried.getItem());
			int done = Math.min(ResearchClientState.progress(carried.getItem()), needed);
			boolean researched = done >= needed;
			graphics.fill(barX, barY, barX + barWidth * done / needed, barY + 8, researched ? DONE : ACCENT);
			Component status = researched
					? Component.translatable(KEY + "researched")
					: Component.literal(done + " / " + needed);
			graphics.centeredText(font, status, px + PANEL_WIDTH / 2, textY, researched ? DONE : TEXT);
		}
		graphics.centeredText(font, Component.translatable(KEY + "total", ResearchClientState.researchedTotal()),
				px + PANEL_WIDTH / 2, py + RESEARCH_HEIGHT - 15, TEXT_DIM);
	}

	private static void extractItems(Font font, InventoryScreen screen, GuiGraphicsExtractor graphics, int mouseX, int mouseY,
			int px, int py) {
		List<Item> items = ResearchClientState.researchedItems();
		int gx = gridX(screen);
		int gy = gridY(screen);
		int hovered = itemAt(screen, mouseX, mouseY);

		for (int row = 0; row < GRID_ROWS; row++) {
			for (int col = 0; col < GRID_COLUMNS; col++) {
				int x = gx + col * CELL;
				int y = gy + row * CELL;
				graphics.fill(x, y, x + CELL, y + CELL, SLOT_BORDER);
				graphics.fill(x + 1, y + 1, x + CELL - 1, y + CELL - 1, SLOT_FILL);
				int index = (scrollRow + row) * GRID_COLUMNS + col;
				if (index < items.size()) {
					graphics.fakeItem(new ItemStack(items.get(index)), x + 1, y + 1);
					if (index == hovered) {
						graphics.fill(x + 1, y + 1, x + CELL - 1, y + CELL - 1, HOVER);
					}
				}
			}
		}

		if (maxScrollRow() > 0) {
			int trackX = px + PANEL_WIDTH - 8;
			int trackHeight = GRID_ROWS * CELL;
			graphics.fill(trackX, gy, trackX + 3, gy + trackHeight, SLOT_FILL);
			int thumbHeight = Math.max(6, trackHeight * GRID_ROWS / totalRows());
			int thumbY = gy + (trackHeight - thumbHeight) * scrollRow / maxScrollRow();
			graphics.fill(trackX, thumbY, trackX + 3, thumbY + thumbHeight, ACCENT);
		}

		int textY = gy + GRID_ROWS * CELL + 5;
		if (items.isEmpty()) {
			textY = gy + 20;
			for (FormattedCharSequence line : font.split(Component.translatable(KEY + "none_yet"), GRID_COLUMNS * CELL - 12)) {
				graphics.centeredText(font, line, gx + GRID_COLUMNS * CELL / 2, textY, TEXT_DIM);
				textY += 10;
			}
		} else {
			for (FormattedCharSequence line : font.split(Component.translatable(KEY + "items_hint"), PANEL_WIDTH - 16)) {
				graphics.centeredText(font, line, px + PANEL_WIDTH / 2, textY, TEXT_DIM);
				textY += 10;
			}
		}

		if (hovered >= 0) {
			graphics.setTooltipForNextFrame(font, new ItemStack(items.get(hovered)).getHoverName(), mouseX, mouseY);
		}
	}

	// ---------------------------------------------------------------- input

	/** Handles a click; returns true if the panels used it, so the inventory should ignore it. */
	private static boolean click(Minecraft client, InventoryScreen screen, double mx, double my, int button, boolean shift) {
		for (Mode forMode : new Mode[] {Mode.RESEARCH, Mode.ITEMS}) {
			if (inside(mx, my, buttonX(screen), buttonY(screen, forMode), BUTTON_SIZE, BUTTON_SIZE)) {
				if (button == 0) {
					mode = mode == forMode ? Mode.NONE : forMode;
					client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
				}
				return true;
			}
		}
		// Anywhere on an open panel: swallow the click, or the inventory would throw the cursor item away.
		if (mode == Mode.NONE || !inside(mx, my, panelX(screen), panelY(screen), PANEL_WIDTH, panelHeight())) {
			return false;
		}
		if (mode == Mode.RESEARCH) {
			if (button == 0 && !screen.getMenu().getCarried().isEmpty()) {
				ClientPlayNetworking.send(new ResearchCarriedPayload());
				client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
			}
		} else if (button == 0 || button == 1) {
			int index = itemAt(screen, mx, my);
			if (index >= 0) {
				Item item = ResearchClientState.researchedItems().get(index);
				int requestMode = shift ? RequestItemPayload.STACK_TO_INVENTORY
						: button == 0 ? RequestItemPayload.STACK_TO_CURSOR : RequestItemPayload.ONE_TO_CURSOR;
				ClientPlayNetworking.send(new RequestItemPayload(BuiltInRegistries.ITEM.getKey(item), requestMode));
				client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
			}
		}
		return true;
	}

	/** Scrolls the item list when the mouse is over its panel; returns true if the scroll was used. */
	private static boolean scroll(InventoryScreen screen, double mx, double my, double amount) {
		if (mode != Mode.ITEMS || !inside(mx, my, panelX(screen), panelY(screen), PANEL_WIDTH, panelHeight())) {
			return false;
		}
		if (amount != 0) {
			scrollRow = Math.clamp(scrollRow - (int) Math.signum(amount), 0, maxScrollRow());
		}
		return true;
	}
}
