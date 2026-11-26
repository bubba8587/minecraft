// dte:C8
package io.github.bubba8587.bubbas_world_tweaks.parasitic;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.bubba8587.bubbas_world_tweaks.BubbasWorldTweaks;
import io.github.bubba8587.bubbas_world_tweaks.WorldTweaksTuning;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.LevelBasedValue;

/** Parasitic (specs/world-tweaks.md "Parasitic"): an enchantment that now and then repairs its item with its wearer's health. */
public final class Parasitic {
	/** The enchantment effect component: the most health a bite takes and the most durability it gives back, per level. */
	public record Effect(LevelBasedValue maxHealth, LevelBasedValue maxRepair) {
		public static final Codec<Effect> CODEC = RecordCodecBuilder.create(i -> i.group(
				LevelBasedValue.CODEC.fieldOf("max_health").forGetter(Effect::maxHealth),
				LevelBasedValue.CODEC.fieldOf("max_repair").forGetter(Effect::maxRepair)).apply(i, Effect::new));
	}

	public static final DataComponentType<Effect> EFFECT = Registry.register(BuiltInRegistries.ENCHANTMENT_EFFECT_COMPONENT_TYPE,
			BubbasWorldTweaks.id("parasitic"), DataComponentType.<Effect>builder().persistent(Effect.CODEC).build());
	public static final ResourceKey<DamageType> DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, BubbasWorldTweaks.id("parasitic"));

	/** Game time each player's next bite may come. */
	private static final Map<ServerPlayer, Long> NEXT_BITE = new WeakHashMap<>();

	private Parasitic() {
	}

	public static void initialize() {
		ServerTickEvents.END_LEVEL_TICK.register(level -> {
			if (level.getGameTime() % 20 != 0) {
				return;
			}
			for (ServerPlayer player : level.players()) {
				tick(level, player);
			}
		});
	}

	/** Once a second per player: off cooldown, a chance to bite. */
	static void tick(ServerLevel level, ServerPlayer player) {
		if (!player.isAlive() || player.isSpectator() || player.isCreative()) {
			return;
		}
		long now = level.getGameTime();
		RandomSource random = player.getRandom();
		Long next = NEXT_BITE.get(player);
		if (next == null) {
			NEXT_BITE.put(player, now + cooldown(random));
			return;
		}
		if (now < next || random.nextFloat() >= WorldTweaksTuning.PARASITIC_BITE_CHANCE) {
			return;
		}
		if (bite(level, player, random)) {
			NEXT_BITE.put(player, now + cooldown(random));
		}
	}

	private static int cooldown(RandomSource random) {
		return WorldTweaksTuning.PARASITIC_COOLDOWN_TICKS + random.nextInt(WorldTweaksTuning.PARASITIC_COOLDOWN_SPREAD + 1);
	}

	/** One bite: a random damaged Parasitic item the player has equipped takes health and mends by it. False if none. */
	public static boolean bite(ServerLevel level, ServerPlayer player, RandomSource random) {
		record Host(ItemStack stack, Effect effect, int enchantLevel) {
		}
		List<Host> hosts = new ArrayList<>();
		EnchantmentHelper.runIterationOnEquipment(player, (enchantment, enchantLevel, item) -> {
			Effect effect = enchantment.value().effects().get(EFFECT);
			if (effect != null && item.itemStack().isDamaged()) {
				hosts.add(new Host(item.itemStack(), effect, enchantLevel));
			}
		});
		if (hosts.isEmpty()) {
			return false;
		}
		Host host = hosts.get(random.nextInt(hosts.size()));
		float share = WorldTweaksTuning.PARASITIC_MIN_SHARE + random.nextFloat() * (1.0F - WorldTweaksTuning.PARASITIC_MIN_SHARE);
		float health = share * host.effect().maxHealth().calculate(host.enchantLevel());
		float repair = share * host.effect().maxRepair().calculate(host.enchantLevel());
		int damaged = host.stack().getDamageValue();
		if (repair > damaged) {
			// nearly mended: take only the health the repair needs
			health *= damaged / repair;
			repair = damaged;
		}
		if (!player.hurtServer(level, level.damageSources().source(DAMAGE), health)) {
			return false; // no health taken (just hit, invulnerable): no repair, try again next second
		}
		host.stack().setDamageValue(Math.max(0, damaged - Math.round(repair)));
		return true;
	}
}
