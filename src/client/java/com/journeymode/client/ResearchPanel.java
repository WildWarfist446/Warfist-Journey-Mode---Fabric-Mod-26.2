package com.journeymode.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.IntFunction;
import java.util.function.ToIntFunction;

import com.journeymode.JourneyMode;
import com.journeymode.client.mixin.AbstractContainerScreenAccessor;
import com.journeymode.network.GodModeTogglePayload;
import com.journeymode.network.MobSpawnSetPayload;
import com.journeymode.network.RequestItemPayload;
import com.journeymode.network.ResearchCarriedPayload;
import com.journeymode.network.ResearchSlotPayload;
import com.journeymode.network.TimeSetPayload;
import com.journeymode.network.WeatherSetPayload;
import com.journeymode.network.TrashClickPayload;
import com.journeymode.research.ItemCategory;
import com.journeymode.research.ResearchCosts;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The Journey Mode panels beside the survival inventory, on the left so they stay clear of JEI on
 * the right. Two buttons on the inventory's left edge each show or hide a panel (only one is
 * open at a time):
 * <ul>
 *   <li><b>Research</b>: with an item on the cursor, click the panel to sacrifice it; the panel
 *   shows how far along that item is.</li>
 *   <li><b>Items</b>: a searchable, scrolling list of everything researched. Clicks work like the
 *   creative menu's item list (see {@code ResearchManager.giveItem}).</li>
 * </ul>
 * Everything is keyed by the item registry, so items added by other mods work like vanilla ones.
 * Drawn with flat fills, no textures. The panels are drawn in the screen's background pass so
 * the search box, an ordinary widget, draws on top of them.
 */
public final class ResearchPanel {
	private enum Mode {
		NONE, RESEARCH, ITEMS, TIME, WEATHER, MOBS, GOD
	}

	// Steampunk palette: blackened iron, aged brass and bronze, dark leather, parchment text.
	static final int PANEL_OUTER = 0xFF1A110B;
	static final int PANEL_INNER = 0xF03A281A;
	static final int ACCENT = 0xFFD9A441;
	static final int ACCENT_DARK = 0xFF8C5A26;
	static final int BRASS_LIGHT = 0xFFF3D88A;
	private static final int DONE = 0xFF8FD08A;
	private static final int TEXT = 0xFFF5E6C4;
	static final int TEXT_DIM = 0xFFB89B6E;
	private static final int SLOT_BORDER = 0xFF6B4724;
	static final int SLOT_FILL = 0xFF181009;
	private static final int HOVER = 0x40FFFFFF;

	/** A small cog, drawn in the corners of the panels' title bars. */
	private static final String[] GEAR = {
			".X.X.X.",
			"XXXXXXX",
			".XX.XX.",
			"XX...XX",
			".XX.XX.",
			"XXXXXXX",
			".X.X.X."};

	private static final int BUTTON_SIZE = 20;
	private static final int BUTTON_GAP = 2;
	/** Six buttons and the trash slot have to fit down the inventory's height, so they sit just 2 pixels apart. */
	private static final int BUTTON_SPACING = 22;
	/** The tab buttons: research, items, time, weather, mob spawns and God Mode. */
	private static final int TAB_BUTTONS = 6;
	private static final int PANEL_GAP = 4;
	/** The recipe book's width, including its border. */
	private static final int RECIPE_BOOK_WIDTH = 148;
	private static final int PANEL_WIDTH = 124;
	/** The item list's scroll bar is a thin strip just right of the grid. */
	private static final int SCROLLBAR_WIDTH = 3;
	private static final int SCROLLBAR_GAP = 1;
	private static final int RESEARCH_HEIGHT = 100;

	/** The Time and Weather panels are the same: a title, a slider with labels, and room for a note. */
	private static final int SLIDER_HEIGHT = 62;
	/** A slider that this player cannot move also has a line below the labels that says so. */
	private static final int SLIDER_HEIGHT_WITH_NOTE = 68;
	/** Where the slider's first stop is, from the panel's left edge and top. */
	private static final int SLIDER_TRACK_OFFSET_X = 26;
	private static final int SLIDER_TRACK_OFFSET_Y = 36;
	/** The four times the time slider stops at, as /time set day, noon, night and midnight (in ticks). */
	private static final int[] TIME_TICKS = {1000, 6000, 13000, 18000};
	/** Room for the title, tab row and search box above the grid (see GRID_OFFSET_Y), seven rows of cells, and a margin. */
	private static final int ITEMS_HEIGHT = 56 + 7 * 18 + 8;

	private static final int GRID_COLUMNS = 6;
	private static final int GRID_ROWS = 7;
	private static final int CELL = 18;
	private static final int GRID_OFFSET_X = 8;
	private static final int GRID_OFFSET_Y = 56;
	private static final int TAB_OFFSET_Y = 18;
	private static final int SEARCH_OFFSET_Y = 38;
	private static final int SEARCH_HEIGHT = 14;

	private static final String KEY = "gui.journeymode.research.";

	/**
	 * A button icon: the part of a square texture to draw. The art is a small picture in the middle
	 * of a larger canvas, so only that part (u, v, width, height) is cut out, then drawn at
	 * drawWidth x drawHeight (the same size for no scaling).
	 */
	private record Icon(Identifier texture, int textureSize, int u, int v, int width, int height, int drawWidth, int drawHeight) {
	}

