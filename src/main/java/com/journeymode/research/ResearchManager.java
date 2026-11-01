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

	/** Gives the player a researched item. Anything not fully researched is ignored. */
	private static void giveItem(ServerPlayer player, RequestItemPayload request) {
		Item item = BuiltInRegistries.ITEM.getOptional(request.item()).orElse(null);
		if (item == null || item == Items.AIR) {
			return;
		}
		ResearchData data = ResearchData.get(player.level().getServer());
		if (data.progress(player.getUUID(), request.item()) < ResearchCosts.required(item)) {
			return;
		}
		ItemStack stack = new ItemStack(item);
		int maxCount = stack.getMaxStackSize();
		switch (request.mode()) {
			case RequestItemPayload.STACK_TO_INVENTORY -> {
				stack.setCount(maxCount);
				player.getInventory().add(stack);
			}
			case RequestItemPayload.STACK_TO_CURSOR, RequestItemPayload.ONE_TO_CURSOR -> {
				int wanted = request.mode() == RequestItemPayload.ONE_TO_CURSOR ? 1 : maxCount;
				ItemStack carried = player.containerMenu.getCarried();
				if (carried.isEmpty()) {
					stack.setCount(wanted);
					player.containerMenu.setCarried(stack);
				} else if (ItemStack.isSameItemSameComponents(carried, stack)) {
					carried.setCount(Math.min(maxCount, carried.getCount() + wanted));
				}
			}
			default -> {
				return;
			}
		}
		player.containerMenu.broadcastChanges();
	}

	private static void sync(ServerPlayer player) {
		if (ServerPlayNetworking.canSend(player, ResearchSyncPayload.TYPE)) {
			ServerPlayNetworking.send(player, new ResearchSyncPayload(ResearchData.get(player.level().getServer()).all(player.getUUID())));
		}
	}
}