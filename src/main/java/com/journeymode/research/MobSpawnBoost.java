package com.journeymode.research;

import java.util.List;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;

/**
 * Extra hostile mob spawns around players who have raised their Mob Spawns setting. Around each
 * such player, the chunk they stand in and the 8 around it get extra tries at the game's own
 * monster spawning (so light, terrain, biome and the 24 block distance from players all still apply).
 * <p>
 * To keep the game fast, the number of hostile mobs in that area is capped at {@value #CAP_PER_STEP}
 * for every step of the setting (so 60 at 10x), the extra spawning runs only every few ticks, and it
 * pauses completely while the server is running slowly.
 */
public final class MobSpawnBoost {
	/** Hostile mobs allowed in the 3 x 3 chunk area for each step of the setting. */
	private static final int CAP_PER_STEP = 6;
	private static final int INTERVAL_TICKS = 4;
	/** Extra spawning stops while the average tick takes longer than this (the game's limit is 50 ms). */
	private static final long SLOW_TICK_NANOS = 40_000_000L;

	private MobSpawnBoost() {
	}

	public static void initialize() {
		ServerTickEvents.END_SERVER_TICK.register(MobSpawnBoost::tick);
	}

	private static void tick(MinecraftServer server) {
		if (server.getTickCount() % INTERVAL_TICKS != 0 || server.getAverageTickTimeNanos() > SLOW_TICK_NANOS
				|| !JourneySettings.get(server).enabled()) {
			return;
		}
		MobSpawnData data = MobSpawnData.get(server);
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			int multiplier = data.get(player.getUUID());
			if (multiplier > MobSpawnData.NORMAL && !player.isSpectator() && player.level() instanceof ServerLevel level
					&& level.isSpawningMonsters()) {
				boost(level, player.chunkPosition(), multiplier);
			}
		}
	}

	/**
	 * Whether natural hostile spawning is switched off at this position: it is inside the 3 x 3 chunk
	 * area around a player whose setting is 0. Called by {@code NaturalSpawnerMixin} for every spawn attempt.
	 */
	public static boolean spawnsStopped(ServerLevel level, BlockPos pos) {
		MinecraftServer server = level.getServer();
		int chunkX = pos.getX() >> 4;
		int chunkZ = pos.getZ() >> 4;
		for (ServerPlayer player : level.players()) {
			ChunkPos at = player.chunkPosition();
			if (Math.abs(at.x() - chunkX) <= 1 && Math.abs(at.z() - chunkZ) <= 1
					&& MobSpawnData.get(server).get(player.getUUID()) == MobSpawnData.MIN && JourneySettings.get(server).enabled()) {
				return true;
			}
		}
		return false;
	}

	private static void boost(ServerLevel level, ChunkPos center, int multiplier) {
		AABB area = new AABB(center.getMinBlockX() - 16, level.getMinY(), center.getMinBlockZ() - 16,
				center.getMaxBlockX() + 17, level.getMaxY(), center.getMaxBlockZ() + 17);
		List<Entity> hostile = level.getEntities((Entity) null, area, entity -> entity.getType().getCategory() == MobCategory.MONSTER);
		int[] total = {hostile.size()};
		int cap = CAP_PER_STEP * multiplier;
		for (int attempt = 0; attempt < multiplier && total[0] < cap; attempt++) {
			int x = center.x() + level.getRandom().nextInt(3) - 1;
			int z = center.z() + level.getRandom().nextInt(3) - 1;
			LevelChunk chunk = level.getChunkSource().getChunkNow(x, z);
			if (chunk != null) {
				NaturalSpawner.spawnCategoryForChunk(MobCategory.MONSTER, level, chunk, (type, pos, c) -> true, (mob, c) -> total[0]++);
			}
		}
	}
}
