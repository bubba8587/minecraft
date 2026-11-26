// dte:C3
package io.github.bubba8587.bubbas_extra_recipes.test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.github.bubba8587.bubbas_extra_recipes.BubbasExtraRecipes;
import io.github.bubba8587.bubbas_extra_recipes.glider.GliderHandling;
import io.github.bubba8587.bubbas_extra_recipes.registry.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/** The Glider: its components, wear and handling. */
public class GliderGameTest {
	private static final Method FALL_FLYING_STEP;
	private static final Method UPDATE_FALL_FLYING;
	private static final Field FALL_FLY_TICKS;

	static {
		try {
			FALL_FLYING_STEP = LivingEntity.class.getDeclaredMethod("updateFallFlyingMovement", Vec3.class);
			FALL_FLYING_STEP.setAccessible(true);
			UPDATE_FALL_FLYING = LivingEntity.class.getDeclaredMethod("updateFallFlying");
			UPDATE_FALL_FLYING.setAccessible(true);
			FALL_FLY_TICKS = LivingEntity.class.getDeclaredField("fallFlyTicks");
			FALL_FLY_TICKS.setAccessible(true);
		} catch (NoSuchMethodException | NoSuchFieldException e) {
			throw new IllegalStateException(e);
		}
	}

	@GameTest
	public void gliderHasGliderEquippableAndDurability(GameTestHelper helper) {
		ItemStack glider = new ItemStack(ModItems.GLIDER);
		helper.assertTrue(glider.has(DataComponents.GLIDER), "no glider component");
		Equippable equippable = glider.get(DataComponents.EQUIPPABLE);
		helper.assertTrue(equippable != null && equippable.slot() == EquipmentSlot.CHEST, "equippable: " + equippable);
		helper.assertValueEqual(equippable.assetId().orElseThrow(), ModItems.GLIDER_ASSET, "equipment asset");
		helper.assertTrue(glider.isDamageableItem() && glider.getDamageValue() == 0, "glider not damageable: " + glider.getComponents());
		helper.assertValueEqual(glider.getMaxDamage(), ModItems.GLIDER_DURABILITY, "max durability");
		helper.assertTrue(glider.isValidRepairItem(new ItemStack(Items.PHANTOM_MEMBRANE)), "not repaired by phantom membrane");
		helper.assertTrue(!glider.isValidRepairItem(new ItemStack(Items.DIAMOND)), "repaired by diamond");
		helper.assertTrue(glider.is(ItemTags.DURABILITY_ENCHANTABLE) && glider.is(ItemTags.EQUIPPABLE_ENCHANTABLE),
				"not in the durability/equippable enchantable tags (Mending, Unbreaking, curses)");
		helper.assertTrue(LivingEntity.canGlideUsing(glider, EquipmentSlot.CHEST), "cannot glide using the glider in the chest slot");
		helper.assertValueEqual(glider.getMaxStackSize(), 1, "stack size");
		helper.succeed();
	}

	@GameTest
	public void gliderWearsWhileGlidingAndStopsAtOneDurability(GameTestHelper helper) throws ReflectiveOperationException {
		ServerPlayer player = flier(helper, ModItems.GLIDER, 0.0F, 0.0F, new Vec3(0.5, 0.0, 0.0));
		player.setGameMode(GameType.SURVIVAL);
		player.startFallFlying();
		helper.assertTrue(player.isFallFlying(), "not fall flying");
		for (int t = 0; t < 200; t++) {
			fallFly(player, t);
		}
		ItemStack worn = player.getItemBySlot(EquipmentSlot.CHEST);
		helper.assertValueEqual(worn.getDamageValue(), 10, "damage after 200 gliding ticks (one point a second)");

		worn.setDamageValue(ModItems.GLIDER_DURABILITY - 2);
		fallFly(player, 19);
		worn = player.getItemBySlot(EquipmentSlot.CHEST);
		helper.assertTrue(worn.is(ModItems.GLIDER) && worn.getDamageValue() == ModItems.GLIDER_DURABILITY - 1,
				"glider at its last point: " + worn + " damage " + worn.getDamageValue());
		fallFly(player, 20);
		helper.assertTrue(!player.isFallFlying(), "still fall flying on a glider with 1 durability");
		worn = player.getItemBySlot(EquipmentSlot.CHEST);
		helper.assertTrue(worn.is(ModItems.GLIDER) && worn.getDamageValue() == ModItems.GLIDER_DURABILITY - 1,
				"glider broke or kept wearing: " + worn);
		helper.assertTrue(!LivingEntity.canGlideUsing(worn, EquipmentSlot.CHEST), "can glide at 1 durability");
		helper.succeed();
	}

	/** One server step of the game's fall-flying upkeep at the given gliding tick. */
	private static void fallFly(ServerPlayer player, int tick) throws ReflectiveOperationException {
		FALL_FLY_TICKS.setInt(player, tick);
		UPDATE_FALL_FLYING.invoke(player);
	}