	/** The research button's icon, sprites/research-menu-icon.png: a 10 x 12 picture on a 32 x 32 canvas, drawn as is. */
	private static final Icon RESEARCH_ICON = new Icon(JourneyMode.id("textures/gui/research_icon.png"), 32, 11, 8, 10, 12, 10, 12);

	/**
	 * The item list button's icon, sprites/item-menu-icon.png: a 23 x 25 picture on a 32 x 32 canvas,
	 * drawn at about half size so it is close to the research icon's size.
	 */
	private static final Icon ITEMS_ICON = new Icon(JourneyMode.id("textures/gui/item_icon.png"), 32, 5, 3, 23, 25, 12, 13);

	/**
	 * The trash slot's icon, sprites/trash-can-icon.png: a 14 x 15 picture on a 32 x 32 canvas,
	 * drawn smaller so it is close to the research icon's size.
	 */
	private static final Icon TRASH_ICON = new Icon(JourneyMode.id("textures/gui/trash_icon.png"), 32, 9, 7, 14, 15, 10, 11);

	/** The God Mode button's icon, sprites/god-mode-icon.png: a 16 x 15 picture on a 32 x 32 canvas, drawn at about two thirds size, like the other icons. */
	private static final Icon GOD_ICON = new Icon(JourneyMode.id("textures/gui/god_mode_icon.png"), 32, 8, 8, 16, 15, 10, 9);

	/** The Weather button's icon, sprites/weather-tab-icon.png: a 22 x 24 picture on a 32 x 32 canvas, drawn at half size. */
	private static final Icon WEATHER_ICON = new Icon(JourneyMode.id("textures/gui/weather_icon.png"), 32, 5, 4, 22, 24, 11, 12);

	private static Mode mode = Mode.NONE;
	/** The first visible row of the item list. */
	private static int scrollRow;
	private static String searchText = "";
	/** The item list's selected tab. */
	private static ItemCategory category = ItemCategory.ALL;
	/** Whether the press of the mouse button now held was used by the panels, so its release must be ignored too. */
	private static boolean swallowRelease;
	/** Whether the scroll bar is being dragged. */
	private static boolean draggingScrollbar;
	/**
	 * A slider with a few stops that it snaps to. The knob follows the mouse while dragged and the
	 * change is sent when the button is released. Otherwise it rests where it was left, and is read
	 * from the world when the panel is opened (resting is -1 until then); following the world live
	 * made it jump around.
	 */
	private static final class Slider {
		final String keyPrefix;
		final String[] keys;
		final int spacing;
		final IntFunction<CustomPacketPayload> payload;
		final ToIntFunction<Minecraft> current;
		/** Whether only operators can move it (the time and weather); the rest are for every player. */
		final boolean opOnly;
		/** Whether only every other stop is labelled (the odd numbers stay blank unless the knob is on them). */
		boolean sparseLabels;
		boolean dragging;
		int stop;
		int resting = -1;

		/** @param keyPrefix the translation key prefix for the stop labels, or null to show {@code keys} as they are */
		Slider(String keyPrefix, String[] keys, int spacing, boolean opOnly, IntFunction<CustomPacketPayload> payload,
				ToIntFunction<Minecraft> current) {
			this.keyPrefix = keyPrefix;
			this.keys = keys;
			this.spacing = spacing;
			this.opOnly = opOnly;
			this.payload = payload;
			this.current = current;
		}

		int stops() {
			return this.keys.length;
		}

		int width() {
			return this.spacing * (this.stops() - 1);
		}
	}

	private static final Slider TIME_SLIDER = new Slider("gui.journeymode.time.", new String[] {"day", "noon", "night", "midnight"},
			24, true, TimeSetPayload::new, ResearchPanel::currentTimeStop);
	private static final Slider WEATHER_SLIDER = new Slider("gui.journeymode.weather.", new String[] {"clear", "rain", "thunder"},
			36, true, WeatherSetPayload::new, ResearchPanel::currentWeatherStop);
	/** Mob spawning from 0x (none) through 1x (normal) to 10x around the player; each stop is its own number. */
	private static final Slider MOB_SLIDER = new Slider(null, new String[] {"0x", "1x", "2x", "3x", "4x", "5x", "6x", "7x", "8x", "9x", "10x"},
			7, false, MobSpawnSetPayload::new, client -> ResearchClientState.mobSpawns());
	private static final Slider[] SLIDERS = {TIME_SLIDER, WEATHER_SLIDER, MOB_SLIDER};

	static {
		MOB_SLIDER.sparseLabels = true;
	}
	/** The current screen's search box, which is only shown and used while the item list is open. */
	private static EditBox searchBox;

	// The researched items matching the search, worked out again only when either changes.
	private static List<Item> shown = List.of();
	private static List<Item> shownSource;
	private static String shownQuery = "";
	private static ItemCategory shownCategory = ItemCategory.ALL;

	private ResearchPanel() {
	}

