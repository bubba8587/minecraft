// dte:C6,B17,B18
package io.github.bubba8587.bubbas_totems_artifacts.growth;

import io.github.bubba8587.bubbas_totems_artifacts.BubbasTotemsArtifacts;
import io.github.bubba8587.bubbas_totems_artifacts.totems.TotemAreas;
import io.github.bubba8587.bubbas_totems_artifacts.totems.TotemsTuning;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.AABB;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

/**
 * The Totem of Growth (working name; specs/totems-artifacts.md): crops, saplings and other growing plants (block tag
 * {@link #BOOSTED}, datapack-editable) get extra random ticks, vanilla's own growth step (B17), so everything about
 * growing (light, farmland, space for a tree) stays vanilla's.
 *
 * <p>Extra ticks are a multiple of vanilla's rate, which follows the random_tick_speed rule: with extra 2 every block in
 * range gets two more random ticks for each one vanilla gives it, so it grows about three times as fast. The sampling
 * is vanilla's: random positions per chunk section, skipping sections with nothing that ticks
 * ({@code LevelChunkSection.isRandomlyTicking}) and chunks vanilla isn't ticking, so the work is a few hundred block
 * lookups a tick for a held totem and bounded by {@link TotemsTuning#GROWTH_MAX_SAMPLES_PER_TICK} per area.
 *
 * <p>Held (either hand, players and mobs alike, B18): a box {@link TotemsTuning#GROWTH_RADIUS} blocks each way, extra
 * {@link TotemsTuning#GROWTH_HELD_EXTRA}. In a beacon: the beacon's area ({@link TotemAreas.Kind#GROWTH}), extra
 * {@link TotemsTuning#GROWTH_BEACON_EXTRA_PER_LAYER} per pyramid layer, every tick while the beacon keeps it alive.
 * A plant that grows from an extra tick puffs a couple of bone-meal sparkles.
 */
public final class Growth {
	/** What the Totem of Growth speeds up. */
	public static final TagKey<Block> BOOSTED = TagKey.create(Registries.BLOCK, BubbasTotemsArtifacts.id("growth_totem_boosts"));

	private Growth() {
	}

	public static void initialize() {
		ServerTickEvents.END_LEVEL_TICK.register(level -> {
			for (TotemAreas.Area area : TotemAreas.all(level, TotemAreas.Kind.GROWTH)) {
				boost(level, area.box(), area.strength());
				if ((level.getGameTime() + area.box().hashCode()) % TotemsTuning.PLANT_CHECK_TICKS == 0) {
					plant(level, area.box());
				}
			}
		});
	}

	/** Whether Growth plants this dropped stack. */
	public static boolean plantable(net.minecraft.world.item.ItemStack stack) {
		return stack.getItem() instanceof net.minecraft.world.item.BlockItem blockItem && blockItem.getBlock().defaultBlockState().is(BOOSTED);
	}

