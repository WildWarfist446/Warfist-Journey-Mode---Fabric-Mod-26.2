package com.journeymode.research;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import java.util.Properties;

import com.journeymode.JourneyMode;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Whether Journey Mode is on for this world, saved with the world. It is decided once, when the
 * world is first loaded with the mod, from the toggle on the world creation screen, and never
 * changes after that.
 */
public final class JourneySettings extends SavedData {
	private static final Codec<JourneySettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.BOOL.optionalFieldOf("enabled").forGetter(settings -> Optional.ofNullable(settings.enabled))
	).apply(instance, enabled -> new JourneySettings(enabled.orElse(null))));

	public static final SavedDataType<JourneySettings> TYPE = new SavedDataType<>(
			JourneyMode.id("settings"), JourneySettings::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

	/** The server.properties key a dedicated server's owner sets before the world is first created. */
	private static final String PROPERTY = "journey-mode";

	/** Null until the world has been decided one way or the other. */
	private Boolean enabled;

	public JourneySettings() {
	}

	private JourneySettings(Boolean enabled) {
		this.enabled = enabled;
	}

	public static JourneySettings get(MinecraftServer server) {
		return server.getDataStorage().computeIfAbsent(TYPE);
	}

	public boolean enabled() {
		return Boolean.TRUE.equals(this.enabled);
	}

	/**
	 * Called when a server starts. A world that has never been decided is decided now, once:
	 * <ul>
	 *   <li>on a dedicated server, by the {@code journey-mode} line in server.properties;</li>
	 *   <li>otherwise, on only if the world is brand new (no game time has passed) and the player
	 *   switched it on while creating it. Worlds that already existed without the mod stay off.</li>
	 * </ul>
	 */
	public static void decide(MinecraftServer server) {
		JourneySettings settings = get(server);
		if (settings.enabled == null) {
			boolean brandNew = server.overworld().getGameTime() == 0;
			settings.enabled = server.isDedicatedServer() ? readServerProperty() : brandNew && JourneyMode.pendingNewWorld;
			settings.setDirty();
			JourneyMode.LOGGER.info("Journey Mode is {} for this world", settings.enabled ? "on" : "off");
		}
		JourneyMode.pendingNewWorld = false;
	}

	/**
	 * Reads {@code journey-mode} from server.properties (false if it is missing, in which case the
	 * line is added so it can be found and edited). Only consulted for a world that has not been decided yet.
	 */
	private static boolean readServerProperty() {
		Path file = FabricLoader.getInstance().getGameDir().resolve("server.properties");
		try {
			Properties properties = new Properties();
			try (Reader reader = Files.newBufferedReader(file)) {
				properties.load(reader);
			}
			String value = properties.getProperty(PROPERTY);
			if (value == null) {
				Files.writeString(file, System.lineSeparator() + PROPERTY + "=false" + System.lineSeparator(), StandardOpenOption.APPEND);
				return false;
			}
			return Boolean.parseBoolean(value.strip());
		} catch (IOException | RuntimeException e) {
			JourneyMode.LOGGER.warn("Could not read {} from {}, leaving Journey Mode off", PROPERTY, file, e);
			return false;
		}
	}
}
