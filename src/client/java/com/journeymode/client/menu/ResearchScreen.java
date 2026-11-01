package com.journeymode.client.menu;

import com.journeymode.menu.ResearchMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/** The research menu screen: a slot, a progress bar and a Research button, drawn with flat fills. */
public class ResearchScreen extends AbstractContainerScreen<ResearchMenu> {
	private static final int PANEL_OUTER = 0xF0182028;
	private static final int PANEL_INNER = 0xF0232F3B;
	private static final int ACCENT = 0xFF5BC0EB;
	private static final int ACCENT_DARK = 0xFF2F6F8F;
	private static final int DONE = 0xFF7BD389;
	private static final int TEXT = 0xFFE6EEF5;
	private static final int TEXT_DIM = 0xFF93A4B3;
	private static final int SLOT_BORDER = 0xFF3C5266;
	private static final int SLOT_FILL = 0xFF111820;

	private static final int BAR_X = 28;
	private static final int BAR_Y = 50;
	private static final int BAR_WIDTH = 120;
	private static final int BAR_HEIGHT = 8;

	private static final String KEY = "gui.journeymode.research.";

	private Button researchButton;

	public ResearchScreen(ResearchMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, ResearchMenu.IMAGE_WIDTH, ResearchMenu.IMAGE_HEIGHT);
		this.inventoryLabelY = ResearchMenu.INVENTORY_Y - 11;
	}

	private static Component tr(String key) {
		return Component.translatable(KEY + key);
	}

	@Override
	protected void init() {
		super.init();
		this.researchButton = this.addRenderableWidget(Button.builder(tr("button"), button ->
				this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, ResearchMenu.RESEARCH_BUTTON))
				.bounds(this.leftPos + 48, this.topPos + 76, 80, 20).build());
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractBackground(graphics, mouseX, mouseY, a);
		int x0 = this.leftPos;
		int y0 = this.topPos;
		int x1 = x0 + this.imageWidth;
		int y1 = y0 + this.imageHeight;

		graphics.fill(x0, y0, x1, y1, PANEL_OUTER);
		graphics.fill(x0 + 3, y0 + 3, x1 - 3, y1 - 3, PANEL_INNER);
		graphics.outline(x0, y0, this.imageWidth, this.imageHeight, ACCENT);
		graphics.outline(x0 + 2, y0 + 2, this.imageWidth - 4, this.imageHeight - 4, ACCENT_DARK);

		for (Slot slot : this.menu.slots) {
			int sx = x0 + slot.x;
			int sy = y0 + slot.y;
			if (slot.index == ResearchMenu.RESEARCH_SLOT) {
				graphics.outline(sx - 3, sy - 3, 22, 22, ACCENT_DARK);
			}
			graphics.fill(sx - 1, sy - 1, sx + 17, sy + 17, SLOT_BORDER);
			graphics.fill(sx, sy, sx + 16, sy + 16, SLOT_FILL);
		}

		// Progress bar.
		int bx = x0 + BAR_X;
		int by = y0 + BAR_Y;
		graphics.fill(bx - 1, by - 1, bx + BAR_WIDTH + 1, by + BAR_HEIGHT + 1, SLOT_BORDER);
		graphics.fill(bx, by, bx + BAR_WIDTH, by + BAR_HEIGHT, SLOT_FILL);
		if (this.menu.required() > 0) {
			int filled = BAR_WIDTH * Math.min(this.menu.progress(), this.menu.required()) / this.menu.required();
			graphics.fill(bx, by, bx + filled, by + BAR_HEIGHT, this.menu.isResearched() ? DONE : ACCENT);
		}

		this.researchButton.active = this.menu.hasItem() && !this.menu.isResearched();
	}

	/** Drawn relative to the panel's top left corner. */
	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		graphics.centeredText(this.font, this.title, this.imageWidth / 2, 6, ACCENT);
		graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, TEXT_DIM, false);

		int center = this.imageWidth / 2;
		if (!this.menu.hasItem()) {
			graphics.centeredText(this.font, tr("place_item"), center, BAR_Y + BAR_HEIGHT + 4, TEXT_DIM);
		} else if (this.menu.isResearched()) {
			graphics.centeredText(this.font, tr("researched"), center, BAR_Y + BAR_HEIGHT + 4, DONE);
		} else {
			graphics.centeredText(this.font, Component.literal(this.menu.progress() + " / " + this.menu.required()),
					center, BAR_Y + BAR_HEIGHT + 4, TEXT);
		}
		graphics.centeredText(this.font, Component.translatable(KEY + "total", this.menu.researchedTotal()), center, 100, TEXT_DIM);
	}
}