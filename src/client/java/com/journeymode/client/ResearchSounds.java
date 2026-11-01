package com.journeymode.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

/**
 * The sounds for research: a TNT explosion then a note block ding when an item is fully researched,
 * and the item pickup pop when items are added to research that doesn't finish it yet.
 */
public final class ResearchSounds {
	/** Ticks between the explosion and the ding (20 ticks is one second). */
	private static final int DING_DELAY_TICKS = 8;
	private static final float EXPLOSION_VOLUME = 0.6F;
	private static final float EXPLOSION_PITCH = 1.0F;
	private static final float DING_VOLUME = 1.0F;
	private static final float DING_PITCH = 1.0F;
	private static final float POP_VOLUME = 0.3F;
	/** The pickup sound's usual pitch is a little over 2, varied a bit each time. */
	private static final float POP_PITCH = 2.0F;

	/** Ticks left until the ding, or -1 when none is waiting. */
	private static int dingCountdown = -1;

	private ResearchSounds() {
	}

	public static void initialize() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (dingCountdown >= 0 && --dingCountdown < 0) {
				client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BELL.value(), DING_PITCH, DING_VOLUME));
			}
		});
	}

	/** An item was fully researched: plays the explosion now and queues the ding. */
	public static void playCompleted() {
		Minecraft.getInstance().getSoundManager()
				.play(SimpleSoundInstance.forUI(SoundEvents.GENERIC_EXPLODE.value(), EXPLOSION_PITCH, EXPLOSION_VOLUME));
		dingCountdown = DING_DELAY_TICKS;
	}

	/** Items were added to an item's research, but it isn't finished: the item pickup pop. */
	public static void playProgress() {
		float pitch = POP_PITCH + (float) (Math.random() - Math.random()) * 0.4F;
		Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.ITEM_PICKUP, pitch, POP_VOLUME));
	}

	/** Forgets a ding still waiting, when leaving a world. */
	public static void reset() {
		dingCountdown = -1;
	}
}
