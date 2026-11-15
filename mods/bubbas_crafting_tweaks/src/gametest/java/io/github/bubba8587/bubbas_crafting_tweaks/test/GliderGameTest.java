// dte:C3
package io.github.bubba8587.bubbas_crafting_tweaks.test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.github.bubba8587.bubbas_crafting_tweaks.BubbasCraftingTweaks;
import io.github.bubba8587.bubbas_crafting_tweaks.glider.GliderHandling;
import io.github.bubba8587.bubbas_crafting_tweaks.registry.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * The Glider (specs/crafting-tweaks-mod.md): its components, its recipe, and its flight against an elytra's from the
 * same start. Flight runs the game's own code: LivingEntity#updateFallFlyingMovement (called directly, it is a pure
 * step from the current velocity, look direction and gravity) and FireworkRocketEntity#tick with the rocket attached.
 */
public class GliderGameTest {
	private static final Method FALL_FLYING_STEP;

	static {
		try {
			FALL_FLYING_STEP = LivingEntity.class.getDeclaredMethod("updateFallFlyingMovement", Vec3.class);
			FALL_FLYING_STEP.setAccessible(true);
		} catch (NoSuchMethodException e) {
			throw new IllegalStateException(e);
		}
	}

	@GameTest
	public void gliderHasGliderAndEquippableAndNoDurability(GameTestHelper helper) {
		ItemStack glider = new ItemStack(ModItems.GLIDER);
		helper.assertTrue(glider.has(DataComponents.GLIDER), "no glider component");
		Equippable equippable = glider.get(DataComponents.EQUIPPABLE);
		helper.assertTrue(equippable != null && equippable.slot() == EquipmentSlot.CHEST, "equippable: " + equippable);
		helper.assertValueEqual(equippable.assetId().orElseThrow(), ModItems.GLIDER_ASSET, "equipment asset");
		helper.assertTrue(!glider.has(DataComponents.MAX_DAMAGE) && !glider.has(DataComponents.DAMAGE) && !glider.isDamageableItem(),
				"glider has durability: " + glider.getComponents());
		helper.assertTrue(LivingEntity.canGlideUsing(glider, EquipmentSlot.CHEST), "cannot glide using the glider in the chest slot");
		helper.assertValueEqual(glider.getMaxStackSize(), 1, "stack size");
		helper.succeed();
	}

	// Core test: registered by CraftingTweaksCoreGameTest (test-lite, specs/quality.md).
	public void gliderRecipeCraftsOneGlider(GameTestHelper helper) {
		var key = ResourceKey.create(Registries.RECIPE, BubbasCraftingTweaks.id("glider"));
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

	/** Velocity after {@code ticks} steps of the game's fall-flying movement. */
	private static Vec3 glide(ServerPlayer player, Vec3 velocity, int ticks) throws ReflectiveOperationException {
		for (int i = 0; i < ticks; i++) {
			velocity = (Vec3) FALL_FLYING_STEP.invoke(player, velocity);
		}
		return velocity;
	}

	/** Angle (degrees) between the horizontal velocity and the horizontal look direction. */
	private static double headingError(ServerPlayer player, Vec3 v) {
		Vec3 look = player.getLookAngle();
		double a = Math.atan2(v.z, v.x) - Math.atan2(look.z, look.x);
		return Math.abs(Math.toDegrees(Math.atan2(Math.sin(a), Math.cos(a))));
	}

	// Core test: registered by CraftingTweaksCoreGameTest (test-lite, specs/quality.md).
	public void gliderTurnsSlowerThanElytra(GameTestHelper helper) throws ReflectiveOperationException {
		// Flying east at 1 block/tick, looking north (yaw 180) and level: both turn toward north over 20 ticks.
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
		// Vanilla blend 10 %/tick: the leftover sideways speed is about 0.9^20 * 0.99^20 = 0.10 of the start; glider 0.95^20 * 0.99^20 = 0.29.
		helper.assertTrue(Math.abs(e.x) < 0.15 && Math.abs(g.x) > 0.22, "sideways speed left: elytra %.3f, glider %.3f".formatted(e.x, g.x));
		// Lift and sink stay vanilla: the vertical speed is the same.
		helper.assertTrue(Math.abs(e.y - g.y) < 1e-9, "vertical speed differs: elytra %.4f, glider %.4f".formatted(e.y, g.y));
		helper.succeed();
	}

	@GameTest
	public void rocketAddsLessSpeedToGlider(GameTestHelper helper) {
		// Flying east slowly, looking east: a rocket attached for 8 ticks.
		Vec3 start = new Vec3(0.3, 0.0, 0.0);
		double elytraSpeed = rocketSpeed(helper, flier(helper, Items.ELYTRA, -90.0F, 0.0F, start), 8);
		double gliderSpeed = rocketSpeed(helper, flier(helper, ModItems.GLIDER, -90.0F, 0.0F, start), 8);
		helper.assertTrue(elytraSpeed > 1.4, "elytra speed after a rocket: %.3f".formatted(elytraSpeed));
		helper.assertTrue(gliderSpeed < elytraSpeed * 0.75 && gliderSpeed > start.x,
				"speed after 8 rocket ticks: elytra %.3f, glider %.3f".formatted(elytraSpeed, gliderSpeed));
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