	/** Growth planting: maybe plant one dropped seed in box; returns whether it did. */
	public static boolean plant(ServerLevel level, AABB box) {
		RandomSource random = level.getRandom();
		if (random.nextFloat() >= TotemsTuning.PLANT_CHANCE) {
			return false;
		}
		java.util.List<net.minecraft.world.entity.item.ItemEntity> seeds = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, box,
				item -> item.isAlive() && plantable(item.getItem()));
		if (seeds.isEmpty()) {
			return false;
		}
		net.minecraft.world.entity.item.ItemEntity seed = seeds.get(random.nextInt(seeds.size()));
		BlockState plant = ((net.minecraft.world.item.BlockItem) seed.getItem().getItem()).getBlock().defaultBlockState();
		BlockPos from = seed.blockPosition();
		int reach = TotemsTuning.PLANT_REACH;
		for (BlockPos pos : BlockPos.randomBetweenClosed(random, 32, from.getX() - reach, from.getY() - 3, from.getZ() - reach,
				from.getX() + reach, from.getY() + 1, from.getZ() + reach)) {
			if (level.getBlockState(pos).isAir() && !isPlant(level.getBlockState(pos.below())) && plant.canSurvive(level, pos) && hasRoom(level, pos, plant)) {
				level.setBlock(pos, plant, Block.UPDATE_ALL);
				seed.getItem().shrink(1);
				if (seed.getItem().isEmpty()) {
					seed.discard();
				}
				level.playSound(null, pos, plant.getSoundType().getPlaceSound(), net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
				level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, 6, 0.3, 0.2, 0.3, 0.0);
				return true;
			}
		}
		return false;
	}

	/** Room to grow: open above, and no wall, trunk or (except for crops) other plant beside it. */
	public static boolean hasRoom(ServerLevel level, BlockPos pos, BlockState plant) {
		if (!level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()) {
			return false;
		}
		boolean crop = plant.is(net.minecraft.tags.BlockTags.CROPS);
		for (net.minecraft.core.Direction side : net.minecraft.core.Direction.Plane.HORIZONTAL) {
			BlockPos n = pos.relative(side);
			BlockState there = level.getBlockState(n);
			if (there.is(net.minecraft.tags.BlockTags.LOGS) || there.isFaceSturdy(level, n, side.getOpposite())
					|| !crop && isPlant(there)) {
				return false;
			}
		}
		return true;
	}

	/** Whether a block is a plant (nothing is planted on top of one). */
	public static boolean isPlant(BlockState state) {
		return state.is(BOOSTED) || state.is(net.minecraft.tags.BlockTags.FLOWERS)
				|| state.getBlock() instanceof net.minecraft.world.level.block.VegetationBlock;
	}

	/** The held totem's box around {@code centre}. */
	public static AABB heldBox(BlockPos centre) {
		int r = TotemsTuning.GROWTH_RADIUS;
		return new AABB(centre.getX() - r, centre.getY() - r, centre.getZ() - r, centre.getX() + r + 1, centre.getY() + r + 1, centre.getZ() + r + 1);
	}

	/**
	 * One tick of extra random ticks in {@code box} (whole blocks inside it), {@code extra} times vanilla's rate.
	 * Returns how many plants changed state (grew).
	 */
	public static int boost(ServerLevel level, AABB box, int extra) {
		int speed = level.getGameRules().get(GameRules.RANDOM_TICK_SPEED);
		if (speed <= 0 || extra <= 0) {
			return 0;
		}
		double perBlock = extra * (double) speed / 4096.0;
		int minX = Mth.floor(box.minX);
		int minY = Math.max(level.getMinY(), Mth.floor(box.minY));
		int minZ = Mth.floor(box.minZ);
		int maxX = Mth.ceil(box.maxX) - 1;
		int maxY = Math.min(level.getMaxY(), Mth.ceil(box.maxY) - 1);
		int maxZ = Mth.ceil(box.maxZ) - 1;
		if (maxY < minY) {
			return 0;
		}
		RandomSource random = level.getRandom();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		int samples = 0;
		int grown = 0;
		for (int cx = SectionPos.blockToSectionCoord(minX); cx <= SectionPos.blockToSectionCoord(maxX); cx++) {
			for (int cz = SectionPos.blockToSectionCoord(minZ); cz <= SectionPos.blockToSectionCoord(maxZ); cz++) {
				if (!level.shouldTickBlocksAt(ChunkPos.pack(cx, cz))) {
					continue;
				}
				LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
				if (chunk == null) {
					continue;
				}
				int x0 = Math.max(minX, SectionPos.sectionToBlockCoord(cx));
				int x1 = Math.min(maxX, SectionPos.sectionToBlockCoord(cx) + 15);
				int z0 = Math.max(minZ, SectionPos.sectionToBlockCoord(cz));
				int z1 = Math.min(maxZ, SectionPos.sectionToBlockCoord(cz) + 15);
				for (int sy = SectionPos.blockToSectionCoord(minY); sy <= SectionPos.blockToSectionCoord(maxY); sy++) {
					LevelChunkSection section = chunk.getSection(chunk.getSectionIndexFromSectionY(sy));
					if (!section.isRandomlyTicking()) {
						continue;
					}
					int y0 = Math.max(minY, SectionPos.sectionToBlockCoord(sy));
					int y1 = Math.min(maxY, SectionPos.sectionToBlockCoord(sy) + 15);
					double want = (x1 - x0 + 1) * (y1 - y0 + 1) * (z1 - z0 + 1) * perBlock;
					int n = (int) want;
					if (random.nextDouble() < want - n) {
						n++;
					}
					for (int i = 0; i < n; i++) {
						if (samples++ >= TotemsTuning.GROWTH_MAX_SAMPLES_PER_TICK) {
							return grown;
						}
						int x = x0 + random.nextInt(x1 - x0 + 1);
						int y = y0 + random.nextInt(y1 - y0 + 1);
						int z = z0 + random.nextInt(z1 - z0 + 1);
						BlockState state = section.getBlockState(x & 15, y & 15, z & 15);
						if (state.isRandomlyTicking() && state.is(BOOSTED)) {
							pos.set(x, y, z);
							state.randomTick(level, pos, random);
							if (level.getBlockState(pos) != state) {
								grown++;
								level.sendParticles(ParticleTypes.HAPPY_VILLAGER, x + 0.5, y + 0.6, z + 0.5, 2, 0.25, 0.25, 0.25, 0.0);
							}
						}
					}
				}
			}
		}
		return grown;
	}
}
