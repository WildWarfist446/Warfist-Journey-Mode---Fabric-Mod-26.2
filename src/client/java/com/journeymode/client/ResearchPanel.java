package com.journeymode.client;

import java.util.List;

import com.journeymode.client.mixin.AbstractContainerScreenAccessor;
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
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

/**
 * The research panel beside the survival inventory. A button on the inventory's right edge shows
 * or hides it. With an item on the cursor, click the panel to sacrifice it; the panel shows how
 * far along that item is. Drawn with flat fills, no textures.
 */
public final class ResearchPanel {
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
	private static final int PANEL_GAP = 4;
	private static final int PANEL_WIDTH = 112;
	private static final int PANEL_HEIGHT = 112;

	private static final String KEY = "gui.journeymode.research.";

	private static boolean open;

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
					!click(client, inventory, event.x(), event.y(), event.button()));
		});
	}

	// ---------------------------------------------------------------- geometry

	private static int buttonX(AbstractContainerScreen<?> screen) {
		AbstractContainerScreenAccessor a = (AbstractContainerScreenAccessor) screen;
		return a.journeymode$leftPos() + a.journeymode$imageWidth() + BUTTON_GAP;
	}

	private static int buttonY(AbstractContainerScreen<?> screen) {
		return ((AbstractContainerScreenAccessor) screen).journeymode$topPos() + 4;
	}

	private static int panelX(AbstractContainerScreen<?> screen) {
		return buttonX(screen) + BUTTON_SIZE + PANEL_GAP;
	}

	private static int panelY(AbstractContainerScreen<?> screen) {
		return ((AbstractContainerScreenAccessor) screen).journeymode$topPos();
	}

	private static boolean inside(double mx, double my, int x, int y, int w, int h) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}

	/** The slot-sized spot in the panel where the cursor stack is shown and dropped. */
	private static int dropX(AbstractContainerScreen<?> screen) {
		return panelX(screen) + (PANEL_WIDTH - 18) / 2;
	}

	private static int dropY(AbstractContainerScreen<?> screen) {
		return panelY(screen) + 22;
	}

	// ---------------------------------------------------------------- drawing

	private static void extract(Minecraft client, InventoryScreen screen, GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		Font font = client.font;
		int bx = buttonX(screen);
		int by = buttonY(screen);

		// Toggle button: a flat square with a book-like mark.
		boolean overButton = inside(mouseX, mouseY, bx, by, BUTTON_SIZE, BUTTON_SIZE);
		graphics.fill(bx, by, bx + BUTTON_SIZE, by + BUTTON_SIZE, open ? ACCENT_DARK : PANEL_INNER);
		graphics.outline(bx, by, BUTTON_SIZE, BUTTON_SIZE, ACCENT);
		graphics.centeredText(font, "R", bx + BUTTON_SIZE / 2, by + 6, open ? TEXT : ACCENT);
		if (overButton) {
			graphics.fill(bx, by, bx + BUTTON_SIZE, by + BUTTON_SIZE, HOVER);
			graphics.setTooltipForNextFrame(font, Component.translatable(KEY + "title"), mouseX, mouseY);
		}
		if (!open) {
			return;
		}

		int px = panelX(screen);
		int py = panelY(screen);
		graphics.fill(px, py, px + PANEL_WIDTH, py + PANEL_HEIGHT, PANEL_OUTER);
		graphics.fill(px + 3, py + 3, px + PANEL_WIDTH - 3, py + PANEL_HEIGHT - 3, PANEL_INNER);
		graphics.outline(px, py, PANEL_WIDTH, PANEL_HEIGHT, ACCENT);
		graphics.outline(px + 2, py + 2, PANEL_WIDTH - 4, PANEL_HEIGHT - 4, ACCENT_DARK);
		graphics.centeredText(font, Component.translatable(KEY + "title"), px + PANEL_WIDTH / 2, py + 7, ACCENT);

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
				px + PANEL_WIDTH / 2, py + PANEL_HEIGHT - 15, TEXT_DIM);
	}

	// ---------------------------------------------------------------- input

	/** Handles a click; returns true if the panel used it, so the inventory should ignore it. */
	private static boolean click(Minecraft client, InventoryScreen screen, double mx, double my, int button) {
		if (inside(mx, my, buttonX(screen), buttonY(screen), BUTTON_SIZE, BUTTON_SIZE)) {
			if (button == 0) {
				open = !open;
				client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
			}
			return true;
		}
		// Anywhere on the open panel: swallow the click, or the inventory would throw the cursor item away.
		if (open && inside(mx, my, panelX(screen), panelY(screen), PANEL_WIDTH, PANEL_HEIGHT)) {
			if (button == 0 && !screen.getMenu().getCarried().isEmpty()) {
				ClientPlayNetworking.send(new ResearchCarriedPayload());
				client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
			}
			return true;
		}
		return false;
	}
}