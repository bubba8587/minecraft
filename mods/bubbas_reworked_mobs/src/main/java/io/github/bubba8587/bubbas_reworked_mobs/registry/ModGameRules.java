// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.registry;

import io.github.bubba8587.bubbas_reworked_mobs.BubbasReworkedMobs;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;

import net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder;

public final class ModGameRules {
	/** Off switch for the natural monster cap cut (for spawn-control mods). */
	public static final GameRule<Boolean> CUT_MONSTER_SPAWNS = GameRuleBuilder.forBoolean(true)
			.category(GameRuleCategory.SPAWNING).buildAndRegister(BubbasReworkedMobs.id("cut_monster_spawns"));
	/** Natural monster cap as a percentage of vanilla's (structure spawn areas keep the full cap). */
	public static final GameRule<Integer> MONSTER_CAP_PERCENT = GameRuleBuilder.forInteger(15).range(0, 100)
			.category(GameRuleCategory.SPAWNING).buildAndRegister(BubbasReworkedMobs.id("monster_cap_percent"));

	/** Creepers come as watchers (one per player at night, behind cover) instead of vanilla's random natural spawns (specs/creepers.md). */
	public static final GameRule<Boolean> WATCHER_CREEPERS = GameRuleBuilder.forBoolean(true)
			.category(GameRuleCategory.SPAWNING).buildAndRegister(BubbasReworkedMobs.id("watcher_creepers"));

	/** Overworld endermen spawn at cairn sites instead of vanilla's random natural spawns (specs/endermen.md, D2). */
	public static final GameRule<Boolean> CAIRN_ENDERMEN = GameRuleBuilder.forBoolean(true)
			.category(GameRuleCategory.SPAWNING).buildAndRegister(BubbasReworkedMobs.id("cairn_endermen"));

	/** Visible meters: a drowned's grip (blue, also on the held player's screen) and a creeper's startle level (red) as bars above them (owner, 2026-11-16; off by default). */
	public static final GameRule<Boolean> SHOW_METERS = GameRuleBuilder.forBoolean(false)
			.category(GameRuleCategory.MOBS).buildAndRegister(BubbasReworkedMobs.id("show_meters"));

	private ModGameRules() {
	}

	public static void initialize() {
	}
}