	@GameTest
	public void gliderRecipeCraftsOneGlider(GameTestHelper helper) {
		var key = ResourceKey.create(Registries.RECIPE, BubbasExtraRecipes.id("glider"));
		RecipeHolder<?> holder = helper.getLevel().recipeAccess().byKey(key).orElse(null);
		helper.assertTrue(holder != null, "recipe " + key.identifier() + " not loaded");
		Map<Character, Item> k = Map.of('#', Items.DIAMOND, 'B', Items.BAMBOO, 'P', Items.PHANTOM_MEMBRANE, 'F', Items.FEATHER);
		List<ItemStack> grid = new ArrayList<>();
		for (String row : List.of("#B#", "P P", "F F")) {
			for (char c : row.toCharArray()) {
				grid.add(c == ' ' ? ItemStack.EMPTY : new ItemStack(k.get(c)));
			}
		}
		CraftingInput input = CraftingInput.of(3, 3, grid);
		var matching = helper.getLevel().recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel()).orElse(null);
		helper.assertTrue(matching != null && matching.id().equals(key), "recipe matched for the glider grid: " + matching);
		@SuppressWarnings("unchecked")
		ItemStack out = ((Recipe<CraftingInput>) holder.value()).assemble(input);
		helper.assertTrue(out.is(ModItems.GLIDER) && out.getCount() == 1, "glider recipe made " + out);
		helper.succeed();
	}

	private static ServerPlayer flier(GameTestHelper helper, Item chest, float yaw, float pitch, Vec3 velocity) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		Vec3 pos = helper.absoluteVec(new Vec3(1.5, 3, 1.5));
		player.snapTo(pos.x, pos.y, pos.z, yaw, pitch);
		player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(chest));
		player.startFallFlying();
		player.setDeltaMovement(velocity);
		return player;
	}

	/** Velocity after ticks steps of the game's fall-flying movement. */
	private static Vec3 glide(ServerPlayer player, Vec3 velocity, int ticks) throws ReflectiveOperationException {
		for (int i = 0; i < ticks; i++) {
			velocity = (Vec3) FALL_FLYING_STEP.invoke(player, velocity);
		}
		return velocity;
	}

	/** Angle between the horizontal velocity and the horizontal look direction. */
	private static double headingError(ServerPlayer player, Vec3 v) {
		Vec3 look = player.getLookAngle();
		double a = Math.atan2(v.z, v.x) - Math.atan2(look.z, look.x);
		return Math.abs(Math.toDegrees(Math.atan2(Math.sin(a), Math.cos(a))));
	}

	// Core test: registered by ExtraRecipesCoreGameTest
	public void gliderTurnsSlowerThanElytra(GameTestHelper helper) throws ReflectiveOperationException {
		Vec3 start = new Vec3(1.0, 0.0, 0.0);
		ServerPlayer elytra = flier(helper, Items.ELYTRA, 180.0F, 0.0F, start);
		ServerPlayer glider = flier(helper, ModItems.GLIDER, 180.0F, 0.0F, start);
		helper.assertTrue(elytra.isFallFlying() && glider.isFallFlying(), "not fall flying");
		helper.assertTrue(GliderHandling.isWearingGlider(glider) && !GliderHandling.isWearingGlider(elytra), "chest items");

		Vec3 e = glide(elytra, start, 20);
		Vec3 g = glide(glider, start, 20);
		double eErr = headingError(elytra, e);
		double gErr = headingError(glider, g);
		helper.assertTrue(gErr > eErr + 10.0, "after 20 ticks: elytra is %.1f degrees off its look, glider %.1f".formatted(eErr, gErr));
		// Vanilla blend 10 %/tick
		helper.assertTrue(Math.abs(e.x) < 0.15 && Math.abs(g.x) > 0.22, "sideways speed left: elytra %.3f, glider %.3f".formatted(e.x, g.x));
		// Lift and sink stay vanilla: the vertical speed is the same
		helper.assertTrue(Math.abs(e.y - g.y) < 1e-9, "vertical speed differs: elytra %.4f, glider %.4f".formatted(e.y, g.y));
		helper.succeed();
	}

	@GameTest
	public void rocketAddsLessSpeedToGlider(GameTestHelper helper) {
		Vec3 start = new Vec3(0.3, 0.0, 0.0);
		double elytraSpeed = rocketSpeed(helper, flier(helper, Items.ELYTRA, -90.0F, 0.0F, start), 8);
		double gliderSpeed = rocketSpeed(helper, flier(helper, ModItems.GLIDER, -90.0F, 0.0F, start), 8);
		helper.assertTrue(elytraSpeed > 1.4, "elytra speed after a rocket: %.3f".formatted(elytraSpeed));
		helper.assertTrue(gliderSpeed < elytraSpeed * 0.75 && gliderSpeed > start.x,
				"speed after 8 rocket ticks: elytra %.3f, glider %.3f".formatted(elytraSpeed, gliderSpeed));
		helper.succeed();
	}

	/** A rocket fired mid-dive, already faster than the Glider's rocket speed, doesn't slow it down (it used to drag it to ~1.0). */
	@GameTest
	public void rocketNeverSlowsAFastGlider(GameTestHelper helper) {
		Vec3 start = new Vec3(2.0, 0.0, 0.0);
		double gliderSpeed = rocketSpeed(helper, flier(helper, ModItems.GLIDER, -90.0F, 0.0F, start), 8);
		helper.assertTrue(gliderSpeed >= start.x, "a rocket slowed a 2.0 blocks/tick Glider to %.3f".formatted(gliderSpeed));
		helper.succeed();
	}

	private static double rocketSpeed(GameTestHelper helper, ServerPlayer player, int ticks) {
		FireworkRocketEntity rocket = new FireworkRocketEntity(helper.getLevel(), new ItemStack(Items.FIREWORK_ROCKET), player);
		helper.getLevel().addFreshEntity(rocket);
		for (int i = 0; i < ticks; i++) {
			rocket.tick();
		}
		rocket.discard();
		return player.getDeltaMovement().length();
	}
}
