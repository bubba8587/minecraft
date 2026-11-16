// dte:C4
package io.github.bubba8587.bubbas_infused_gems.effect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.bubba8587.bubbas_infused_gems.balance.Balance;
import io.github.bubba8587.bubbas_infused_gems.gem.Gem;
import io.github.bubba8587.bubbas_infused_gems.registry.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.LivingEntity;

/**
 * Gems in Bubba's Reworked Beacons (owner, 2026-11-16): a gem in a beacon broadcasts its effects, as worn, to every living
 * thing in the area. This class never touches the beacons API (only the payload does), so it loads without that mod.
 *
 * <p>Strength (agent's choice, the model's {@code BEACON_SHARE}): a gem in a beacon gives everyone in range BEACON_SHARE
 * of what a full netherite set of that gem would (half, so a perfect gem in a full beacon is about a vanilla beacon's
 * level I to II), with its Gem Power capped by the pyramid. Its bonus roll the same; its drawback as one worn drawback.
 * A beacon's gems join the receiver's worn totals and the stronger applies per effect (never added): armor, other
 * beacons and potions alike. Each receipt lasts the beacon's effect time and is renewed by every pulse.
 */
public final class GemBeacons {
	/** One beacon's gem on a receiver, until game time {@code until}. */
	public record Received(BlockPos beacon, Gem gem, int power, long until) {
		public static final StreamCodec<RegistryFriendlyByteBuf, Received> STREAM_CODEC = StreamCodec.composite(
				BlockPos.STREAM_CODEC, Received::beacon, Gem.STREAM_CODEC, Received::gem, ByteBufCodecs.VAR_INT, Received::power,
				ByteBufCodecs.VAR_LONG, Received::until, Received::new);
	}

	private GemBeacons() {
	}

	/** A beacon pulse reached this entity (server): note it and, when anything changed, recompute its worn totals. */
	public static void receive(LivingEntity entity, BlockPos beacon, Gem gem, int power, long until) {
		List<Received> now = entity.getAttached(ModAttachments.BEACON_GEMS);
		List<Received> next = new ArrayList<>();
		boolean changed = true;
		if (now != null) {
			for (Received r : now) {
				if (r.beacon().equals(beacon)) {
					changed = !r.gem().equals(gem) || r.power() != power;
				} else if (r.until() > entity.level().getGameTime()) {
					next.add(r);
				}
			}
		}
		next.add(new Received(beacon.immutable(), gem, power, until));
		entity.setAttached(ModAttachments.BEACON_GEMS, List.copyOf(next));
		if (changed) {
			WornEffects.refresh(entity);
		}
	}

	/** Drops receipts that ran out (server, from the worn tick); recomputes the totals when one did. */
	public static void expire(LivingEntity entity) {
		List<Received> now = entity.getAttached(ModAttachments.BEACON_GEMS);
		if (now == null) {
			return;
		}
		long time = entity.level().getGameTime();
		List<Received> live = now.stream().filter(r -> r.until() > time).toList();
		if (live.size() != now.size()) {
			if (live.isEmpty()) {
				entity.removeAttached(ModAttachments.BEACON_GEMS);
			} else {
				entity.setAttached(ModAttachments.BEACON_GEMS, live);
			}
			WornEffects.refresh(entity);
		}
	}

	/** The live beacon gems on this entity as totals (each effect the strongest beacon's), or NONE. */
	public static GemTotals totals(LivingEntity entity) {
		List<Received> now = entity.getAttached(ModAttachments.BEACON_GEMS);
		if (now == null || now.isEmpty()) {
			return GemTotals.NONE;
		}
		long time = entity.level().getGameTime();
		Map<String, Double> totals = new HashMap<>();
		Map<String, Double> drawbacks = new HashMap<>();
		Map<String, Integer> counts = new HashMap<>();
		Balance b = Balance.get();
		for (Received r : now) {
			if (r.until() <= time) {
				continue;
			}
			Gem g = r.gem();
			// The pyramid caps the Gem Power: the gem's quality as if made at the capped level.
			int power = Math.max(1, Math.min(r.power(), g.infusion()));
			double ratio = b.infusion(power).factor() / b.infusion(g.infusion()).factor();
			if (g.hasMain()) {
				totals.merge(g.key(), share(g.key(), g.main() * ratio), Math::max);
			}
			g.other().ifPresent(o -> totals.merge(o.key(), share(o.key(), o.strength() * ratio), Math::max));
			g.drawback().ifPresent(d -> {
				drawbacks.merge(d.key(), b.drawbackSum(List.of((double) d.strength())), Math::max);
				counts.merge(d.key(), 1, Math::max);
			});
		}
		return totals.isEmpty() && drawbacks.isEmpty() ? GemTotals.NONE : new GemTotals(Map.copyOf(totals), Map.copyOf(counts), Map.copyOf(drawbacks));
	}

	/** BEACON_SHARE of a full netherite set of a gem of quality q: that set's total is the effect's worn cap x q. */
	private static double share(String key, double q) {
		Balance b = Balance.get();
		Balance.EffectData e = b.effect(key);
		double cap = e != null && e.wornCap() > 0.0 ? e.wornCap() : b.cap;
		return b.beaconShare() * cap * q;
	}
}