	public static void initialize() {
		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			if (!(screen instanceof InventoryScreen inventory)) {
				return;
			}
			for (Slider slider : SLIDERS) {
				slider.resting = -1;
			}
			searchBox = new EditBox(client.font, 0, 0, GRID_COLUMNS * CELL, SEARCH_HEIGHT, Component.translatable(KEY + "search"));
			searchBox.setHint(Component.translatable(KEY + "search").withStyle(ChatFormatting.DARK_GRAY));
			searchBox.setMaxLength(50);
			searchBox.setValue(searchText);
			searchBox.setResponder(text -> {
				searchText = text;
				scrollRow = 0;
			});
			Screens.getWidgets(screen).add(searchBox);

			// The recipe book button is drawn by the game, so ours goes over it once everything else is drawn.
			ScreenEvents.afterExtract(screen).register((s, graphics, mouseX, mouseY, delta) -> {
				if (ResearchClientState.journeyEnabled()) {
					InventoryTheme.drawRecipeButton(client, inventory, graphics, mouseX, mouseY);
				}
			});
			ScreenEvents.afterBackground(screen).register((s, graphics, mouseX, mouseY, delta) ->
					extract(client, inventory, graphics, mouseX, mouseY));
			ScreenMouseEvents.allowMouseClick(screen).register((s, event) -> {
				swallowRelease = click(client, inventory, event.x(), event.y(), event.button(), event.hasShiftDown());
				return !swallowRelease;
			});
			// Letting go of a button we used the press of would make the inventory throw away the item
			// now on the cursor, because the panels are outside its window.
			ScreenMouseEvents.allowMouseRelease(screen).register((s, event) -> {
				boolean swallow = swallowRelease;
				swallowRelease = false;
				draggingScrollbar = false;
				for (Slider slider : SLIDERS) {
					if (slider.dragging) {
						slider.dragging = false;
						slider.resting = slider.stop;
						if (canUse(client, slider)) {
							ClientPlayNetworking.send(slider.payload.apply(slider.stop));
							client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
						}
					}
				}
				return !swallow;
			});
			ScreenMouseEvents.allowMouseDrag(screen).register((s, event, horizontal, vertical) -> {
				if (draggingScrollbar) {
					scrollTo(inventory, event.y());
					return false;
				}
				for (Slider slider : SLIDERS) {
					if (slider.dragging) {
						slider.stop = stopAt(slider, inventory, event.x());
						return false;
					}
				}
				return true;
			});
			ScreenMouseEvents.allowMouseScroll(screen).register((s, mouseX, mouseY, horizontal, vertical) ->
					!scroll(inventory, mouseX, mouseY, vertical));
		});
	}

	// ---------------------------------------------------------------- searching

	/** The researched items that match the search box, in the game's item order. */
	private static List<Item> shownItems() {
		List<Item> source = ResearchClientState.listedItems();
		String query = searchText.strip().toLowerCase(Locale.ROOT);
		if (source != shownSource || !query.equals(shownQuery) || category != shownCategory) {
			shownSource = source;
			shownQuery = query;
			shownCategory = category;
			List<Item> matches = new ArrayList<>();
			for (Item item : source) {
				if (category.matches(item) && (query.isEmpty() || matches(item, query))) {
					matches.add(item);
				}
			}
			shown = List.copyOf(matches);
		}
		return shown;
	}

	/**
	 * Whether the item matches the query: every word of it appears in the item's name or registry
	 * name. A query starting with "@" matches the mod that added the item instead (by id or name).
	 */
	private static boolean matches(Item item, String query) {
		Identifier id = BuiltInRegistries.ITEM.getKey(item);
		if (query.startsWith("@")) {
			String mod = query.substring(1).strip();
			return id.getNamespace().contains(mod) || modName(id.getNamespace()).toLowerCase(Locale.ROOT).contains(mod);
		}
		String text = (new ItemStack(item).getHoverName().getString() + " " + id.getPath()).toLowerCase(Locale.ROOT);
		for (String word : query.split("\\s+")) {
			if (!text.contains(word)) {
				return false;
			}
		}
		return true;
	}

	/** The display name of the mod with this id, or the id itself if it has none. */
	private static String modName(String modId) {
		return FabricLoader.getInstance().getModContainer(modId).map(mod -> mod.getMetadata().getName()).orElse(modId);
	}

	// ---------------------------------------------------------------- geometry

	/**
	 * The left edge of everything on the inventory's left: the inventory itself, or the recipe book
	 * when it is open (it shifts the inventory right, and sits just left of it).
	 */
	private static int leftEdge(AbstractContainerScreen<?> screen) {
		AbstractContainerScreenAccessor a = (AbstractContainerScreenAccessor) screen;
		return a.journeymode$leftPos() - (recipeBookOpen(screen) ? RECIPE_BOOK_WIDTH : 0);
	}

	/** Whether the recipe book is open: it shifts the inventory right of the screen's centre. */
	private static boolean recipeBookOpen(AbstractContainerScreen<?> screen) {
		AbstractContainerScreenAccessor a = (AbstractContainerScreenAccessor) screen;
		return a.journeymode$leftPos() > (screen.width - a.journeymode$imageWidth()) / 2;
	}

	private static int buttonX(AbstractContainerScreen<?> screen) {
		return leftEdge(screen) - BUTTON_GAP - BUTTON_SIZE;
	}

	/** The top of the button for a mode: research first, the item list under it. */
	private static int buttonY(AbstractContainerScreen<?> screen, Mode forMode) {
		int slot = forMode == Mode.GOD ? 5 : forMode == Mode.MOBS ? 4 : forMode == Mode.WEATHER ? 3 : forMode == Mode.TIME ? 2
				: forMode == Mode.ITEMS ? 1 : 0;
		return buttonsTop(screen) + slot * BUTTON_SPACING;
	}

	/** The top of the first button: the whole column, the tab buttons and the trash slot under them, is centred vertically on the inventory. */
	private static int buttonsTop(AbstractContainerScreen<?> screen) {
		AbstractContainerScreenAccessor a = (AbstractContainerScreenAccessor) screen;
		int columnHeight = TAB_BUTTONS * BUTTON_SPACING + BUTTON_SIZE;
		return a.journeymode$topPos() + (a.journeymode$imageHeight() - columnHeight) / 2;
	}

	/** The trash slot sits in the same column as the buttons, right under the last one. */
	private static int trashY(AbstractContainerScreen<?> screen) {
		return buttonsTop(screen) + TAB_BUTTONS * BUTTON_SPACING;
	}

	private static int panelX(AbstractContainerScreen<?> screen) {
		return buttonX(screen) - PANEL_GAP - PANEL_WIDTH;
	}

	/** The panel is centred vertically on the inventory, so the taller item list sticks out equally above and below. */
	private static int panelY(AbstractContainerScreen<?> screen) {
		AbstractContainerScreenAccessor a = (AbstractContainerScreenAccessor) screen;
		return a.journeymode$topPos() + (a.journeymode$imageHeight() - panelHeight()) / 2;
	}

	private static int panelHeight() {
		return mode == Mode.ITEMS ? ITEMS_HEIGHT : openSlider() != null
				? (canUse(Minecraft.getInstance(), openSlider()) ? SLIDER_HEIGHT : SLIDER_HEIGHT_WITH_NOTE) : RESEARCH_HEIGHT;
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

	/** The tab under the mouse, or null. The tabs sit in a row above the search box, one cell wide each. */
	private static ItemCategory tabAt(AbstractContainerScreen<?> screen, double mx, double my) {
		int tabY = panelY(screen) + TAB_OFFSET_Y;
		if (mode != Mode.ITEMS || my < tabY || my >= tabY + CELL || mx < gridX(screen)) {
			return null;
		}
		int index = (int) (mx - gridX(screen)) / CELL;
		return index < ItemCategory.values().length ? ItemCategory.values()[index] : null;
	}

	private static int scrollbarX(AbstractContainerScreen<?> screen) {
		return gridX(screen) + GRID_COLUMNS * CELL + SCROLLBAR_GAP;
	}

	/** The scroll bar thumb's height: the share of the list that is visible, with a minimum so it stays grabbable. */
	private static int thumbHeight() {
		int trackHeight = GRID_ROWS * CELL;
		return Math.min(trackHeight, Math.max(10, trackHeight * GRID_ROWS / Math.max(GRID_ROWS, totalRows())));
	}

	/** Scrolls so the thumb's centre follows the mouse at screen height {@code my}. */
	private static void scrollTo(AbstractContainerScreen<?> screen, double my) {
		int travel = GRID_ROWS * CELL - thumbHeight();
		if (maxScrollRow() <= 0 || travel <= 0) {
			return;
		}
		double fraction = (my - gridY(screen) - thumbHeight() / 2.0) / travel;
		scrollRow = Math.clamp(Math.round(fraction * maxScrollRow()), 0, maxScrollRow());
	}

	private static int totalRows() {
		return (shownItems().size() + GRID_COLUMNS - 1) / GRID_COLUMNS;
	}

	private static int maxScrollRow() {
		return Math.max(0, totalRows() - GRID_ROWS);
	}

	/** The index into the shown items of the grid cell under the mouse, or -1. */
	private static int itemAt(AbstractContainerScreen<?> screen, double mx, double my) {
		int gx = gridX(screen);
		int gy = gridY(screen);
		if (!inside(mx, my, gx, gy, GRID_COLUMNS * CELL, GRID_ROWS * CELL)) {
			return -1;
		}
		int index = (scrollRow + (int) (my - gy) / CELL) * GRID_COLUMNS + (int) (mx - gx) / CELL;
		return index < shownItems().size() ? index : -1;
	}

	// ---------------------------------------------------------------- drawing

	/** A filled rectangle with its corners cut off {@code cut} pixels, so edges look worn rather than sharp. */
	static void chamfer(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int cut, int color) {
		for (int i = 0; i < cut; i++) {
			int inset = cut - i;
			graphics.fill(x + inset, y + i, x + w - inset, y + i + 1, color);
			graphics.fill(x + inset, y + h - i - 1, x + w - inset, y + h - i, color);
		}
		graphics.fill(x, y + cut, x + w, y + h - cut, color);
	}

	/** A sunken slot: a dark edge on the top and left, a lit edge on the bottom and right, and a dark fill. */
	static void recess(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
		graphics.fill(x, y, x + w, y + h, ACCENT_DARK);
		graphics.fill(x, y, x + w - 1, y + h - 1, PANEL_OUTER);
		graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, SLOT_FILL);
	}

	/** A small square plate: iron edge, brass rim, then {@code fill}. Used for buttons and slots. */
	private static void plate(GuiGraphicsExtractor graphics, int x, int y, int size, int fill) {
		chamfer(graphics, x, y, size, size, 2, PANEL_OUTER);
		chamfer(graphics, x + 1, y + 1, size - 2, size - 2, 1, ACCENT);
		graphics.fill(x + 2, y + 2, x + size - 2, y + size - 2, fill);
		graphics.fill(x + 2, y + 1, x + size - 2, y + 2, BRASS_LIGHT);
	}

	/** Draws the {@link #GEAR} cog with its top left corner at (x, y). */
	static void gear(GuiGraphicsExtractor graphics, int x, int y) {
		for (int row = 0; row < GEAR.length; row++) {
			for (int col = 0; col < GEAR[row].length(); col++) {
				if (GEAR[row].charAt(col) == 'X') {
					graphics.fill(x + col, y + row, x + col + 1, y + row + 1, row + col < 4 ? ACCENT : ACCENT_DARK);
				}
			}
		}
	}

	private static void extract(Minecraft client, InventoryScreen screen, GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		Font font = client.font;
		// With the recipe book open, everything is hidden (and ignores clicks) until it is closed again.
		if (recipeBookOpen(screen) || !ResearchClientState.journeyEnabled()) {
			searchBox.visible = false;
			searchBox.active = false;
			searchBox.setFocused(false);
			draggingScrollbar = false;
			return;
		}
		scrollRow = Math.clamp(scrollRow, 0, maxScrollRow());
		drawButton(font, screen, graphics, mouseX, mouseY, Mode.RESEARCH, "R", RESEARCH_ICON, "title");
		drawButton(font, screen, graphics, mouseX, mouseY, Mode.ITEMS, "I", ITEMS_ICON, "items_title");
		drawButton(font, screen, graphics, mouseX, mouseY, Mode.TIME, "T", null, "time_title");
		drawButton(font, screen, graphics, mouseX, mouseY, Mode.WEATHER, "W", WEATHER_ICON, "weather_title");
		drawButton(font, screen, graphics, mouseX, mouseY, Mode.MOBS, "M", null, "mobs_title");
		drawButton(font, screen, graphics, mouseX, mouseY, Mode.GOD, "G", GOD_ICON, "god_title");
		drawTrash(font, screen, graphics, mouseX, mouseY);

		// The search box only exists while the item list is open.
		boolean listOpen = mode == Mode.ITEMS;
		searchBox.visible = listOpen;
		searchBox.active = listOpen;
		if (!listOpen) {
			searchBox.setFocused(false);
		}
		if (mode == Mode.NONE) {
			return;
		}

		int px = panelX(screen);
		int py = panelY(screen);
		int height = panelHeight();
		// An iron plate with a brass rim, with a cog at each end of the title bar.
		chamfer(graphics, px, py, PANEL_WIDTH, height, 3, PANEL_OUTER);
		chamfer(graphics, px + 1, py + 1, PANEL_WIDTH - 2, height - 2, 2, ACCENT);
		chamfer(graphics, px + 2, py + 2, PANEL_WIDTH - 4, height - 4, 2, ACCENT_DARK);
		chamfer(graphics, px + 3, py + 3, PANEL_WIDTH - 6, height - 6, 1, PANEL_INNER);
		graphics.fill(px + 4, py + 1, px + PANEL_WIDTH - 4, py + 2, BRASS_LIGHT);
		gear(graphics, px + 5, py + 6);
		gear(graphics, px + PANEL_WIDTH - 12, py + 6);
		graphics.centeredText(font, Component.translatable(KEY + (mode == Mode.ITEMS ? "items_title" : mode == Mode.TIME ? "time_title" : mode == Mode.WEATHER ? "weather_title" : mode == Mode.MOBS ? "mobs_title" : "title")),
				px + PANEL_WIDTH / 2, py + 7, ACCENT);

		if (mode == Mode.RESEARCH) {
			extractResearch(font, screen, graphics, mouseX, mouseY, px, py);
		} else if (openSlider() != null) {
			extractSlider(client, openSlider(), graphics, px, py);
		} else {
			searchBox.setX(gridX(screen));
			searchBox.setY(py + SEARCH_OFFSET_Y);
			extractItems(client, screen, graphics, mouseX, mouseY, px, py);
		}
	}

	/**
	 * A toggle button: a flat square with an icon, or a placeholder letter until that button's icon
	 * is made.
	 *
	 * @param icon the button's icon, or null to draw {@code mark} instead
	 */
	private static void drawButton(Font font, InventoryScreen screen, GuiGraphicsExtractor graphics, int mouseX, int mouseY,
			Mode forMode, String mark, Icon icon, String tooltipKey) {
		int bx = buttonX(screen);
		int by = buttonY(screen, forMode);
		// The God Mode button is a switch, so it looks pressed while God Mode is on; the others while their panel is open.
		boolean active = forMode == Mode.GOD ? ResearchClientState.godMode() : mode == forMode;
		plate(graphics, bx, by, BUTTON_SIZE, active ? ACCENT_DARK : PANEL_INNER);
		if (icon != null) {
			graphics.blit(RenderPipelines.GUI_TEXTURED, icon.texture(), bx + (BUTTON_SIZE - icon.drawWidth()) / 2,
					by + (BUTTON_SIZE - icon.drawHeight()) / 2, icon.u(), icon.v(), icon.drawWidth(), icon.drawHeight(),
					icon.width(), icon.height(), icon.textureSize(), icon.textureSize());
		} else if (forMode == Mode.TIME || forMode == Mode.MOBS) {
			graphics.fakeItem(new ItemStack(forMode == Mode.TIME ? Items.CLOCK : Items.CREEPER_HEAD), bx + 2, by + 2);
		} else {
			graphics.centeredText(font, mark, bx + BUTTON_SIZE / 2, by + 6, active ? TEXT : ACCENT);
		}
		if (inside(mouseX, mouseY, bx, by, BUTTON_SIZE, BUTTON_SIZE)) {
			graphics.fill(bx, by, bx + BUTTON_SIZE, by + BUTTON_SIZE, HOVER);
			graphics.setTooltipForNextFrame(font, forMode == Mode.GOD
					? Component.translatable("gui.journeymode.god.state", Component.translatable(active ? "options.on" : "options.off"))
					: Component.translatable(KEY + tooltipKey), mouseX, mouseY);
		}
	}

	/** The trash slot: shows the trash can icon while empty, otherwise the stack waiting in it. It opens no panel. */
	private static void drawTrash(Font font, InventoryScreen screen, GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		int bx = buttonX(screen);
		int by = trashY(screen);
		ItemStack trashed = ResearchClientState.trash();
		plate(graphics, bx, by, BUTTON_SIZE, PANEL_INNER);
		if (trashed.isEmpty()) {
			graphics.blit(RenderPipelines.GUI_TEXTURED, TRASH_ICON.texture(), bx + (BUTTON_SIZE - TRASH_ICON.drawWidth()) / 2,
					by + (BUTTON_SIZE - TRASH_ICON.drawHeight()) / 2, TRASH_ICON.u(), TRASH_ICON.v(), TRASH_ICON.drawWidth(),
					TRASH_ICON.drawHeight(), TRASH_ICON.width(), TRASH_ICON.height(), TRASH_ICON.textureSize(), TRASH_ICON.textureSize());
		} else {
			graphics.fakeItem(trashed, bx + 2, by + 2);
			graphics.itemDecorations(font, trashed, bx + 2, by + 2);
		}
		if (inside(mouseX, mouseY, bx, by, BUTTON_SIZE, BUTTON_SIZE)) {
			graphics.fill(bx, by, bx + BUTTON_SIZE, by + BUTTON_SIZE, HOVER);
		}
	}

	// ---------------------------------------------------------------- sliders (time and weather)

	private static int trackX(AbstractContainerScreen<?> screen) {
		return panelX(screen) + SLIDER_TRACK_OFFSET_X;
	}

	private static int trackY(AbstractContainerScreen<?> screen) {
		return panelY(screen) + SLIDER_TRACK_OFFSET_Y;
	}

	/** The slider shown by the open panel, or null. */
	private static Slider openSlider() {
		return mode == Mode.TIME ? TIME_SLIDER : mode == Mode.WEATHER ? WEATHER_SLIDER : mode == Mode.MOBS ? MOB_SLIDER : null;
	}

	/** Whether this player may move the slider: anyone, unless it is one for operators only. */
	private static boolean canUse(Minecraft client, Slider slider) {
		return !slider.opOnly || isOp(client);
	}

	/** Whether this player is an operator, which is what the server requires to change the time or weather. */
	private static boolean isOp(Minecraft client) {
		return client.player != null && client.player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
	}

	/** The slider's stop nearest the mouse. */
	private static int stopAt(Slider slider, AbstractContainerScreen<?> screen, double mx) {
		return Math.clamp(Math.round((mx - trackX(screen)) / slider.spacing), 0, slider.stops() - 1);
	}

	/** The stop for the weather now: clear, rain or thunder. */
	private static int currentWeatherStop(Minecraft client) {
		return client.level == null ? 0 : client.level.isThundering() ? 2 : client.level.isRaining() ? 1 : 0;
	}

	/** The stop whose time of day is nearest the world's current time. */
	private static int currentTimeStop(Minecraft client) {
		if (client.level == null) {
			return 0;
		}
		long now = Math.floorMod(client.level.getOverworldClockTime(), 24000L);
		int best = 0;
		long bestDistance = Long.MAX_VALUE;
		for (int i = 0; i < TIME_TICKS.length; i++) {
			long distance = Math.abs(now - TIME_TICKS[i]);
			distance = Math.min(distance, 24000L - distance);
			if (distance < bestDistance) {
				bestDistance = distance;
				best = i;
			}
		}
		return best;
	}

	/**
	 * A slider panel: a brass knob on a groove with a few stops. Operators drag it and the change is
	 * made when they let go. Everyone else sees it dimmed.
	 */
	private static void extractSlider(Minecraft client, Slider slider, GuiGraphicsExtractor graphics, int px, int py) {
		Font font = client.font;
		boolean op = canUse(client, slider);
		int trackX = px + SLIDER_TRACK_OFFSET_X;
		int trackY = py + SLIDER_TRACK_OFFSET_Y;
		if (slider.resting < 0) {
			slider.resting = slider.current.applyAsInt(client);
		}
		int stop = slider.dragging ? slider.stop : slider.resting;

		recess(graphics, trackX - 6, trackY - 3, slider.width() + 12, 7);
		for (int i = 0; i < slider.stops(); i++) {
			int x = trackX + i * slider.spacing;
			graphics.fill(x, trackY - 5, x + 1, trackY + 6, op ? ACCENT : ACCENT_DARK);
			// With many stops, only every other label (the even numbers) is shown, plus the one the knob is on.
			if (slider.sparseLabels && i % 2 == 0 && i != stop) {
				continue;
			}
			// Labels alternate above and below the groove so the longer names never run into each other.
			int labelY = i % 2 == 0 ? trackY - 17 : trackY + 9;
			graphics.centeredText(font, slider.keyPrefix == null ? Component.literal(slider.keys[i])
					: Component.translatable(slider.keyPrefix + slider.keys[i]), x, labelY,
					i == stop ? (op ? ACCENT : TEXT_DIM) : TEXT_DIM);
		}

		int knobX = trackX + stop * slider.spacing;
		int fill = op ? ACCENT : ACCENT_DARK;
		chamfer(graphics, knobX - 4, trackY - 8, 9, 16, 1, PANEL_OUTER);
		graphics.fill(knobX - 3, trackY - 7, knobX + 4, trackY + 7, fill);
		graphics.fill(knobX - 3, trackY - 7, knobX + 4, trackY - 6, op ? BRASS_LIGHT : fill);
		graphics.fill(knobX, trackY - 5, knobX + 1, trackY + 5, PANEL_OUTER);

		if (!op) {
			graphics.centeredText(font, Component.translatable("gui.journeymode.time.ops_only"), px + PANEL_WIDTH / 2,
					py + SLIDER_HEIGHT_WITH_NOTE - 13, 0xFFD9835A);
		}
	}

	private static void extractResearch(Font font, InventoryScreen screen, GuiGraphicsExtractor graphics, int mouseX, int mouseY,
			int px, int py) {
		// The drop spot.
		ItemStack carried = screen.getMenu().getCarried();
		int dx = dropX(screen);
		int dy = dropY(screen);
		plate(graphics, dx - 3, dy - 3, 24, ACCENT_DARK);
		recess(graphics, dx - 1, dy - 1, 20, 20);
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
		recess(graphics, barX - 1, barY - 1, barWidth + 2, 10);
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
	}

	private static void extractItems(Minecraft client, InventoryScreen screen, GuiGraphicsExtractor graphics, int mouseX, int mouseY,
			int px, int py) {
		Font font = client.font;
		List<Item> items = shownItems();
		int gx = gridX(screen);
		int gy = gridY(screen);
		int hovered = itemAt(screen, mouseX, mouseY);

		ItemCategory hoveredTab = tabAt(screen, mouseX, mouseY);
		for (ItemCategory tab : ItemCategory.values()) {
			int x = gx + tab.ordinal() * CELL;
			int y = py + TAB_OFFSET_Y;
			boolean selected = tab == category;
			if (selected) {
				// The chosen tab stands out like a raised brass plate; the others are sunk into the panel.
				graphics.fill(x, y, x + CELL, y + CELL, ACCENT);
				graphics.fill(x + 1, y + 1, x + CELL - 1, y + CELL - 1, ACCENT_DARK);
				graphics.fill(x + 1, y, x + CELL - 1, y + 1, BRASS_LIGHT);
			} else {
				recess(graphics, x, y, CELL, CELL);
			}
			graphics.fakeItem(new ItemStack(tab.icon()), x + 1, y + 1);
			if (tab == hoveredTab) {
				graphics.fill(x + 1, y + 1, x + CELL - 1, y + CELL - 1, HOVER);
				graphics.setTooltipForNextFrame(font, Component.translatable(KEY + "tab." + tab.key()), mouseX, mouseY);
			}
		}

		for (int row = 0; row < GRID_ROWS; row++) {
			for (int col = 0; col < GRID_COLUMNS; col++) {
				int x = gx + col * CELL;
				int y = gy + row * CELL;
				recess(graphics, x, y, CELL, CELL);
				int index = (scrollRow + row) * GRID_COLUMNS + col;
				if (index < items.size()) {
					graphics.fakeItem(new ItemStack(items.get(index)), x + 1, y + 1);
					if (index == hovered) {
						graphics.fill(x + 1, y + 1, x + CELL - 1, y + CELL - 1, HOVER);
					}
				}
			}
		}

		// The scroll bar is always shown; with nothing to scroll the thumb fills the track and is dimmed.
		int trackX = scrollbarX(screen);
		int trackHeight = GRID_ROWS * CELL;
		// A dark groove with a brass rod sliding in it: shaded like a cylinder, with collars at both ends and grip rings.
		graphics.fill(trackX, gy, trackX + SCROLLBAR_WIDTH, gy + trackHeight, PANEL_OUTER);
		int thumbHeight = thumbHeight();
		int thumbY = maxScrollRow() > 0 ? gy + (trackHeight - thumbHeight) * scrollRow / maxScrollRow() : gy;
		boolean scrollable = maxScrollRow() > 0;
		boolean overBar = inside(mouseX, mouseY, trackX - 1, gy, SCROLLBAR_WIDTH + 2, trackHeight);
		boolean lit = scrollable && (draggingScrollbar || overBar);
		int edge = !scrollable ? SLOT_BORDER : ACCENT_DARK;
		int body = !scrollable ? ACCENT_DARK : ACCENT;
		int shine = !scrollable ? ACCENT_DARK : lit ? 0xFFFFF2C0 : BRASS_LIGHT;
		graphics.fill(trackX, thumbY, trackX + 1, thumbY + thumbHeight, edge);
		graphics.fill(trackX + 1, thumbY, trackX + 2, thumbY + thumbHeight, shine);
		graphics.fill(trackX + 2, thumbY, trackX + 3, thumbY + thumbHeight, body);
		graphics.fill(trackX, thumbY, trackX + SCROLLBAR_WIDTH, thumbY + 1, PANEL_OUTER);
		graphics.fill(trackX, thumbY + thumbHeight - 1, trackX + SCROLLBAR_WIDTH, thumbY + thumbHeight, PANEL_OUTER);
		for (int ring = thumbY + thumbHeight / 2 - 2; ring <= thumbY + thumbHeight / 2 + 2 && thumbHeight >= 14; ring += 2) {
			graphics.fill(trackX, ring, trackX + SCROLLBAR_WIDTH, ring + 1, edge);
		}

		if (items.isEmpty()) {
			// Either nothing is researched yet, or nothing matches the search.
			String message = ResearchClientState.listedItems().isEmpty() ? "none_yet" : "no_matches";
			int y = gy + 20;
			for (FormattedCharSequence line : font.split(Component.translatable(KEY + message), GRID_COLUMNS * CELL - 12)) {
				graphics.centeredText(font, line, gx + GRID_COLUMNS * CELL / 2, y, TEXT_DIM);
				y += 10;
			}
		}

		if (hovered >= 0) {
			// The item's usual tooltip, then the mod that added it.
			ItemStack stack = new ItemStack(items.get(hovered));
			List<Component> tooltip = new ArrayList<>(Screen.getTooltipFromItem(client, stack));
			tooltip.add(Component.literal(modName(BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace()))
					.withStyle(ChatFormatting.BLUE, ChatFormatting.ITALIC));
			graphics.setComponentTooltipForNextFrame(font, tooltip, mouseX, mouseY);
		}
	}

	// ---------------------------------------------------------------- input

	/** Handles a click; returns true if the panels used it, so the inventory should ignore it. */
	private static boolean click(Minecraft client, InventoryScreen screen, double mx, double my, int button, boolean shift) {
		if (recipeBookOpen(screen) || !ResearchClientState.journeyEnabled()) {
			return false;
		}
		for (Mode forMode : new Mode[] {Mode.RESEARCH, Mode.ITEMS, Mode.TIME, Mode.WEATHER, Mode.MOBS, Mode.GOD}) {
			if (inside(mx, my, buttonX(screen), buttonY(screen, forMode), BUTTON_SIZE, BUTTON_SIZE)) {
				if (button == 0) {
					if (forMode == Mode.GOD) {
						// God Mode has no panel: the button itself is the switch.
						ClientPlayNetworking.send(new GodModeTogglePayload());
					} else {
						mode = mode == forMode ? Mode.NONE : forMode;
						// The sliders start from the world's current time and weather each time a panel is opened.
						for (Slider slider : SLIDERS) {
							slider.resting = -1;
						}
					}
					client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
				}
				return true;
			}
		}
		if (inside(mx, my, buttonX(screen), trashY(screen), BUTTON_SIZE, BUTTON_SIZE)) {
			if (button == 0 || button == 1) {
				ClientPlayNetworking.send(new TrashClickPayload());
				client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
			}
			return true;
		}
		// A click on the search box goes to the box itself.
		if (mode == Mode.ITEMS && inside(mx, my, searchBox.getX(), searchBox.getY(), searchBox.getWidth(), searchBox.getHeight())) {
			return false;
		}
		// Any other click takes the keyboard away from the search box.
		searchBox.setFocused(false);
		// With the research panel open, shift-clicking an item that still needs research sends it to the panel.
		if (mode == Mode.RESEARCH && shift && (button == 0 || button == 1)) {
			Slot slot = ((AbstractContainerScreenAccessor) screen).journeymode$hoveredSlot();
			if (slot != null && slot.hasItem() && slot.container == client.player.getInventory() && slot.getContainerSlot() < 36
					&& ResearchClientState.progress(slot.getItem().getItem()) < ResearchCosts.required(slot.getItem().getItem())) {
				ClientPlayNetworking.send(new ResearchSlotPayload(slot.index));
				client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
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
		} else if (openSlider() != null) {
			// Operators drag the slider and the change is made when they let go.
			Slider slider = openSlider();
			if (button == 0 && canUse(client, slider) && inside(mx, my, trackX(screen) - 8, trackY(screen) - 8, slider.width() + 16, 16)) {
				slider.dragging = true;
				slider.stop = stopAt(slider, screen, mx);
			}
		} else if (inside(mx, my, scrollbarX(screen) - 1, gridY(screen), SCROLLBAR_WIDTH + 2, GRID_ROWS * CELL)) {
			// Click or drag the scroll bar to move the list.
			if (button == 0) {
				draggingScrollbar = true;
				scrollTo(screen, my);
			}
		} else if (tabAt(screen, mx, my) != null) {
			if (button == 0) {
				category = tabAt(screen, mx, my);
				scrollRow = 0;
				client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
			}
		} else if (button >= 0 && button <= 2) {
			int index = itemAt(screen, mx, my);
			if (index >= 0) {
				Item item = shownItems().get(index);
				ClientPlayNetworking.send(new RequestItemPayload(BuiltInRegistries.ITEM.getKey(item), button, shift));
				client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
			}
		}
		return true;
	}

	/** Scrolls the item list when the mouse is over its panel; returns true if the scroll was used. */
	private static boolean scroll(InventoryScreen screen, double mx, double my, double amount) {
		if (recipeBookOpen(screen) || !ResearchClientState.journeyEnabled() || mode != Mode.ITEMS || !inside(mx, my, panelX(screen), panelY(screen), PANEL_WIDTH, panelHeight())) {
			return false;
		}
		if (amount != 0) {
			scrollRow = Math.clamp(scrollRow - (int) Math.signum(amount), 0, maxScrollRow());
		}
		return true;
	}
}
