// dte:C8
package io.github.bubba8587.bubbas_world_tweaks.test;

import io.github.bubba8587.bubbas_world_tweaks.BubbasWorldTweaks;
import io.github.bubba8587.bubbas_world_tweaks.parasitic.Parasitic;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/** Parasitic (specs/world-tweaks.md "Parasitic"). */
public class ParasiticGameTest {
	static Holder<Enchantment> parasitic(GameTestHelper helper) {
		return helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
				.getOrThrow(ResourceKey.create(Registries.ENCHANTMENT, BubbasWorldTweaks.id("parasitic")));
	}

	/** A survival player that takes damage, holding {@code stack}. */
	static ServerPlayer holder(GameTestHelper helper, ItemStack stack) {
		@SuppressWarnings("removal")
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		Vec3 at = helper.absoluteVec(new Vec3(1.5, 1.0, 1.5));
		player.snapTo(at.x, at.y, at.z, 0.0F, 0.0F);
		player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		return player;
	}

	/** A bite takes up to the level's health cap and mends at the level's ratio (III: up to 2 health, 55 durability). */
	public static void biteTradesHealthForDurability(GameTestHelper helper) {
		ItemStack sword = new ItemStack(Items.IRON_SWORD);
		sword.enchant(parasitic(helper), 3);
		sword.setDamageValue(200);
		ServerPlayer player = holder(helper, sword);
		float before = player.getHealth();
		helper.assertTrue(Parasitic.bite(helper.getLevel(), player, player.getRandom()), "no bite");
		float lost = before - player.getHealth();
		int mended = 200 - player.getMainHandItem().getDamageValue();
		helper.assertTrue(lost >= 0.5F - 0.01F && lost <= 2.0F + 0.01F, "health taken %.2f (III: 0.5 to 2)".formatted(lost));
		helper.assertTrue(Math.abs(mended / lost - 27.5F) < 1.5F, "mended %d for %.2f health (III: 27.5 a point)".formatted(mended, lost));
		helper.succeed();
	}

	/** Parasitic and Mending don't go on the same item. */
	@GameTest
	public void exclusiveWithMending(GameTestHelper helper) {
		Holder<Enchantment> mending = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.MENDING);
		helper.assertTrue(!Enchantment.areCompatible(parasitic(helper), mending), "Parasitic and Mending are compatible");
		helper.succeed();
	}

	/** An undamaged item doesn't bite. */
	@GameTest
	public void undamagedItemDoesNotBite(GameTestHelper helper) {
		ItemStack sword = new ItemStack(Items.IRON_SWORD);
		sword.enchant(parasitic(helper), 1);
		ServerPlayer player = holder(helper, sword);
		float before = player.getHealth();
		helper.assertTrue(!Parasitic.bite(helper.getLevel(), player, player.getRandom()) && player.getHealth() == before, "an undamaged item bit");
		helper.succeed();
	}
}
