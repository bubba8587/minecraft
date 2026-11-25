// dte:C4,C1
package io.github.bubba8587.bubbas_infused_gems.test;

import java.util.Optional;

import io.github.bubba8587.bubbas_infused_gems.balance.Balance;
import io.github.bubba8587.bubbas_infused_gems.gem.GemForm;
import io.github.bubba8587.bubbas_infused_gems.gem.InfusedGem;
import io.github.bubba8587.bubbas_infused_gems.registry.ModComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.crafting.BrewingInput;
import net.minecraft.world.item.crafting.BrewingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;

/**
 * The Soulguard gem (specs/soulfire.md, specs/effect-gems.md). Without Bubba's Reworked Mobs (these game tests' usual
 * run) the gem is absent; with it the gem brews and protects (also checked from Reworked Mobs' side: its
 * SoulfireGameTest with -PwithGems). Milestone.
 */
public class SoulguardGemGameTest {
	private static final boolean MOBS = FabricLoader.getInstance().isModLoaded("bubbas_reworked_mobs");

	private static Holder<MobEffect> soulguard() {
		return BuiltInRegistries.MOB_EFFECT.get(Identifier.parse("bubbas_reworked_mobs:soulguard")).orElseThrow();
	}

	/** Brewed from the Soulguard potion like other potion gems; without Reworked Mobs there is no Soulguard gem at all. */
	@GameTest
	public void soulguardGemBrewsFromItsPotion(GameTestHelper helper) {
		Balance b = Balance.get();
		if (!MOBS) {
			GemKit.check(helper, b.effect("soulguard") == null && !b.brewable.contains("soulguard"), "a Soulguard gem without Reworked Mobs");
			helper.succeed();
			return;
		}
		Holder<Potion> potion = BuiltInRegistries.POTION.get(Identifier.parse("bubbas_reworked_mobs:long_soulguard")).orElseThrow();
		BrewingInput input = new BrewingInput(PotionContents.createItemStack(Items.LINGERING_POTION, potion), new ItemStack(Items.DIAMOND));
		Optional<RecipeHolder<BrewingRecipe>> recipe = helper.getLevel().recipeAccess().getRecipeFor(RecipeType.BREWING, input, helper.getLevel());
		ItemStack out = recipe.map(r -> r.value().assemble(input)).orElse(ItemStack.EMPTY);
		InfusedGem gem = out.get(ModComponents.INFUSED_GEM);
		GemKit.check(helper, gem != null && gem.key().equals("soulguard") && gem.variant().equals("long") && gem.form() == GemForm.LINGERING
				&& gem.infusion() == 4, "brewed " + out + " " + gem);
		helper.succeed();
	}

	/** Any Soulguard gem worn, however weak, gives the full Soulguard effect: no damage in soul fire. */
	@GameTest(maxTicks = 100)
	public void anySoulguardGemGivesFullImmunity(GameTestHelper helper) {
		if (!MOBS) {
			helper.succeed();
			return;
		}
		BlockPos at = new BlockPos(2, 1, 2);
		helper.setBlock(at.below(), Blocks.SOUL_SAND);
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				if (dx != 0 || dz != 0) {
					helper.setBlock(at.offset(dx, 0, dz), Blocks.GLASS);
					helper.setBlock(at.offset(dx, 1, dz), Blocks.GLASS);
				}
			}
		}
		Pig pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, at);
		pig.setItemSlot(EquipmentSlot.FEET, GemKit.socketed(Items.LEATHER_BOOTS, GemKit.modelGem("soulguard", "normal", "amethyst", "weak")));
		helper.startSequence()
				.thenWaitUntil(() -> GemKit.check(helper, pig.hasEffect(soulguard()), "no Soulguard from the gem yet"))
				.thenExecute(() -> helper.setBlock(at, Blocks.SOUL_FIRE))
				.thenIdle(60)
				.thenExecute(() -> GemKit.check(helper, pig.getHealth() == pig.getMaxHealth() && !pig.isOnFire(),
						"a Soulguard gem wearer was hurt or burning in soul fire: " + pig.getHealth()))
				.thenSucceed();
	}
}
