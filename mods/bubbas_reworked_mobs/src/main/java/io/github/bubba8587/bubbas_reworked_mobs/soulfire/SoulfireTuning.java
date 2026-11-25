// dte:C1,B15,B17
package io.github.bubba8587.bubbas_reworked_mobs.soulfire;

import java.util.List;

import net.minecraft.resources.Identifier;

/** Every Soulfire, Soulguard and soul blaze number the owner tunes by playing (specs/soulfire.md). */
public final class SoulfireTuning {
	private SoulfireTuning() {
	}

	// --- Soulfire burning ------------------------------------------------------------------------------

	/** The living take normal burn damage times this while soul-burning; undead take normal burn damage. */
	public static final float SOUL_BONUS = 1.5F;
	/** Normal burning: 1 damage a second (Entity.baseTick). */
	public static final float BURN_DAMAGE = 1.0F;
	/** Undead in soul fire / on a lit soul campfire take normal fire / campfire damage (vanilla's soul blocks: 2). */
	public static final float UNDEAD_FIRE_CONTACT_DAMAGE = 1.0F;
	public static final float UNDEAD_CAMPFIRE_CONTACT_DAMAGE = 1.0F;
	/** Soul sounds while soul-burning: a 1 in SOUND_ONE_IN chance every SOUND_INTERVAL ticks. */
	public static final int SOUND_INTERVAL = 20;
	public static final int SOUND_ONE_IN = 2;

	// --- soul blaze materials -------------------------------------------------------------------------
	// Data, not code: rod burn time 4800 ticks = 24 items (context_int_provider/cooking/time_soul_blaze_rod.json; a blaze
	// rod 2400 = 12), powder 40 brews (context_int_provider/brewing/uses_soul_blaze_powder.json; blaze powder 20), rods
	// from Soulfire Blazes (loot_table/entities/soulfire_blaze.json).

	/** Seconds before a player can throw another Soul Fire Charge (a wind charge: 0.5). */
	public static final float THROW_COOLDOWN_SECONDS = 0.5F;

	// --- Eye of the Soul: Phasing ----------------------------------------------------------------------

	/** Phasing from one Eye of the Soul (ticks): 10 s. */
	public static final int PHASING_TICKS = 200;
	/** Phasing blindness fades in over this many ticks, and out over the last this many (vanilla's effect blend, as Darkness). */
	public static final int PHASING_FADE_IN_TICKS = 10;
	public static final int PHASING_FADE_OUT_TICKS = 40;
	/** Blocks walked between soul sounds while passing through blocks. */
	public static final float PHASING_SOUND_STRIDE = 1.5F;
	/** Eyes of the Soul per stack, and seconds before a player can use another. */
	public static final int EYE_STACK_SIZE = 16;
	public static final float EYE_COOLDOWN_SECONDS = 15.0F;
	/** Phasing effect colour: a deep soul teal. */
	public static final int PHASING_COLOR = 0x0B5F63;

	// --- Soulguard ------------------------------------------------------------------------------------

	/** Potion of Soulguard (ticks): 3:00, and 8:00 with redstone. */
	public static final int POTION_TICKS = 3600;
	public static final int LONG_POTION_TICKS = 9600;
	/** Potion and effect colour: vanilla soul fire mid-cyan. */
	public static final int COLOR = 0x01A7AC;
	/** Chance a Soulguard book is added to each of these chests. */
	public static final float BOOK_CHANCE = 0.04F;
	public static final List<Identifier> BOOK_CHESTS = List.of(
			Identifier.withDefaultNamespace("chests/bastion_treasure"),
			Identifier.withDefaultNamespace("chests/bastion_other"),
			Identifier.withDefaultNamespace("chests/bastion_bridge"),
			Identifier.withDefaultNamespace("chests/bastion_hoglin_stable"),
			Identifier.withDefaultNamespace("chests/nether_bridge"),
			Identifier.fromNamespaceAndPath("bubbas_reworked_mobs", "chests/nether_outpost_room"));
}
