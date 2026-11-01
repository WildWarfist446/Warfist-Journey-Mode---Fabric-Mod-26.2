package com.journeymode.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

/** The sound when an item is researched: a TNT explosion, then a note block ding. */
public final class ResearchSounds {
	/** Ticks between the explosion and the ding (20 ticks is one second). */
	private static final int DING_DELAY_TICKS = 8;
	private static final float EXPLOSION_VOLUME = 0.6F;
	private static final float EXPLOSION_PITCH = 1.0F;
	private static final float DING_VOLUME = 1.0F;
	private static final float DING_PITCH = 1.0F;

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

	/** Plays the explosion now and queues the ding. */
	public static void playResearched() {
		Minecraft.getInstance().getSoundManager()
				.play(SimpleSoundInstance.forUI(SoundEvents.GENERIC_EXPLODE.value(), EXPLOSION_PITCH, EXPLOSION_VOLUME));
		dingCountdown = DING_DELAY_TICKS;
	}

	/** Forgets a ding still waiting, when leaving a world. */
	public static void reset() {
		dingCountdown = -1;
	}
}
