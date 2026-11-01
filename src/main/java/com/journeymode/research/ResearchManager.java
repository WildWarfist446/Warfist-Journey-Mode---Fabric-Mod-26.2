package com.journeymode.research;

import com.journeymode.network.CostConfigPayload;
import com.journeymode.network.GodModeSyncPayload;
import com.journeymode.network.GodModeTogglePayload;
import com.journeymode.network.JourneyStatePayload;
import com.journeymode.network.MobSpawnSetPayload;
import com.journeymode.network.RequestItemPayload;
import com.journeymode.network.ResearchCarriedPayload;
import com.journeymode.network.ResearchSlotPayload;
import com.journeymode.network.ResearchSyncPayload;
import com.journeymode.network.TimeSetPayload;
import com.journeymode.network.TrashClickPayload;
import com.journeymode.network.TrashSyncPayload;
import com.journeymode.network.WeatherSetPayload;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.tags.DamageTypeTags;
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
		PayloadTypeRegistry.clientboundPlay().register(JourneyStatePayload.TYPE, JourneyStatePayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(GodModeTogglePayload.TYPE, GodModeTogglePayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(GodModeSyncPayload.TYPE, GodModeSyncPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(GodModeTogglePayload.TYPE, (payload, context) -> toggleGodMode(context.player()));
		// God Mode: damage to a player who has it on is cancelled, except what ignores invulnerability (the void, /kill).
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> !(entity instanceof ServerPlayer player
				&& !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
				&& enabled(player)
				&& GodModeData.get(player.level().getServer()).isOn(player.getUUID())));
		// One payload type in both directions: the player asks for a setting and the server confirms it.
		PayloadTypeRegistry.serverboundPlay().register(MobSpawnSetPayload.TYPE, MobSpawnSetPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(MobSpawnSetPayload.TYPE, MobSpawnSetPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(MobSpawnSetPayload.TYPE, (payload, context) -> setMobSpawns(context.player(), payload));
		MobSpawnBoost.initialize();
		PayloadTypeRegistry.serverboundPlay().register(WeatherSetPayload.TYPE, WeatherSetPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(WeatherSetPayload.TYPE, (payload, context) -> setWeather(context.player(), payload));
		PayloadTypeRegistry.serverboundPlay().register(TimeSetPayload.TYPE, TimeSetPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(TimeSetPayload.TYPE, (payload, context) -> setTime(context.player(), payload));
		ResearchCosts.load();
		ServerLifecycleEvents.SERVER_STARTED.register(JourneySettings::decide);
		ServerPlayNetworking.registerGlobalReceiver(ResearchCarriedPayload.TYPE, (payload, context) -> researchCarried(context.player()));
		ServerPlayNetworking.registerGlobalReceiver(RequestItemPayload.TYPE, (payload, context) -> giveItem(context.player(), payload));
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer player = handler.getPlayer();
			// Journey Mode is chosen per world, when it is created. Where it is off, the client gets only that fact.
			boolean enabled = enabled(player);
			if (ServerPlayNetworking.canSend(player, JourneyStatePayload.TYPE)) {
				ServerPlayNetworking.send(player, new JourneyStatePayload(enabled));
			}
			if (!enabled) {
				return;
			}
			// The costs go first, so the client knows what counts as researched when the progress arrives.
			if (ServerPlayNetworking.canSend(player, CostConfigPayload.TYPE)) {
				ServerPlayNetworking.send(player, new CostConfigPayload(ResearchCosts.rules()));
			}
			sync(player);
			syncTrash(player);
			syncGodMode(player);
			syncMobSpawns(player);
			JourneyAdvancements.onJoin(player);
		});
	}

	/**
	 * Clicking the trash slot with an item on the cursor puts that stack in it, throwing away
	 * whatever was there before. With an empty cursor it takes the trashed stack back out.
	 */
	/** The four times the time slider can set, as the names /time set takes. */
	private static final String[] TIME_NAMES = {"day", "noon", "night", "midnight"};

	/**
	 * Sets the time by running {@code /time set} as the player, so the game's own permission check
	 * applies: only operators can do it, and a client that is not one is simply refused.
	 */
	private static void setTime(ServerPlayer player, TimeSetPayload payload) {
		if (!enabled(player) || payload.stop() < 0 || payload.stop() >= TIME_NAMES.length) {
			return;
		}
		player.level().getServer().getCommands().performPrefixedCommand(player.createCommandSourceStack(), "time set " + TIME_NAMES[payload.stop()]);
		if (player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
			JourneyAdvancements.award(player, "khonshu_control");
		}
	}

	/** The weathers the weather slider can set, as the names /weather takes. */
	private static final String[] WEATHER_NAMES = {"clear", "rain", "thunder"};

	/** Sets the weather by running {@code /weather} as the player, so only operators can (see {@link #setTime}). */
	private static void setWeather(ServerPlayer player, WeatherSetPayload payload) {
		if (!enabled(player) || payload.stop() < 0 || payload.stop() >= WEATHER_NAMES.length) {
			return;
		}
		player.level().getServer().getCommands().performPrefixedCommand(player.createCommandSourceStack(), "weather " + WEATHER_NAMES[payload.stop()]);
		if (player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
			JourneyAdvancements.award(player, "weather_wizard");
		}
	}

	/** Saves the player's Mob Spawns setting (see {@link MobSpawnBoost}) and tells them what was kept. */
	private static void setMobSpawns(ServerPlayer player, MobSpawnSetPayload payload) {
		if (enabled(player)) {
			MobSpawnData.get(player.level().getServer()).set(player.getUUID(), payload.multiplier());
			syncMobSpawns(player);
		}
	}

	private static void syncMobSpawns(ServerPlayer player) {
		if (ServerPlayNetworking.canSend(player, MobSpawnSetPayload.TYPE)) {
			ServerPlayNetworking.send(player, new MobSpawnSetPayload(MobSpawnData.get(player.level().getServer()).get(player.getUUID())));
		}
	}

	private static void toggleGodMode(ServerPlayer player) {
		if (enabled(player)) {
			if (GodModeData.get(player.level().getServer()).toggle(player.getUUID())) {
				JourneyAdvancements.award(player, "fallen_from_glory");
			}
			syncGodMode(player);
		}
	}

	private static void syncGodMode(ServerPlayer player) {
		if (ServerPlayNetworking.canSend(player, GodModeSyncPayload.TYPE)) {
			ServerPlayNetworking.send(player, new GodModeSyncPayload(GodModeData.get(player.level().getServer()).isOn(player.getUUID())));
		}
	}

	private static boolean enabled(ServerPlayer player) {
		return JourneySettings.get(player.level().getServer()).enabled();
	}

	private static void clickTrash(ServerPlayer player) {
		if (!enabled(player)) {
			return;
		}
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
		if (enabled(player)) {
			research(player, player.containerMenu.getCarried());
		}
	}

	/**
	 * Uses up as much of the stack in an inventory slot as the item still needs. Only the main
	 * inventory and hotbar count, so a crafting result can never be turned into free research.
	 */
	private static void researchSlot(ServerPlayer player, ResearchSlotPayload payload) {
		if (!enabled(player) || payload.slot() < 0 || payload.slot() >= player.containerMenu.slots.size()) {
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
		// The stack may be used up below, and an empty stack reports no item, so keep the item and its key.
		Item item = stack.getItem();
		String key = ResearchKeys.of(stack);
		int needed = ResearchCosts.required(item);
		ResearchData data = ResearchData.get(player.level().getServer());
		int used = Math.min(needed - data.progress(player.getUUID(), key), stack.getCount());
		if (used <= 0) {
			return;
		}
		data.add(player.getUUID(), key, used, needed);
		stack.shrink(used);
		player.containerMenu.broadcastChanges();
		sync(player);
		if (data.progress(player.getUUID(), key) >= needed) {
			JourneyAdvancements.onResearched(player, item);
		}
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
		if (!enabled(player)) {
			return;
		}
		// What was clicked is made from its key, so a potion or book comes with what was researched.
		ItemStack clicked = ResearchKeys.stackOf(request.key(), player.level().getServer().registryAccess());
		if (clicked.isEmpty()) {
			return;
		}
		ResearchData data = ResearchData.get(player.level().getServer());
		if (data.progress(player.getUUID(), request.key()) < ResearchCosts.required(clicked.getItem())) {
			return;
		}
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