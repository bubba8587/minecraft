// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.test;

import io.github.bubba8587.bubbas_reworked_mobs.BubbasReworkedMobs;
import io.github.bubba8587.bubbas_reworked_mobs.creeper.look.CreeperSkins;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.commands.FillBiomeCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

/** Biome creeper skins (specs/creepers.md): picked from the spawn biome and kept across a save and load. */
public class BiomeCreeperGameTest {
	/** Birch forest and desert creepers get those skins and keep them through a save round trip. */
	public void birchAndDesertCreepersKeepTheirSkins(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos at = helper.absolutePos(new BlockPos(1, 1, 1));
		Holder<Biome> before = level.getBiome(at);
		try {
			check(helper, at, Biomes.BIRCH_FOREST, "birch");
			check(helper, at, Biomes.DESERT, "sand");
			check(helper, at, Biomes.NETHER_WASTES, "vanilla");
		} finally {
			FillBiomeCommand.fill(level, at.offset(-2, -2, -2), at.offset(2, 2, 2), before);
		}
		helper.succeed();
	}

	private static void check(GameTestHelper helper, BlockPos at, ResourceKey<Biome> biome, String expected) {
		ServerLevel level = helper.getLevel();
		FillBiomeCommand.fill(level, at.offset(-2, -2, -2), at.offset(2, 2, 2), level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(biome));
		Creeper creeper = helper.spawn(EntityTypes.CREEPER, new BlockPos(1, 1, 1));
		Identifier want = BubbasReworkedMobs.id(expected);
		helper.assertTrue(CreeperSkins.skin(creeper).equals(want), biome.identifier() + " creeper skin " + CreeperSkins.skin(creeper));
		var output = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
		creeper.saveWithoutId(output);
		Creeper loaded = EntityTypes.CREEPER.create(level, EntitySpawnReason.LOAD);
		loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), output.buildResult()));
		helper.assertTrue(loaded.hasAttached(CreeperSkins.SKIN) && CreeperSkins.skin(loaded).equals(want),
				biome.identifier() + " skin after load " + loaded.getAttached(CreeperSkins.SKIN));
		loaded.discard();
		creeper.discard();
	}
}
