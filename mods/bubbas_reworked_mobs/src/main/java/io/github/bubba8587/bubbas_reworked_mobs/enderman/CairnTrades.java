// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.enderman;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mojang.serialization.Codec;
import io.github.bubba8587.bubbas_reworked_mobs.BubbasReworkedMobs;
import io.github.bubba8587.bubbas_reworked_mobs.display.HandPlacing;
import io.github.bubba8587.bubbas_reworked_mobs.display.LootDisplays;
import io.github.bubba8587.bubbas_reworked_mobs.world.NaturalSpawns;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jspecify.annotations.Nullable;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

/** Trading at cairns: an item left on a cairn's trade pad is taken by its endermen and answered from a loot table. */
public final class CairnTrades {
	/** Per site origin: the game time the answer comes back, or ANSWERED while the answer lies on the pad. */
	public static final AttachmentType<Map<Long, Long>> TRADES = AttachmentRegistry.createPersistent(BubbasReworkedMobs.id("cairn_trades"),
			Codec.unboundedMap(Codec.STRING.xmap(Long::parseLong, String::valueOf), Codec.LONG));

	/** Per site origin: the game time the staring enderman takes the item. */
	public static final AttachmentType<Map<Long, Long>> STARES = AttachmentRegistry.createPersistent(BubbasReworkedMobs.id("cairn_trade_stares"),
			Codec.unboundedMap(Codec.STRING.xmap(Long::parseLong, String::valueOf), Codec.LONG));

	/** Entity tag of the enderman staring at an offered item. */
	public static final String TAKER_TAG = BubbasReworkedMobs.MOD_ID + ".trade_taker";

	public static final ResourceKey<LootTable> TABLE =ResourceKey.create(Registries.LOOT_TABLE, BubbasReworkedMobs.id("gameplay/enderman_trade"));
	private static final long ANSWERED = -1L;

	private CairnTrades() {
	}

	static void tick(ServerLevel level) {
		if (level.dimension() != Level.OVERWORLD || level.getServer() instanceof GameTestServer
				|| (level.getGameTime() % EndermanTuning.TRADE_CHECK_TICKS) != 0) {
			return;
		}
		for (CairnSite site : CairnSites.nearPlayers(level, EndermanTuning.SITE_ACTIVE_RANGE, false)) {
			step(level, site);
		}
	}

	/** One check of a site's trade pad; returns what happened (tests). */
	public static Step step(ServerLevel level, CairnSite site) {
		BlockPos spot = site.tradeSpot();
		if (spot == null || !level.isLoaded(spot) || EndermanWards.isWarded(level, site.origin())) {
			return Step.NONE;
		}
		long key = site.origin().asLong();
		Map<Long, Long> trades = new HashMap<>(level.getAttachedOrElse(TRADES, Map.of()));
		Map<Long, Long> stares = new HashMap<>(level.getAttachedOrElse(STARES, Map.of()));
		Long state = trades.get(key);
		Long stare = stares.get(key);
		boolean item = LootDisplays.hasItemAt(level, spot);
		if (state == null) {
			if (stare != null) {
				if (item && level.getGameTime() < stare) {
					return Step.STARING;
				}
				stares.remove(key);
				level.setAttached(STARES, Map.copyOf(stares));
				sendTakersAway(level, spot);
				if (!item) {
					return Step.NONE;
				}
				take(level, key, spot, trades);
				return Step.TAKEN;
			}
			if (!item || playerNear(level, spot)) {
				return Step.NONE;
			}
			if (comeToStare(level, site, spot)) {
				stares.put(key, level.getGameTime() + EndermanTuning.TRADE_STARE_MIN_TICKS
						+ level.getRandom().nextInt(EndermanTuning.TRADE_STARE_SPREAD_TICKS + 1));
				level.setAttached(STARES, Map.copyOf(stares));
				return Step.STARING;
			}
			take(level, key, spot, trades);
			return Step.TAKEN;
		}
		if (state == ANSWERED) {
			if (!item) {
				trades.remove(key);
				level.setAttached(TRADES, Map.copyOf(trades));
			}
			return Step.NONE;
		}
		if (level.getGameTime() < state || item || !level.getBlockState(spot).isAir()) {
			return Step.NONE;
		}
		answer(level, site, spot);
		trades.put(key, ANSWERED);
		level.setAttached(TRADES, Map.copyOf(trades));
		return Step.ANSWERED;
	}

	private static void take(ServerLevel level, long key, BlockPos spot, Map<Long, Long> trades) {
		LootDisplays.takeForMob(level, spot);
		vanishEffects(level, spot);
		trades.put(key, level.getGameTime() + EndermanTuning.TRADE_RETURN_MIN_TICKS
				+ level.getRandom().nextInt(EndermanTuning.TRADE_RETURN_SPREAD_TICKS + 1));
		level.setAttached(TRADES, Map.copyOf(trades));
	}

