package com.journeymode.research;

import com.journeymode.network.RequestItemPayload;
import com.journeymode.network.ResearchCarriedPayload;
import com.journeymode.network.ResearchSyncPayload;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
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
		PayloadTypeRegistry.clientboundPlay().register(ResearchSyncPayload.TYPE, ResearchSyncPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(ResearchCarriedPayload.TYPE, (payload, context) -> researchCarried(context.player()));
		ServerPlayNetworking.registerGlobalReceiver(RequestItemPayload.TYPE, (payload, context) -> giveItem(context.player(), payload));
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sync(handler.getPlayer()));
	}

	/** Uses up as much of the player's cursor stack as the item still needs. */
	private static void researchCarried(ServerPlayer player) {
		ItemStack carried = player.containerMenu.getCarried();
		if (carried.isEmpty()) {
			return;
		}
		Identifier id = BuiltInRegistries.ITEM.getKey(carried.getItem());
		int needed = ResearchCosts.required(carried.getItem());
		ResearchData data = ResearchData.get(player.level().getServer());
		int used = Math.min(needed - data.progress(player.getUUID(), id), carried.getCount());
		if (used <= 0) {
			return;
		}
		data.add(player.getUUID(), id, used, needed);
		carried.shrink(used);
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