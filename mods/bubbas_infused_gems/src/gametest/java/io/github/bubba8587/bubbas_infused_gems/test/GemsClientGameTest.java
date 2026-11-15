// dte:C4
package io.github.bubba8587.bubbas_infused_gems.test;

import java.util.List;
import java.util.function.Function;

import io.github.bubba8587.bubbas_infused_gems.gem.Gem;
import io.github.bubba8587.bubbas_infused_gems.gem.GemForm;
import io.github.bubba8587.bubbas_infused_gems.gem.GemText;
import io.github.bubba8587.bubbas_infused_gems.test.mixin.ContainerScreenAccessor;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/**
 * Looks at the gems (screenshots in build/run/clientGameTest/screenshots/): several colors in the inventory with their
 * tooltips (inventory), a big view of finished and Infused Gems in many colors (colors), the enchanting table with an Infused
 * Gem and its hint (enchanting), socketing at the smithing table (smithing), and a player in gemmed armor with /gems
 * (worn), a pig hit by a Glowing + Poison sword and /gems for Turtle Master gear (hits). Run a part with -PclientTests=inventory,... (ClientTests).
 */
@SuppressWarnings("UnstableApiUsage")
public class GemsClientGameTest implements FabricClientGameTest {
	private static final List<Gem> COLORS = List.of(
			GemKit.gem("speed", 1.0, "strength", 0.5, null),
			GemKit.gem("strength", 0.92, null, 0, "hunger"),
			GemKit.gem("poison", 0.88, "slowness", 0.5, "resistance"),
			GemKit.gem("fire_resistance", 0.75, "night_vision", 0.75, null),
			GemKit.gem("night_vision", 0.6),
			GemKit.gem("regeneration", 0.8, "absorption", 0.5, null),
			GemKit.gem("instant_health", 0.5, null, 0, "weakness"),
			GemKit.gem("resistance", 1.0, "jump_boost", 0.5, null),
			GemKit.gem("wither", 0.7, "poison", 0.5, "speed"));

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!List.of("inventory", "colors", "enchanting", "smithing", "worn", "hits").stream().anyMatch(ClientTests::run)) {
			return;
		}
		try (TestSingleplayerContext sp = context.worldBuilder()
				.adjustSettings(s -> s.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL))
				.create()) {
			sp.getConnection().waitForChunksRender();
			TestServerContext server = sp.getServer();
			server.runCommand("gamerule send_command_feedback false");
			server.runCommand("gamerule spawn_mobs false");
			server.runCommand("gamerule advance_time false");
			server.runCommand("time set noon");
			server.runCommand("difficulty peaceful");
			if (ClientTests.run("inventory")) {
				inventory(context, server);
			}
			if (ClientTests.run("colors")) {
				colors(context, server);
			}
			if (ClientTests.run("enchanting")) {
				enchanting(context, server);
			}
			if (ClientTests.run("smithing")) {
				smithing(context, server);
			}
			if (ClientTests.run("worn")) {
				worn(context, server);
			}
			if (ClientTests.run("hits")) {
				hits(context, server);
			}
		}
	}

	private static ServerPlayer player(MinecraftServer s) {
		return s.getPlayerList().getPlayers().getFirst();
	}

	private static void items(TestServerContext server, Function<Integer, ItemStack> slot) {
		server.runOnServer(s -> {
			ServerPlayer p = player(s);
			p.getInventory().clearContent();
			for (int i = 0; i < 36; i++) {
				p.getInventory().setItem(i, slot.apply(i));
			}
		});
	}

	/** Moves the mouse over a menu slot of the open container screen. */
	private static void hover(ClientGameTestContext context, int slotIndex) {
		double[] at = context.computeOnClient(mc -> {
			AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) mc.gui.screen();
			Slot slot = screen.getMenu().getSlot(slotIndex);
			ContainerScreenAccessor a = (ContainerScreenAccessor) screen;
			double scale = mc.getWindow().getGuiScale();
			return new double[] {(a.bubbas$left() + slot.x + 8) * scale, (a.bubbas$top() + slot.y + 8) * scale};
		});
		context.getInput().setCursorPos(at[0], at[1]);
		context.waitTicks(3);
	}

	private static void hoverAt(ClientGameTestContext context, int guiX, int guiY) {
		double[] at = context.computeOnClient(mc -> {
			ContainerScreenAccessor a = (ContainerScreenAccessor) mc.gui.screen();
			double scale = mc.getWindow().getGuiScale();
			return new double[] {(a.bubbas$left() + guiX) * scale, (a.bubbas$top() + guiY) * scale};
		});
		context.getInput().setCursorPos(at[0], at[1]);
		context.waitTicks(3);
	}

	private static void inventory(ClientGameTestContext context, TestServerContext server) {
		items(server, i -> {
			if (i < COLORS.size()) {
				return GemKit.item(COLORS.get(i)); // hotbar: finished gems
			}
			return switch (i) {
				case 9 -> GemKit.infused("speed", "strong", GemForm.LINGERING, 6);
				case 10 -> GemKit.infused("poison", "normal", GemForm.SPLASH, 3);
				case 11 -> GemKit.infused("fire_resistance", "long", GemForm.NORMAL, 1);
				case 12 -> GemKit.infused("absorption", "strong", GemForm.NORMAL, 4);
				case 13 -> GemKit.infused("wither", "normal", GemForm.LINGERING, 2);
				case 14 -> GemKit.infused("instant_health", "strong", GemForm.SPLASH, 5);
				case 18 -> GemKit.socketed(Items.NETHERITE_CHESTPLATE, COLORS.get(0), COLORS.get(1), COLORS.get(7));
				case 19 -> GemKit.socketed(Items.DIAMOND_SWORD, COLORS.get(2), COLORS.get(8));
				case 20 -> GemKit.socketed(Items.BOW, GemKit.gem("slowness", 0.9));
				default -> ItemStack.EMPTY;
			};
		});
		context.waitTicks(5);
		context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
		context.waitTicks(5);
		// InventoryMenu: hotbar slots are 36-44, main inventory 9-35.
		hover(context, 36 + 2);
		context.takeScreenshot("gems-inventory-poison-gem-tooltip");
		hover(context, 36);
		context.takeScreenshot("gems-inventory-speed-gem-tooltip");
		hover(context, 9);
		context.takeScreenshot("gems-inventory-infused-gem-tooltip");
		hover(context, 18);
		context.takeScreenshot("gems-inventory-socketed-chestplate-tooltip");
		hover(context, 19);
		context.takeScreenshot("gems-inventory-socketed-sword-tooltip");
		context.runOnClient(mc -> mc.player.closeContainer());
	}

	/** A big look at the colors: finished gems in the hotbar, the matching Infused Gems in the first inventory row. */
	private static void colors(ClientGameTestContext context, TestServerContext server) {
		items(server, i -> {
			int col = i % 9;
			if (col >= COLORS.size() || i >= 18) {
				return ItemStack.EMPTY;
			}
			Gem gem = COLORS.get(col);
			return i < 9 ? GemKit.item(gem)
					: GemKit.infused(GemText.key(gem.effect()),"normal", GemForm.LINGERING, 6);
		});
		context.getInput().resizeWindow(1280, 1024);
		context.waitTicks(5);
		context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
		context.waitTicks(5);
		context.getInput().setCursorPos(0, 0);
		context.waitTicks(2);
		context.takeScreenshot("gems-colors-finished-hotbar-infused-above");
		context.runOnClient(mc -> mc.player.closeContainer());
		context.getInput().resizeWindow(854, 480);
		context.waitTicks(5);
	}

	private static BlockPos stationPos(TestServerContext server) {
		return server.computeOnServer(s -> player(s).blockPosition().offset(3, 0, 0));
	}

	private static void enchanting(ClientGameTestContext context, TestServerContext server) {
		BlockPos table = stationPos(server);
		server.runCommand("setblock %d %d %d minecraft:enchanting_table".formatted(table.getX(), table.getY(), table.getZ()));
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				if (Math.abs(dx) == 2 || Math.abs(dz) == 2) {
					server.runCommand("setblock %d %d %d minecraft:bookshelf".formatted(table.getX() + dx, table.getY(), table.getZ() + dz));
					server.runCommand("setblock %d %d %d minecraft:bookshelf".formatted(table.getX() + dx, table.getY() + 1, table.getZ() + dz));
				}
			}
		}
		items(server, i -> i == 0 ? new ItemStack(Items.LAPIS_LAZULI, 64) : ItemStack.EMPTY);
		server.runOnServer(s -> {
			ServerPlayer p = player(s);
			p.setExperienceLevels(40);
			p.openMenu(new SimpleMenuProvider((id, inv, pl) -> new EnchantmentMenu(id, inv, ContainerLevelAccess.create(p.level(), table)),
					Component.translatable("container.enchant")));
		});
		context.waitTicks(5);
		server.runOnServer(s -> {
			AbstractContainerMenu menu = player(s).containerMenu;
			menu.getSlot(0).set(GemKit.infused("speed", "strong", GemForm.LINGERING, 6));
			menu.getSlot(1).set(new ItemStack(Items.LAPIS_LAZULI, 3));
		});
		context.waitTicks(10);
		hoverAt(context, 60 + 54, 14 + 19 * 2 + 8);
		context.takeScreenshot("gems-enchanting-lingering-gem-tier3-hint");
		server.runOnServer(s -> player(s).containerMenu.getSlot(0).set(GemKit.infused("poison", "normal", GemForm.NORMAL, 2)));
		context.waitTicks(5);
		hoverAt(context, 60 + 54, 14 + 8);
		context.takeScreenshot("gems-enchanting-normal-gem-only-tier1");
		context.runOnClient(mc -> mc.player.closeContainer());
		context.waitTicks(3);
	}

	private static void smithing(ClientGameTestContext context, TestServerContext server) {
		BlockPos at = stationPos(server).offset(0, 0, 4);
		server.runCommand("setblock %d %d %d minecraft:smithing_table".formatted(at.getX(), at.getY(), at.getZ()));
		items(server, i -> i == 0 ? GemKit.item(COLORS.get(5)) : ItemStack.EMPTY);
		server.runOnServer(s -> {
			ServerPlayer p = player(s);
			p.openMenu(new SimpleMenuProvider((id, inv, pl) -> new SmithingMenu(id, inv, ContainerLevelAccess.create(p.level(), at)),
					Component.translatable("container.upgrade")));
		});
		context.waitTicks(5);
		server.runOnServer(s -> {
			AbstractContainerMenu menu = player(s).containerMenu;
			menu.getSlot(SmithingMenu.BASE_SLOT).set(GemKit.socketed(Items.DIAMOND_HELMET, COLORS.get(0)));
			menu.getSlot(SmithingMenu.ADDITIONAL_SLOT).set(GemKit.item(COLORS.get(5)));
		});
		context.waitTicks(10);
		hover(context, 3);
		context.takeScreenshot("gems-smithing-socket-result-tooltip");
		context.runOnClient(mc -> mc.player.closeContainer());
		context.waitTicks(3);
	}

	private static void worn(ClientGameTestContext context, TestServerContext server) {
		Gem speed = GemKit.gem("speed", 1.0, "strength", 0.5, "hunger");
		server.runOnServer(s -> {
			ServerPlayer p = player(s);
			p.getInventory().clearContent();
			p.setItemSlot(EquipmentSlot.HEAD, GemKit.socketed(Items.NETHERITE_HELMET, speed, speed, GemKit.gem("night_vision", 0.9)));
			p.setItemSlot(EquipmentSlot.CHEST, GemKit.socketed(Items.NETHERITE_CHESTPLATE, speed, speed, speed));
			p.setItemSlot(EquipmentSlot.LEGS, GemKit.socketed(Items.NETHERITE_LEGGINGS, speed, GemKit.gem("resistance", 0.9)));
			p.setItemSlot(EquipmentSlot.FEET, GemKit.socketed(Items.DIAMOND_BOOTS, GemKit.gem("jump_boost", 0.8)));
			p.setItemSlot(EquipmentSlot.MAINHAND, GemKit.socketed(Items.NETHERITE_SWORD, COLORS.get(2), GemKit.gem("instant_damage", 1.0)));
		});
		context.waitTicks(10);
		context.runOnClient(mc -> mc.player.connection.sendCommand("gems"));
		context.waitTicks(10);
		context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
		context.waitTicks(5);
		context.takeScreenshot("gems-worn-netherite-third-person-with-gems-command");
		context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
		context.waitTicks(5);
		hover(context, 6);
		context.takeScreenshot("gems-worn-inventory-chestplate-tooltip");
		context.runOnClient(mc -> mc.player.closeContainer());
		context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
		// Night vision at midnight: without the gem, then with 12 perfect Night Vision gems in full netherite (95%).
		server.runCommand("time set midnight");
		BlockPos wall = stationPos(server).offset(0, 0, -3);
		server.runCommand("fill %d %d %d %d %d %d minecraft:red_wool".formatted(wall.getX(), wall.getY(), wall.getZ() - 2, wall.getX(), wall.getY() + 2, wall.getZ() + 2));
		context.runOnClient(mc -> mc.gui.hud.getChat().clearMessages(false));
		context.getInput().lookAt(wall.above());
		server.runOnServer(s -> {
			ServerPlayer p = player(s);
			for (EquipmentSlot slot : io.github.bubba8587.bubbas_infused_gems.effect.GemTotals.ARMOR) {
				p.setItemSlot(slot, ItemStack.EMPTY);
			}
		});
		context.waitTicks(10);
		context.takeScreenshot("gems-worn-midnight-no-night-vision-gem");
		Gem nv = GemKit.gem("night_vision", 1.0);
		server.runOnServer(s -> {
			ServerPlayer p = player(s);
			p.setItemSlot(EquipmentSlot.HEAD, GemKit.socketed(Items.NETHERITE_HELMET, nv, nv, nv));
			p.setItemSlot(EquipmentSlot.CHEST, GemKit.socketed(Items.NETHERITE_CHESTPLATE, nv, nv, nv));
			p.setItemSlot(EquipmentSlot.LEGS, GemKit.socketed(Items.NETHERITE_LEGGINGS, nv, nv, nv));
			p.setItemSlot(EquipmentSlot.FEET, GemKit.socketed(Items.NETHERITE_BOOTS, nv, nv, nv));
		});
		// The client's own copy of the totals (it drives the lightmap): 12 perfect gems in full netherite = the cap (0.90).
		context.waitFor(mc -> Math.abs(((io.github.bubba8587.bubbas_infused_gems.effect.GemEntity) mc.player).bubbas$wornGems()
				.value("night_vision") - 0.90) < 1e-4);
		context.waitTicks(5);
		context.takeScreenshot("gems-worn-midnight-12-night-vision-gems");
	}

	/** One real sword swing: the target pig glows (outline) and is poisoned; then /gems lists Turtle Master's two parts. */
	private static void hits(ClientGameTestContext context, TestServerContext server) {
		server.runCommand("time set noon");
		context.runOnClient(mc -> mc.gui.hud.getChat().clearMessages(false));
		BlockPos at = stationPos(server).offset(1, 0, 0);
		server.runOnServer(s -> {
			ServerPlayer p = player(s);
			for (EquipmentSlot slot : io.github.bubba8587.bubbas_infused_gems.effect.GemTotals.ARMOR) {
				p.setItemSlot(slot, ItemStack.EMPTY);
			}
			p.setItemSlot(EquipmentSlot.MAINHAND, GemKit.socketed(Items.NETHERITE_SWORD, GemKit.gem("glowing", 1.0), GemKit.gem("poison", 1.0)));
			net.minecraft.world.entity.animal.pig.Pig z = net.minecraft.world.entity.EntityTypes.PIG.create(p.level(),
					net.minecraft.world.entity.EntitySpawnReason.COMMAND);
			z.setPos(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
			z.setNoAi(true);
			z.setPersistenceRequired();
			z.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(200.0);
			z.setHealth(200.0F);
			p.level().addFreshEntity(z);
		});
		context.waitTicks(5);
		context.getInput().lookAt(at.above());
		server.runOnServer(s -> {
			ServerPlayer p = player(s);
			p.level().getEntitiesOfClass(net.minecraft.world.entity.animal.pig.Pig.class, p.getBoundingBox().inflate(8)).forEach(p::attack);
		});
		context.waitTicks(25);
		context.takeScreenshot("gems-hit-pig-glowing-and-poisoned");
		Gem tm = GemKit.gem("turtle_master", 1.0);
		server.runOnServer(s -> {
			ServerPlayer p = player(s);
			p.setItemSlot(EquipmentSlot.HEAD, GemKit.socketed(Items.NETHERITE_HELMET, tm, tm, tm));
			p.setItemSlot(EquipmentSlot.CHEST, GemKit.socketed(Items.NETHERITE_CHESTPLATE, tm, tm, tm));
			p.setItemSlot(EquipmentSlot.LEGS, GemKit.socketed(Items.NETHERITE_LEGGINGS, tm, tm, tm));
			p.setItemSlot(EquipmentSlot.FEET, GemKit.socketed(Items.NETHERITE_BOOTS, tm, tm, tm));
			p.setItemSlot(EquipmentSlot.MAINHAND, GemKit.socketed(Items.NETHERITE_SWORD, GemKit.gem("weakness", 1.0), GemKit.gem("instant_damage", 1.0)));
		});
		context.waitTicks(10);
		context.runOnClient(mc -> mc.gui.hud.getChat().clearMessages(false));
		context.runOnClient(mc -> mc.player.connection.sendCommand("gems"));
		context.waitTicks(10);
		context.takeScreenshot("gems-command-turtle-master-and-weapon");
	}
}