	/** An enderman appears beside the pad and stands staring at the item; false when there's no room for one. */
	private static boolean comeToStare(ServerLevel level, CairnSite site, BlockPos spot) {
		Direction out = Direction.getApproximateNearest(spot.getX() - site.origin().getX(), 0, spot.getZ() - site.origin().getZ());
		BlockPos stand = EndermanSpawning.spawnCell(level, spot.getX() + out.getStepX() * 2, spot.getY(), spot.getZ() + out.getStepZ() * 2);
		if (stand == null) {
			return false;
		}
		Enderman taker = NaturalSpawns.create(level, EntityTypes.ENDERMAN, stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, out.getOpposite().toYRot());
		if (taker == null) {
			return false;
		}
		((EndermanAccess) taker).brm$markCairnSpawn();
		((EndermanAccess) taker).brm$setHome(site.origin());
		taker.addTag(TAKER_TAG);
		taker.setNoAi(true);
		lookAt(taker, spot);
		level.addFreshEntity(taker);
		level.sendParticles(ParticleTypes.PORTAL, taker.getX(), taker.getY() + 1.5, taker.getZ(), 40, 0.4, 1.0, 0.4, 0.3);
		level.playSound(null, taker.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0F, 1.0F);
		return true;
	}

	/** Body and head turned to the item on the pad, eyes down on it. */
	private static void lookAt(Enderman taker, BlockPos spot) {
		taker.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, new net.minecraft.world.phys.Vec3(spot.getX() + 0.5, spot.getY() + 0.1, spot.getZ() + 0.5));
		taker.setYBodyRot(taker.getYRot());
		taker.setYHeadRot(taker.getYRot());
	}

	/** The endermen staring at a pad teleport away. */
	private static void sendTakersAway(ServerLevel level, BlockPos spot) {
		for (Enderman taker : level.getEntitiesOfClass(Enderman.class, new net.minecraft.world.phys.AABB(spot).inflate(4.0),
				e -> e.entityTags().contains(TAKER_TAG))) {
			level.sendParticles(ParticleTypes.PORTAL, taker.getX(), taker.getY() + 1.5, taker.getZ(), 40, 0.4, 1.0, 0.4, 0.3);
			taker.discard();
		}
	}

	/** What a check did. */
	public enum Step {
		NONE, STARING, TAKEN, ANSWERED
	}

	/** For tests: the stare is over now. */
	public static void hurryStare(ServerLevel level, CairnSite site) {
		Map<Long, Long> stares = new HashMap<>(level.getAttachedOrElse(STARES, Map.of()));
		stares.computeIfPresent(site.origin().asLong(), (k, v) -> level.getGameTime());
		level.setAttached(STARES, Map.copyOf(stares));
	}

	/** For tests: the answer is due now. */
	public static void hurry(ServerLevel level, CairnSite site) {
		Map<Long, Long> trades = new HashMap<>(level.getAttachedOrElse(TRADES, Map.of()));
		trades.computeIfPresent(site.origin().asLong(), (k, v) -> v == ANSWERED ? v : level.getGameTime());
		level.setAttached(TRADES, Map.copyOf(trades));
	}

	private static boolean playerNear(ServerLevel level, BlockPos spot) {
		return level.getNearestPlayer(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, EndermanTuning.TRADE_QUIET_RANGE,
				EntitySelector.NO_SPECTATORS) != null;
	}

	private static void vanishEffects(ServerLevel level, BlockPos spot) {
		level.sendParticles(ParticleTypes.PORTAL, spot.getX() + 0.5, spot.getY() + 0.8, spot.getZ() + 0.5, 40, 0.3, 0.8, 0.3, 0.3);
		level.playSound(null, spot, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0F, 1.0F);
	}

	/** An enderman comes back with the answer and lays it on the pad. */
	private static void answer(ServerLevel level, CairnSite site, BlockPos spot) {
		Direction out = Direction.getApproximateNearest(spot.getX() - site.origin().getX(), 0, spot.getZ() - site.origin().getZ());
		BlockPos stand = EndermanSpawning.spawnCell(level, spot.getX() + out.getStepX() * 2, spot.getY(), spot.getZ() + out.getStepZ() * 2);
		Enderman courier = stand == null ? null
				: NaturalSpawns.create(level, EntityTypes.ENDERMAN, stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, out.getOpposite().toYRot());
		if (courier == null) {
			HandPlacing.lay(level, null, spot, roll(level, EntityTypes.ENDERMAN.create(level, net.minecraft.world.entity.EntitySpawnReason.LOAD)));
			vanishEffects(level, spot);
			return;
		}
		((EndermanAccess) courier).brm$markCairnSpawn();
		((EndermanAccess) courier).brm$setHome(site.origin());
		courier.setItemSlot(EquipmentSlot.MAINHAND, roll(level, courier));
		level.addFreshEntity(courier);
		level.sendParticles(ParticleTypes.PORTAL, courier.getX(), courier.getY() + 1.5, courier.getZ(), 40, 0.4, 1.0, 0.4, 0.3);
		level.playSound(null, courier.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0F, 1.0F);
		HandPlacing.lay(level, courier, spot, courier.getMainHandItem());
	}

	/** One answer from the trade table (always one item; an empty roll gives a poppy). */
	public static ItemStack roll(ServerLevel level, @Nullable Enderman by) {
		LootTable table = level.getServer().reloadableRegistries().getLootTable(TABLE);
		if (by != null) {
			LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.THIS_ENTITY, by).create(LootContextParamSets.PIGLIN_BARTER);
			List<ItemStack> items = table.getRandomItems(params);
			if (!items.isEmpty() && !items.getFirst().isEmpty()) {
				return items.getFirst();
			}
		}
		return new ItemStack(net.minecraft.world.item.Items.POPPY);
	}
}
