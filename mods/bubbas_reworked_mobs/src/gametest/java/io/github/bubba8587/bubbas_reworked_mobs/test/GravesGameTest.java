// dte:C1,B15
package io.github.bubba8587.bubbas_reworked_mobs.test;

import java.util.List;

import io.github.bubba8587.bubbas_reworked_mobs.grave.CryptYardPiece;
import io.github.bubba8587.bubbas_reworked_mobs.grave.GraveText;
import io.github.bubba8587.bubbas_reworked_mobs.grave.GraveyardCommand;
import io.github.bubba8587.bubbas_reworked_mobs.grave.GravestoneBlockEntity;
import io.github.bubba8587.bubbas_reworked_mobs.registry.ModBlocks;
import io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper.SleeperBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.entity.SignTextSlot;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * Gravestones and crypt yards (specs/graveyards.md). The yard test is core (through ReworkedMobsCoreGameTest); the rest
 * is milestone. The crypt below a yard is checked by eye in the client part {@code graves} (the test fields are too
 * shallow for it).
 */
public class GravesGameTest {
	private static final String FIELD = EndermanGameTest.FIELD;

	/**
	 * Core. A built yard (19 x 13, no crypt) has gravestones carrying only glyphs or nothing (never real names) and
	 * sleepers in its graves.
	 */
	public void yardHasStonesWithoutNamesAndSleepers(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		// 19 wide (six grave columns) so that graves skipped at a chunk border, which falls differently wherever the
		// test field is, still leave several.
		CryptYardPiece piece = GraveyardCommand.build(level, helper.absolutePos(new BlockPos(11, 0, 1)), false, 4242L, 19, 13);
		BoundingBox box = piece.getBoundingBox();
		int stones = 0;
		int sleepers = 0;
		for (BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
			var state = level.getBlockState(pos);
			if (state.is(ModBlocks.GRAVESTONE)) {
				stones++;
				helper.assertTrue(level.getBlockEntity(pos) instanceof GravestoneBlockEntity stone && GraveText.noRealNames(stone.getText(SignTextSlot.FRONT))
						&& !stone.getText(SignTextSlot.BACK).hasMessage(false), "a generated gravestone carries readable text at " + pos.toShortString());
			} else if (state.getBlock() instanceof SleeperBlock) {
				sleepers++;
			}
		}
		helper.assertTrue(stones >= 2, "expected a few gravestones, found " + stones);
		helper.assertTrue(sleepers >= 1, "expected sleepers in the graves, found none");
		helper.succeed();
	}

	/** Worldgen text is only ever blank or glyphs, over many stones. */
	@GameTest(maxTicks = 5)
	public void generatedTextIsGlyphsOrBlank(GameTestHelper helper) {
		RandomSource random = RandomSource.create(7L);
		int blank = 0;
		for (int i = 0; i < 2000; i++) {
			SignText text = GraveText.random(random, 0.4F);
			helper.assertTrue(GraveText.noRealNames(text), "generated readable text: " + text.getMessages(false));
			if (!text.hasMessage(false)) {
				blank++;
			}
		}
		helper.assertTrue(blank > 600 && blank < 1000, "blank share off: " + blank + " of 2000");
		// A player's name is readable text: the check catches it.
		SignText named = new SignText(List.of(Component.literal("Steve"), Component.empty(), Component.empty(), Component.empty()),
				List.of(Component.literal("Steve"), Component.empty(), Component.empty(), Component.empty()), DyeColor.BLACK, false);
		helper.assertFalse(GraveText.noRealNames(named), "readable text passed the no-names check");
		helper.succeed();
	}

	/**
	 * A gravestone takes any of the 16 rotations, keeps text front and back (and its dye and glow) through a save and
	 * load, and drops itself when mined with a pickaxe.
	 */
	@GameTest(structure = FIELD, maxTicks = 20)
	public void gravestoneIsASign(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		for (int r = 0; r < 16; r++) {
			BlockPos pos = new BlockPos(2 + r, 1, 3);
			helper.setBlock(pos, ModBlocks.GRAVESTONE.defaultBlockState().setValue(StandingSignBlock.ROTATION, r));
			helper.assertTrue(helper.getBlockState(pos).getValue(StandingSignBlock.ROTATION) == r, "rotation " + r + " didn't stick");
		}
		BlockPos at = helper.absolutePos(new BlockPos(2, 1, 3));
		GravestoneBlockEntity stone = (GravestoneBlockEntity) level.getBlockEntity(at);
		List<Component> front = List.of(Component.literal("Here lies"), Component.literal("Bramble"), Component.empty(), Component.empty());
		List<Component> back = List.of(Component.empty(), Component.literal("back"), Component.empty(), Component.empty());
		stone.setText(new SignText(front, front, DyeColor.RED, true), SignTextSlot.FRONT);
		stone.setText(new SignText(back, back, DyeColor.BLACK, false), SignTextSlot.BACK);
		CompoundTag saved = stone.saveWithFullMetadata(level.registryAccess());
		BlockEntity loaded = BlockEntity.loadStatic(at, stone.getBlockState(), saved, level.registryAccess());
		helper.assertTrue(loaded instanceof GravestoneBlockEntity, "didn't load as a gravestone: " + loaded);
		GravestoneBlockEntity copy = (GravestoneBlockEntity) loaded;
		helper.assertTrue(copy.getText(SignTextSlot.FRONT).getMessages(false).get(1).getString().equals("Bramble")
				&& copy.getText(SignTextSlot.FRONT).getColor() == DyeColor.RED && copy.getText(SignTextSlot.FRONT).hasGlowingText(), "front text lost");
		helper.assertTrue(copy.getText(SignTextSlot.BACK).getMessages(false).get(1).getString().equals("back"), "back text lost");
		List<ItemStack> drops = Block.getDrops(stone.getBlockState(), level, at, stone, null, new ItemStack(Items.IRON_PICKAXE));
		helper.assertTrue(drops.size() == 1 && drops.getFirst().is(ModBlocks.GRAVESTONE.asItem()), "mined, it dropped " + drops);
		helper.succeed();
	}
}
