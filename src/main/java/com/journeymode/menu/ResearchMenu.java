package com.journeymode.menu;

import com.journeymode.research.ResearchCosts;
import com.journeymode.research.ResearchData;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The research menu. Put an item in the research slot and press Research: the items are used up
 * and counted toward that item's cost. Once an item is fully researched, the player has
 * unlocked it. Progress is saved per player in {@link ResearchData}.
 *
 * <p>The server works out the numbers the screen shows and syncs them in data slots, so the
 * client never needs the saved data. The button works like the stonecutter's: the screen sends
 * {@code handleInventoryButtonClick(containerId, RESEARCH_BUTTON)}, which reaches
 * {@link #clickMenuButton} on the server.
 */
public class ResearchMenu extends AbstractContainerMenu {
	public static final int RESEARCH_BUTTON = 0;

	public static final int RESEARCH_SLOT = 0;
	private static final int INV_SLOT_START = 1;
	private static final int INV_SLOT_END = 28;
	private static final int HOTBAR_SLOT_END = 37;

	public static final int IMAGE_WIDTH = 176;
	public static final int IMAGE_HEIGHT = 208;
	public static final int SLOT_X = 80;
	public static final int SLOT_Y = 24;
	public static final int INVENTORY_X = 8;
	public static final int INVENTORY_Y = 124;

	private final Inventory playerInventory;
	private final DataSlot progress = DataSlot.standalone();
	private final DataSlot required = DataSlot.standalone();
	private final DataSlot researchedTotal = DataSlot.standalone();
	private final Slot researchSlot;

	private final Container input = new SimpleContainer(1) {
		@Override
		public void setChanged() {
			super.setChanged();
			ResearchMenu.this.slotsChanged(this);
		}
	};

	/** Client side: opened from the server's menu. */
	public ResearchMenu(int containerId, Inventory inventory) {
		super(ModMenus.RESEARCH, containerId);
		this.playerInventory = inventory;
		this.researchSlot = this.addSlot(new Slot(this.input, 0, SLOT_X, SLOT_Y));
		this.addStandardInventorySlots(inventory, INVENTORY_X, INVENTORY_Y);
		this.addDataSlot(this.progress);
		this.addDataSlot(this.required);
		this.addDataSlot(this.researchedTotal);
		this.refresh();
	}

	// For the screen

	public boolean hasItem() {
		return this.researchSlot.hasItem();
	}

	/** How much of the item in the slot has been researched so far. */
	public int progress() {
		return this.progress.get();
	}

	/** How many of the item in the slot are needed in total (0 with an empty slot). */
	public int required() {
		return this.required.get();
	}

	public boolean isResearched() {
		return this.hasItem() && this.progress() >= this.required();
	}

	/** How many different items this player has fully researched. */
	public int researchedTotal() {
		return this.researchedTotal.get();
	}

	// Logic

	@Override
	public boolean clickMenuButton(Player player, int buttonId) {
		if (buttonId != RESEARCH_BUTTON || !(player instanceof ServerPlayer serverPlayer)) {
			return false;
		}
		ItemStack stack = this.researchSlot.getItem();
		if (stack.isEmpty()) {
			return false;
		}
		Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
		int needed = ResearchCosts.required(stack.getItem());
		ResearchData data = ResearchData.get(serverPlayer.level().getServer());
		int missing = needed - data.progress(serverPlayer.getUUID(), id);
		if (missing <= 0) {
			return false;
		}
		int used = Math.min(missing, stack.getCount());
		data.add(serverPlayer.getUUID(), id, used, needed);
		this.researchSlot.remove(used);
		this.refresh();
		return true;
	}

	@Override
	public void slotsChanged(Container container) {
		this.refresh();
		super.slotsChanged(container);
	}

	/** Recalculates what the screen shows. Only the server has the saved data, so the client waits for the sync. */
	private void refresh() {
		if (!(this.playerInventory.player instanceof ServerPlayer player)) {
			return;
		}
		ResearchData data = ResearchData.get(player.level().getServer());
		ItemStack stack = this.researchSlot.getItem();
		if (stack.isEmpty()) {
			this.progress.set(0);
			this.required.set(0);
		} else {
			Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
			this.progress.set(data.progress(player.getUUID(), id));
			this.required.set(ResearchCosts.required(stack.getItem()));
		}
		this.researchedTotal.set(data.researchedCount(player.getUUID(), id ->
				BuiltInRegistries.ITEM.getOptional(id).map(item -> data.isResearched(player.getUUID(), id, item)).orElse(false)));
	}

	@Override
	public boolean stillValid(Player player) {
		return true;
	}

	@Override
	public ItemStack quickMoveStack(Player player, int slotIndex) {
		Slot slot = this.slots.get(slotIndex);
		if (slot == null || !slot.hasItem()) {
			return ItemStack.EMPTY;
		}
		ItemStack stack = slot.getItem();
		ItemStack original = stack.copy();

		if (slotIndex == RESEARCH_SLOT) {
			if (!this.moveItemStackTo(stack, INV_SLOT_START, HOTBAR_SLOT_END, true)) {
				return ItemStack.EMPTY;
			}
		} else if (!this.moveItemStackTo(stack, RESEARCH_SLOT, RESEARCH_SLOT + 1, false)) {
			if (slotIndex < INV_SLOT_END) {
				if (!this.moveItemStackTo(stack, INV_SLOT_END, HOTBAR_SLOT_END, false)) {
					return ItemStack.EMPTY;
				}
			} else if (!this.moveItemStackTo(stack, INV_SLOT_START, INV_SLOT_END, false)) {
				return ItemStack.EMPTY;
			}
		}

		if (stack.isEmpty()) {
			slot.setByPlayer(ItemStack.EMPTY);
		} else {
			slot.setChanged();
		}
		if (stack.getCount() == original.getCount()) {
			return ItemStack.EMPTY;
		}
		slot.onTake(player, stack);
		return original;
	}

	/** Nothing is kept in the menu: closing it gives the item back. */
	@Override
	public void removed(Player player) {
		super.removed(player);
		this.clearContainer(player, this.input);
	}
}