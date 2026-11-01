package com.journeymode.research;

import com.journeymode.network.CostConfigPayload;
import com.journeymode.network.RequestItemPayload;
import com.journeymode.network.ResearchCarriedPayload;
import com.journeymode.network.ResearchSlotPayload;
import com.journeymode.network.ResearchSyncPayload;
import com.journeymode.network.TrashClickPayload;
import com.journeymode.network.TrashSyncPayload;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** The server side of research: sacrificing the stack on a player's cursor, and keeping their client up to date. */
public final class ResearchManager {
	private ResearchManager() {
	}

	public static void initialize() {
		PayloadTypeRegistry.serverboundPlay().register(ResearchCarriedPayload.TYPE, ResearchCarriedPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(RequestItemPayload.TYPE, RequestItemPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(ResearchSlotPayload.TYPE, ResearchSlotPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(ResearchSlotPayload.TYPE, (payload, context) -> researchSlot(context.player(), payload));
		PayloadTypeRegistry.clientboundPlay().register(ResearchSyncPayload.TYPE, ResearchSyncPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(CostConfigPayload.TYPE, CostConfigPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(TrashClickPayload.TYPE, TrashClickPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(TrashSyncPayload.TYPE, TrashSyncPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(TrashClickPayload.TYPE, (payload, context) -> clickTrash(context.player()));
		ResearchCosts.load();
		ServerPlayNetworking.registerGlobalReceiver(ResearchCarriedPayload.TYPE, (payload, context) -> researchCarried(context.player()));
		ServerPlayNetworking.registerGlobalReceiver(RequestItemPayload.TYPE, (payload, context) -> giveItem(context.player(), payload));
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer player = handler.getPlayer();
			// The costs go first, so the client knows what counts as researched when the progress arrives.
			if (ServerPlayNetworking.canSend(player, CostConfigPayload.TYPE)) {
				ServerPlayNetworking.send(player, new CostConfigPayload(ResearchCosts.rules()));
			}
			sync(player);
			syncTrash(player);
		});
	}

	/**
	 * Clicking the trash slot with an item on the cursor puts that stack in it, throwing away
	 * whatever was there before. With an empty cursor it takes the trashed stack back out.
	 */
	private static void clickTrash(ServerPlayer player) {
		TrashData data = TrashData.get(player.level().getServer());
		ItemStack carried = player.containerMenu.getCarried();
		if (!carried.isEmpty()) {
			data.set(player.getUUID(), carried);
			player.containerMenu.setCarried(ItemStack.EMPTY);
		} else if (!data.get(player.getUUID()).isEmpty()) {
			player.containerMenu.setCarried(data.get(player.getUUID()).copy());
			data.set(player.getUUID(), ItemStack.EMPTY);
		} else {
			return;
		}
		player.containerMenu.broadcastChanges();
		syncTrash(player);
	}

	private static void syncTrash(ServerPlayer player) {
		if (ServerPlayNetworking.canSend(player, TrashSyncPayload.TYPE)) {
			ServerPlayNetworking.send(player, new TrashSyncPayload(TrashData.get(player.level().getServer()).get(player.getUUID()).copy()));
		}
	}

	/** Uses up as much of the player's cursor stack as the item still needs. */
	private static void researchCarried(ServerPlayer player) {
		research(player, player.containerMenu.getCarried());
	}

	/**
	 * Uses up as much of the stack in an inventory slot as the item still needs. Only the main
	 * inventory and hotbar count, so a crafting result can never be turned into free research.
	 */
	private static void researchSlot(ServerPlayer player, ResearchSlotPayload payload) {
		if (payload.slot() < 0 || payload.slot() >= player.containerMenu.slots.size()) {
			return;
		}
		Slot slot = player.containerMenu.getSlot(payload.slot());
		if (slot.container != player.getInventory() || slot.getContainerSlot() >= 36 || !slot.mayPickup(player)) {
			return;
		}
		research(player, slot.getItem());
		slot.setChanged();
	}

	private static void research(ServerPlayer player, ItemStack stack) {
		if (stack.isEmpty()) {
			return;
		}
		Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
		int needed = ResearchCosts.required(stack.getItem());
		ResearchData data = ResearchData.get(player.level().getServer());
		int used = Math.min(needed - data.progress(player.getUUID(), id), stack.getCount());
		if (used <= 0) {
			return;
		}
		data.add(player.getUUID(), id, used, needed);
		stack.shrink(used);
		player.containerMenu.broadcastChanges();
		sync(player);
	}

	/**
	 * Handles a click on a researched item in the item list, the way the creative menu does:
	 * <ul>
	 *   <li>empty cursor: left or right click picks up one, shift-click a full stack, middle-click a full stack;</li>
	 *   <li>the same item on the cursor: left click adds one, shift-click fills the stack, right click removes one.</li>
	 * </ul>
	 * Unlike the creative menu, a click with a different item on the cursor does nothing, so a
	 * real item in the player's hand is never thrown away. Anything not fully researched is ignored.
	 */
	private static void giveItem(ServerPlayer player, RequestItemPayload request) {
		Item item = BuiltInRegistries.ITEM.getOptional(request.item()).orElse(null);
		if (item == null || item == Items.AIR) {
			return;
		}
		ResearchData data = ResearchData.get(player.level().getServer());
		if (data.progress(player.getUUID(), request.item()) < ResearchCosts.required(item)) {
			return;
		}
		ItemStack clicked = new ItemStack(item);
		int max = clicked.getMaxStackSize();
		ItemStack carried = player.containerMenu.getCarried();

		if (carried.isEmpty()) {
			boolean fullStack = request.quick() || request.button() == 2;
			if (request.button() < 0 || request.button() > 2) {
				return;
			}
			player.containerMenu.setCarried(clicked.copyWithCount(fullStack ? max : 1));
		} else if (ItemStack.isSameItemSameComponents(carried, clicked)) {
			switch (request.button()) {
				case 0 -> {
					if (request.quick()) {
						carried.setCount(max);
					} else if (carried.getCount() < max) {
						carried.grow(1);
					}
				}
				case 1 -> carried.shrink(1);
				default -> {
					return;
				}
			}
		} else {
			return;
		}
		player.containerMenu.broadcastChanges();
	}
	private static void sync(ServerPlayer player) {
		if (ServerPlayNetworking.canSend(player, ResearchSyncPayload.TYPE)) {
			ServerPlayNetworking.send(player, new ResearchSyncPayload(ResearchData.get(player.level().getServer()).all(player.getUUID())));
		}
	}
}