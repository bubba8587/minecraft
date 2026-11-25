// dte:C6,C1,B17
package io.github.bubba8587.bubbas_totems_artifacts.keeping;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.jspecify.annotations.Nullable;

/**
 * Reworked Mobs' Soulfire deaths (specs/soulfire.md), by id with no compile dependency: damage in
 * {@code #bubbas_reworked_mobs:bypasses_totems} is not kept by a Totem of Keeping unless the victim has Soulguard (the
 * effect or the enchantment). Without Reworked Mobs the tag is empty and nothing changes.
 */
public final class SoulfireDeaths {
	private static final String MOBS = "bubbas_reworked_mobs";
	public static final TagKey<DamageType> BYPASSES_TOTEMS = TagKey.create(Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath(MOBS, "bypasses_totems"));
	private static final Identifier SOULGUARD = Identifier.fromNamespaceAndPath(MOBS, "soulguard");
	private static final ResourceKey<Enchantment> SOULGUARD_ENCHANTMENT = ResourceKey.create(Registries.ENCHANTMENT, SOULGUARD);

	private SoulfireDeaths() {
	}

	/** Whether this killing damage skips the Totem of Keeping. */
	public static boolean bypassesKeeping(LivingEntity victim, @Nullable DamageSource killing) {
		return killing != null && killing.is(BYPASSES_TOTEMS) && !hasSoulguard(victim);
	}

	static boolean hasSoulguard(LivingEntity entity) {
		boolean effect = BuiltInRegistries.MOB_EFFECT.get(SOULGUARD).map(entity::hasEffect).orElse(false);
		return effect || entity.registryAccess().lookup(Registries.ENCHANTMENT).flatMap(r -> r.get(SOULGUARD_ENCHANTMENT))
				.map(h -> EnchantmentHelper.getEnchantmentLevel(h, entity) > 0).orElse(false);
	}
}
