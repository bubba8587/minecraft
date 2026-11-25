// dte:C1,B15,B17
package io.github.bubba8587.bubbas_reworked_mobs.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import io.github.bubba8587.bubbas_reworked_mobs.BubbasReworkedMobs;
import io.github.bubba8587.bubbas_reworked_mobs.captain.Captains;
import io.github.bubba8587.bubbas_reworked_mobs.captain.SoulfireBlaze;
import io.github.bubba8587.bubbas_reworked_mobs.registry.ModItems;
import io.github.bubba8587.bubbas_reworked_mobs.soulfire.SoulBurning;
import io.github.bubba8587.bubbas_reworked_mobs.soulfire.Soulfire;
import io.github.bubba8587.bubbas_reworked_mobs.soulfire.SoulfireTuning;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.BrewingInput;
import net.minecraft.world.item.crafting.BrewingRecipe;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/** Soulfire burning, Soulguard and the soul blaze items (specs/soulfire.md). Milestone; one core smoke test. */
public class SoulfireGameTest {
	/** A pig penned in glass on (x, 1, z), standing in or on {@code block}. */
	private static Pig penned(GameTestHelper helper, int x, int z, Block floor, net.minecraft.world.level.block.state.BlockState block) {
		helper.setBlock(x, 0, z, floor);
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				if (dx != 0 || dz != 0) {
					helper.setBlock(x + dx, 1, z + dz, Blocks.GLASS);
					helper.setBlock(x + dx, 2, z + dz, Blocks.GLASS);
				}
			}
		}
		helper.setBlock(new BlockPos(x, 1, z), block);
		return helper.spawn(EntityTypes.PIG, new Vec3(x + 0.5, 1.0, z + 0.5));
	}

	private static net.minecraft.world.level.block.state.BlockState litSoulCampfire() {
		return Blocks.SOUL_CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true);
	}

	private static Holder<Enchantment> soulguard(GameTestHelper helper) {
		return helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Soulfire.SOULGUARD_ENCHANTMENT);
	}

	// --- Soulfire burning ------------------------------------------------------------------------------

	/** Core: a mob on a lit soul campfire becomes Soulfire-burning (registered by ReworkedMobsCoreGameTest). */
	public void soulCampfireSetsSoulBurning(GameTestHelper helper) {
		TestFloors.stone(helper);
		Pig pig = penned(helper, 2, 2, Blocks.STONE, litSoulCampfire());
		helper.succeedWhen(() -> helper.assertTrue(pig.isOnFire() && SoulBurning.isSoulBurning(pig), "not soul-burning yet"));
	}

	/** Soul fire sets Soulfire burning; walking into ordinary fire keeps it Soulfire. */
	@GameTest(maxTicks = 100)
	public void normalFireKeepsSoulBurning(GameTestHelper helper) {
		TestFloors.stone(helper);
		Pig pig = penned(helper, 2, 2, Blocks.SOUL_SAND, Blocks.SOUL_FIRE.defaultBlockState());
		penned(helper, 6, 6, Blocks.NETHERRACK, Blocks.FIRE.defaultBlockState()).discard();
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(SoulBurning.isSoulBurning(pig), "not soul-burning in soul fire"))
				.thenExecute(() -> {
					Vec3 at = helper.absoluteVec(new Vec3(6.5, 1.0, 6.5));
					pig.teleportTo(at.x, at.y, at.z);
				})
				.thenIdle(30)
				.thenExecute(() -> helper.assertTrue(pig.isOnFire() && SoulBurning.isSoulBurning(pig), "ordinary fire turned Soulfire back to normal burning"))
				.thenSucceed();
	}

	/** Soulfire burning against the living: normal burn damage plus the soul bonus; undead take normal burn damage. */
	@GameTest(maxTicks = 80)
	public void undeadTakeNormalBurnDamageTheLivingMore(GameTestHelper helper) {
		TestFloors.stone(helper);
		Pig pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new Vec3(1.5, 1.0, 1.5));
		Zombie zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new Vec3(5.5, 1.0, 5.5));
		zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
		Pig plain = helper.spawnWithNoFreeWill(EntityTypes.PIG, new Vec3(1.5, 1.0, 5.5));
		SoulBurning.igniteForSeconds(pig, 10.0F);
		SoulBurning.igniteForSeconds(zombie, 10.0F);
		plain.igniteForSeconds(10.0F);
		helper.assertTrue(SoulBurning.isSoulBurning(pig) && SoulBurning.isSoulBurning(zombie) && !SoulBurning.isSoulBurning(plain), "set up");
		helper.runAfterDelay(70, () -> {
			float living = pig.getMaxHealth() - pig.getHealth();
			float undead = zombie.getMaxHealth() - zombie.getHealth();
			float normal = plain.getMaxHealth() - plain.getHealth();
			helper.assertTrue(normal > 0.0F && Math.abs(undead - normal) < 0.01F, "undead " + undead + " vs normal burning " + normal);
			helper.assertTrue(Math.abs(living - normal * SoulfireTuning.SOUL_BONUS) < 0.01F, "living " + living + " vs normal burning " + normal);
			helper.succeed();
		});
	}

	/** Fire Resistance blocks the fire part of Soulfire burning; the soul bonus still hurts. */
	@GameTest(maxTicks = 80)
	public void fireResistanceLeavesTheSoulPart(GameTestHelper helper) {
		TestFloors.stone(helper);
		Pig soul = helper.spawnWithNoFreeWill(EntityTypes.PIG, new Vec3(1.5, 1.0, 1.5));
		Pig plain = helper.spawnWithNoFreeWill(EntityTypes.PIG, new Vec3(5.5, 1.0, 5.5));
		for (Pig pig : new Pig[] { soul, plain }) {
			pig.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE, 400));
		}
		SoulBurning.igniteForSeconds(soul, 10.0F);
		plain.igniteForSeconds(10.0F);
		helper.runAfterDelay(70, () -> {
			float soulHurt = soul.getMaxHealth() - soul.getHealth();
			float plainHurt = plain.getMaxHealth() - plain.getHealth();
			helper.assertTrue(plainHurt == 0.0F, "normal burning under Fire Resistance hurt " + plainHurt);
			float perSecond = SoulfireTuning.BURN_DAMAGE * (SoulfireTuning.SOUL_BONUS - 1.0F);
			helper.assertTrue(soulHurt >= perSecond * 2 - 0.01F && soulHurt <= perSecond * 4 + 0.01F, "soul part under Fire Resistance " + soulHurt);
			helper.succeed();
		});
	}

	// --- totems -------------------------------------------------------------------------------------

	/**
	 * A Soulfire death isn't saved by a Totem of Undying; an ordinary fire death is; with Soulguard soul fire doesn't hurt.
	 * Zombies, since test players are invulnerable until their client has loaded.
	 */
	@GameTest(maxTicks = 5)
	public void soulfireDeathBypassesTheTotem(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Zombie burned = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new Vec3(1.5, 1.0, 1.5));
		Zombie guarded = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new Vec3(3.5, 1.0, 1.5));
		Zombie plain = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new Vec3(5.5, 1.0, 1.5));
		for (Zombie z : List.of(burned, guarded, plain)) {
			z.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.TOTEM_OF_UNDYING));
			z.setHealth(2.0F);
		}
		guarded.addEffect(new MobEffectInstance(Soulfire.SOULGUARD, 600));
		burned.hurtServer(level, level.damageSources().source(Soulfire.SOUL_BURN), 50.0F);
		plain.hurtServer(level, level.damageSources().onFire(), 50.0F);
		guarded.hurtServer(level, level.damageSources().source(Soulfire.SOUL_FIRE), 50.0F);
		helper.assertTrue(burned.isDeadOrDying(), "a totem saved a Soulfire death");
		helper.assertTrue(!plain.isDeadOrDying() && plain.getMainHandItem().isEmpty(), "the totem didn't save an ordinary fire death");
		helper.assertTrue(!guarded.isDeadOrDying() && guarded.getHealth() == 2.0F, "Soulguard didn't stop soul fire damage");
		helper.succeed();
	}

	// --- Soulguard ----------------------------------------------------------------------------------

	/** The enchantment (on any armour piece), the potion effect and the Captain's Helm each make a mob immune to soul fire. */
	@GameTest(maxTicks = 100)
	public void soulguardMakesImmuneToSoulFire(GameTestHelper helper) {
		TestFloors.stone(helper);
		ItemStack boots = new ItemStack(Items.IRON_BOOTS);
		boots.enchant(soulguard(helper), 1);
		Pig enchanted = penned(helper, 2, 2, Blocks.SOUL_SAND, Blocks.SOUL_FIRE.defaultBlockState());
		enchanted.setItemSlot(EquipmentSlot.FEET, boots);
		Pig potion = penned(helper, 6, 2, Blocks.SOUL_SAND, litSoulCampfire());
		potion.addEffect(new MobEffectInstance(Soulfire.SOULGUARD, 600));
		Pig helm = penned(helper, 2, 6, Blocks.SOUL_SAND, Blocks.SOUL_FIRE.defaultBlockState());
		helm.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.CAPTAINS_HELM));
		Pig fire = penned(helper, 6, 6, Blocks.NETHERRACK, Blocks.FIRE.defaultBlockState());
		fire.addEffect(new MobEffectInstance(Soulfire.SOULGUARD, 600));
		helper.runAfterDelay(80, () -> {
			for (Pig pig : List.of(enchanted, potion, helm)) {
				helper.assertTrue(pig.getHealth() == pig.getMaxHealth() && !SoulBurning.isSoulBurning(pig), "hurt or soul-burning with Soulguard: " + pig);
			}
			helper.assertTrue(fire.getHealth() < fire.getMaxHealth(), "Soulguard stopped ordinary fire");
			helper.succeed();
		});
	}

	/**
	 * With Bubba's Infused Gems (-PwithGems): the Soulguard potion brews a Soulguard Infused Gem, and a Soulguard gem worn,
	 * even at Base Power I, makes a mob immune to soul fire.
	 * The gems come from its /gems kit, by command, so this mod needs no compile dependency. Passes without the mod.
	 */
	@GameTest(maxTicks = 120)
	public void soulguardGemMakesImmuneToSoulFire(GameTestHelper helper) {
		if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("bubbas_infused_gems")) {
			helper.succeed();
			return;
		}
		ItemStack infused = brew(helper, potion(Items.LINGERING_POTION, Soulfire.LONG_SOULGUARD_POTION), new ItemStack(Items.DIAMOND));
		helper.assertTrue(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(infused.getItem()).toString().equals("bubbas_infused_gems:infused_gem")
				&& infused.getComponents().toString().contains("bubbas_reworked_mobs:soulguard"), "the Soulguard potion brewed " + infused);
		TestFloors.stone(helper);
		ServerLevel level = helper.getLevel();
		ServerPlayer player = TestPlayers.survivalAt(helper, 6.5, 6.5);
		TestCleanup.whenDone(helper, () -> TestPlayers.remove(helper, player));
		level.getServer().getCommands().performPrefixedCommand(level.getServer().createCommandSourceStack().withEntity(player).withSuppressedOutput(),
				"gems kit soulguard leather 1");
		ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET).copy();
		helper.assertTrue(boots.is(Items.LEATHER_BOOTS), "no gem kit: " + boots);
		Pig pig = penned(helper, 2, 2, Blocks.SOUL_SAND, Blocks.GLASS.defaultBlockState());
		pig.setItemSlot(EquipmentSlot.FEET, boots);
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(pig.hasEffect(Soulfire.SOULGUARD), "no Soulguard from the gem yet"))
				.thenExecute(() -> helper.setBlock(2, 1, 2, Blocks.SOUL_FIRE))
				.thenIdle(60)
				.thenExecute(() -> helper.assertTrue(pig.getHealth() == pig.getMaxHealth() && !SoulBurning.isSoulBurning(pig),
						"a Soulguard gem wearer was hurt in soul fire: " + pig.getHealth()))
				.thenSucceed();
	}

	/** The Captain's Helm carries the Soulguard enchantment; the enchantment is a treasure: never at the table or from villagers. */
	@GameTest(maxTicks = 5)
	public void helmCarriesSoulguardAndItIsTreasure(GameTestHelper helper) {
		Holder<Enchantment> soulguard = soulguard(helper);
		helper.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(soulguard, new ItemStack(ModItems.CAPTAINS_HELM)) == 1, "the helm has no Soulguard");
		helper.assertTrue(soulguard.is(EnchantmentTags.TREASURE), "not a treasure enchantment");
		helper.assertTrue(!soulguard.is(EnchantmentTags.IN_ENCHANTING_TABLE) && !soulguard.is(EnchantmentTags.TRADEABLE)
				&& !soulguard.is(EnchantmentTags.ON_RANDOM_LOOT), "Soulguard is in a vanilla random pool");
		helper.assertTrue(soulguard.value().canEnchant(new ItemStack(Items.LEATHER_CHESTPLATE)) && soulguard.value().canEnchant(new ItemStack(Items.GOLDEN_BOOTS)),
				"not on any armour");
		helper.succeed();
	}

	/** Soulguard books in Nether chests at about BOOK_CHANCE. */
	@GameTest(maxTicks = 5)
	public void soulguardBooksInNetherChests(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Holder<Enchantment> soulguard = soulguard(helper);
		LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.ZERO).create(LootContextParamSets.CHEST);
		int rolls = 1500;
		for (Identifier id : SoulfireTuning.BOOK_CHESTS) {
			LootTable table = level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, id));
			int books = 0;
			for (int i = 0; i < rolls; i++) {
				for (ItemStack stack : table.getRandomItems(params, i)) {
					if (stack.is(Items.ENCHANTED_BOOK) && stack.getOrDefault(net.minecraft.core.component.DataComponents.STORED_ENCHANTMENTS,
							net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY).getLevel(soulguard) > 0) {
						books++;
					}
				}
			}
			double expected = rolls * SoulfireTuning.BOOK_CHANCE;
			helper.assertTrue(books > expected * 0.6 && books < expected * 1.4, id + ": " + books + " Soulguard books in " + rolls);
		}
		helper.succeed();
	}

	/** Awkward + soul blaze rod: Soulguard 3:00; redstone: 8:00; gunpowder and dragon's breath: splash and lingering. */
	@GameTest(maxTicks = 5)
	public void soulguardPotionsBrew(GameTestHelper helper) {
		ItemStack rod = new ItemStack(Soulfire.SOUL_BLAZE_ROD);
		check(helper, brew(helper, potion(Items.POTION, Potions.AWKWARD), rod), Items.POTION, Soulfire.SOULGUARD_POTION);
		check(helper, brew(helper, potion(Items.SPLASH_POTION, Potions.AWKWARD), rod), Items.SPLASH_POTION, Soulfire.SOULGUARD_POTION);
		check(helper, brew(helper, potion(Items.POTION, Soulfire.SOULGUARD_POTION), new ItemStack(Items.REDSTONE)), Items.POTION, Soulfire.LONG_SOULGUARD_POTION);
		check(helper, brew(helper, potion(Items.POTION, Soulfire.LONG_SOULGUARD_POTION), new ItemStack(Items.GUNPOWDER)), Items.SPLASH_POTION, Soulfire.LONG_SOULGUARD_POTION);
		check(helper, brew(helper, potion(Items.SPLASH_POTION, Soulfire.SOULGUARD_POTION), new ItemStack(Items.DRAGON_BREATH)), Items.LINGERING_POTION, Soulfire.SOULGUARD_POTION);
		helper.assertTrue(Soulfire.SOULGUARD_POTION.value().getEffects().getFirst().getDuration() == 3600
				&& Soulfire.LONG_SOULGUARD_POTION.value().getEffects().getFirst().getDuration() == 9600, "durations");
		helper.succeed();
	}

	private static ItemStack potion(net.minecraft.world.item.Item container, Holder<Potion> potion) {
		return PotionContents.createItemStack(container, potion);
	}

	private static ItemStack brew(GameTestHelper helper, ItemStack input, ItemStack reagent) {
		Optional<RecipeHolder<BrewingRecipe>> recipe = helper.getLevel().recipeAccess()
				.getRecipeFor(RecipeType.BREWING, new BrewingInput(input, reagent), helper.getLevel());
		return recipe.map(r -> r.value().assemble(new BrewingInput(input, reagent))).orElse(ItemStack.EMPTY);
	}

	private static void check(GameTestHelper helper, ItemStack out, net.minecraft.world.item.Item container, Holder<Potion> potion) {
		PotionContents contents = out.get(net.minecraft.core.component.DataComponents.POTION_CONTENTS);
		helper.assertTrue(out.is(container) && contents != null && contents.is(potion), "brewed " + out + " " + contents + ", expected " + potion);
	}

	// --- soul blaze materials ------------------------------------------------------------------------

	/** Soulfire Blazes (natural and summoned) drop soul blaze rods at blaze odds, never blaze rods. */
	@GameTest(maxTicks = 5)
	public void soulfireBlazesDropSoulRods(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer killer = helper.makeMockServerPlayerInLevel();
		ResourceKey<LootTable> table = ResourceKey.create(Registries.LOOT_TABLE, BubbasReworkedMobs.id("entities/soulfire_blaze"));
		for (boolean summoned : new boolean[] {false, true}) {
			SoulfireBlaze blaze = Captains.SOULFIRE_BLAZE.create(level, EntitySpawnReason.COMMAND);
			blaze.snapTo(helper.absoluteVec(new Vec3(2.5, 1.0, 2.5)));
			if (summoned) {
				blaze.linkTo(Captains.WITHER_CAPTAIN.create(level, EntitySpawnReason.COMMAND));
			}
			blaze.setLastHurtByPlayer(killer, 100);
			int rods = 0;
			int rolls = 400;
			for (int i = 0; i < rolls; i++) {
				List<ItemStack> drops = new ArrayList<>();
				blaze.dropFromLootTable(level, level.damageSources().playerAttack(killer), true, table, drops::add);
				helper.assertTrue(drops.stream().noneMatch(s -> s.is(Items.BLAZE_ROD)), "a blaze rod");
				rods += drops.stream().filter(s -> s.is(Soulfire.SOUL_BLAZE_ROD)).mapToInt(ItemStack::getCount).sum();
			}
			helper.assertTrue(rods > rolls * 0.35 && rods < rolls * 0.65, (summoned ? "summoned: " : "natural: ") + rods + " rods in " + rolls);
		}
		helper.succeed();
	}

	/** A rod crafts two powder; powder, gunpowder and coal or charcoal craft three Soul Fire Charges. */
	@GameTest(maxTicks = 5)
	public void soulBlazeItemsCraft(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ItemStack powder = craft(helper, CraftingInput.of(1, 1, List.of(new ItemStack(Soulfire.SOUL_BLAZE_ROD))));
		helper.assertTrue(powder.is(Soulfire.SOUL_BLAZE_POWDER) && powder.getCount() == 2, "rod made " + powder);
		for (net.minecraft.world.item.Item coal : List.of(Items.COAL, Items.CHARCOAL)) {
			ItemStack charges = craft(helper, CraftingInput.of(3, 1, List.of(new ItemStack(Items.GUNPOWDER), new ItemStack(Soulfire.SOUL_BLAZE_POWDER), new ItemStack(coal))));
			helper.assertTrue(charges.is(Soulfire.SOUL_FIRE_CHARGE) && charges.getCount() == 3, "made " + charges);
		}
		helper.succeed();
	}

	private static ItemStack craft(GameTestHelper helper, CraftingInput input) {
		var holder = helper.getLevel().recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel()).orElse(null);
		helper.assertTrue(holder != null, "no recipe for " + input);
		@SuppressWarnings("unchecked")
		ItemStack out = ((Recipe<CraftingInput>) holder.value()).assemble(input);
		return out;
	}

	/** Soul blaze powder fuels a brewing stand for 40 brews; a soul blaze rod burns 24 items' worth in a furnace. */
	@GameTest(maxTicks = 20)
	public void soulBlazeFuels(GameTestHelper helper) {
		BlockPos standPos = new BlockPos(1, 1, 1);
		helper.setBlock(standPos, Blocks.BREWING_STAND);
		BrewingStandBlockEntity stand = helper.getBlockEntity(standPos, BrewingStandBlockEntity.class);
		stand.setItem(4, new ItemStack(Soulfire.SOUL_BLAZE_POWDER));
		BlockPos furnacePos = new BlockPos(3, 1, 1);
		helper.setBlock(furnacePos, Blocks.FURNACE);
		FurnaceBlockEntity furnace = helper.getBlockEntity(furnacePos, FurnaceBlockEntity.class);
		furnace.setItem(0, new ItemStack(Items.COBBLESTONE, 64));
		furnace.setItem(1, new ItemStack(Soulfire.SOUL_BLAZE_ROD));
		helper.runAfterDelay(5, () -> {
			var registries = helper.getLevel().registryAccess();
			var standData = stand.saveWithoutMetadata(registries);
			helper.assertTrue(standData.getIntOr("Fuel", 0) == 40, "brewing fuel " + standData.getIntOr("Fuel", 0));
			var furnaceData = furnace.saveWithoutMetadata(registries);
			int total = furnaceData.getIntOr("lit_total_time", 0);
			helper.assertTrue(total == 24 * 200, "furnace burn time " + total + " " + furnaceData);
			helper.succeed();
		});
	}

	/** Thrown and dispensed Soul Fire Charges place soul fire on soul sand and fire on netherrack. */
	@GameTest(maxTicks = 80)
	public void soulFireChargesThrownAndDispensed(GameTestHelper helper) {
		TestFloors.stone(helper);
		helper.setBlock(1, 0, 1, Blocks.SOUL_SAND);
		helper.setBlock(5, 0, 1, Blocks.NETHERRACK);
		for (int x : new int[] {1, 5}) {
			BlockPos at = new BlockPos(x, 4, 1);
			helper.setBlock(at, Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.DOWN));
			helper.getBlockEntity(at, DispenserBlockEntity.class).setItem(0, new ItemStack(Soulfire.SOUL_FIRE_CHARGE));
			helper.setBlock(at.above(), Blocks.REDSTONE_BLOCK);
		}
		// Thrown by a player, straight down onto soul sand
		helper.setBlock(1, 0, 6, Blocks.SOUL_SAND);
		ServerPlayer player = TestPlayers.survivalAt(helper, 1.5, 6.5);
		TestCleanup.whenDone(helper, () -> TestPlayers.remove(helper, player));
		Vec3 above = helper.absoluteVec(new Vec3(1.5, 4.0, 6.5));
		player.snapTo(above.x, above.y, above.z, 0.0F, 90.0F);
		player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Soulfire.SOUL_FIRE_CHARGE));
		player.getMainHandItem().use(helper.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND);
		helper.succeedWhen(() -> {
			helper.assertBlockPresent(Blocks.SOUL_FIRE, 1, 1, 1);
			helper.assertBlockPresent(Blocks.FIRE, 5, 1, 1);
			helper.assertBlockPresent(Blocks.SOUL_FIRE, 1, 1, 6);
		});
	}

	/** A Soulfire charge's hit sets Soulfire burning, except with Soulguard. */
	@GameTest(maxTicks = 60)
	public void chargeHitSetsSoulBurning(GameTestHelper helper) {
		TestFloors.stone(helper);
		ServerLevel level = helper.getLevel();
		Pig pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new Vec3(1.5, 1.0, 1.5));
		Pig guarded = helper.spawnWithNoFreeWill(EntityTypes.PIG, new Vec3(5.5, 1.0, 1.5));
		guarded.addEffect(new MobEffectInstance(Soulfire.SOULGUARD, 600));
		for (LivingEntity target : List.of(pig, guarded)) {
			var charge = Captains.SOULFIRE_CHARGE.create(level, EntitySpawnReason.TRIGGERED);
			Vec3 at = target.position().add(0.0, 3.0, 0.0);
			charge.snapTo(at.x, at.y, at.z, 0.0F, 90.0F);
			charge.setDeltaMovement(0.0, -charge.accelerationPower, 0.0);
			level.addFreshEntity(charge);
		}
		helper.succeedWhen(() -> {
			helper.assertTrue(pig.getHealth() < pig.getMaxHealth() && SoulBurning.isSoulBurning(pig), "the pig isn't soul-burning");
			helper.assertTrue(guarded.getHealth() < guarded.getMaxHealth() && !guarded.isOnFire(), "the guarded pig wasn't hit or is burning");
		});
	}
}
