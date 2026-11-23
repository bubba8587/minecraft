// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.test;

import io.github.bubba8587.bubbas_reworked_mobs.grave.CryptYardPiece;
import io.github.bubba8587.bubbas_reworked_mobs.grave.GraveyardCommand;
import io.github.bubba8587.bubbas_reworked_mobs.grave.YardKinds;
import io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper.SleeperBlockEntity;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/** Noble dead in gemmed armor: gems with Infused Gems installed (-PwithGems), plain gear without. Milestone. */
public class NobleFineryGameTest {
	@GameTest(structure = CampsGameTest.FIELD, maxTicks = 20)
	public void nobleDeadWearGemsOnlyWithInfusedGems(GameTestHelper helper) {
		var level = helper.getLevel();
		CryptYardPiece piece = GraveyardCommand.build(level, helper.absolutePos(new BlockPos(16, 7, 1)), 71L, 15, 13, true, YardKinds.State.OVERGROWN,
				YardKinds.Wealth.NOBLE);
		BoundingBox box = piece.getBoundingBox();
		int armoured = 0;
		int gemmed = 0;
		for (BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
			if (level.getBlockEntity(pos) instanceof SleeperBlockEntity sleeper && !sleeper.getGear(EquipmentSlot.LEGS).isEmpty()) {
				armoured++;
				for (EquipmentSlot slot : SleeperBlockEntity.SLOTS) {
					ItemStack stack = sleeper.getGear(slot);
					if (!stack.isEmpty() && hasGems(stack)) {
						gemmed++;
						break;
					}
				}
			}
		}
		helper.assertTrue(armoured > 0, "the noble crypt should hold sleepers in full armor");
		if (FabricLoader.getInstance().isModLoaded("bubbas_infused_gems")) {
			helper.assertTrue(gemmed * 2 > armoured, "with Infused Gems most noble sleepers' gear carries gems: " + gemmed + " of " + armoured);
		} else {
			helper.assertTrue(gemmed == 0, "without Infused Gems the gear stays plain: " + gemmed);
		}
		helper.succeed();
	}

	/** Whether the stack carries any of Infused Gems' data. */
	private static boolean hasGems(ItemStack stack) {
		return !stack.getComponentsPatch().forget(type -> {
			var id = net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type);
			return id == null || !id.getNamespace().equals("bubbas_infused_gems");
		}).isEmpty();
	}
}
