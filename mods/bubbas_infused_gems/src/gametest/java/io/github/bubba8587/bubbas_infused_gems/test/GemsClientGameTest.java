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

/** Looks at the gems: several colors in the inventory with their tooltips. */
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
		if (!List.of("inventory", "colors", "enchanting", "smithing", "worn", "hits", "mobs", "screen", "levitation", "beyond", "jump", "weaving", "invisible", "looks", "dials").stream().anyMatch(ClientTests::run)) {
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
			if (ClientTests.run("mobs")) {
				mobs(context, server);
			}
			if (ClientTests.run("screen")) {
				screen(context, server);
			}
			if (ClientTests.run("levitation")) {
				levitation(context, server);
			}
			if (ClientTests.run("beyond")) {
				beyond(context, server);
			}
			if (ClientTests.run("jump")) {
				jump(context, server);
			}
			if (ClientTests.run("weaving")) {
				weaving(context, server);
			}
			if (ClientTests.run("invisible")) {
				invisible(context, server);
			}
			if (ClientTests.run("looks")) {
				looks(context, server);
			}
			if (ClientTests.run("dials")) {
				dials(context, server);
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
				return GemKit.item(COLORS.get(i));
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
		// InventoryMenu: hotbar slots are 36-44, main inventory 9-35
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

	/** A big look at the colors. */
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
		// The preview names an extra roll's kind and size only
		for (String want : List.of("bonus", "drawback")) {
			ItemStack pickGem = server.computeOnServer(s -> {
				int seed = player(s).getEnchantmentSeed();
				for (String key : io.github.bubba8587.bubbas_infused_gems.balance.Balance.get().gemEffects) {
					for (int inf = 1; inf <= 6; inf++) {
						ItemStack st = GemKit.infused(key, io.github.bubba8587.bubbas_infused_gems.registry.ModCreativeTabs.top(key), GemForm.LINGERING, inf);
						var o = io.github.bubba8587.bubbas_infused_gems.station.GemEnchanting.outcome(seed, 3,
								st.get(io.github.bubba8587.bubbas_infused_gems.registry.ModComponents.INFUSED_GEM));
						if (want.equals("bonus") ? o.other() != null : o.drawback() != null) {
							return st;
						}
					}
				}
				return GemKit.infused("speed", "strong", GemForm.LINGERING, 6);
			});
			server.runOnServer(s -> player(s).containerMenu.getSlot(0).set(pickGem));
			context.waitTicks(5);
			hoverAt(context, 60 + 54, 14 + 19 * 2 + 8);
			context.takeScreenshot("gems-enchanting-tier3-hint-with-" + want);
		}
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
		// Night vision at midnight
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
		// The client's own copy of the totals
		double nvCap = io.github.bubba8587.bubbas_infused_gems.balance.Balance.get().effect("night_vision").wornCap();
		context.waitFor(mc -> Math.abs(((io.github.bubba8587.bubbas_infused_gems.effect.GemEntity) mc.player).bubbas$wornGems()
				.value("night_vision") - nvCap) < 1e-4);
		context.waitTicks(5);
		context.takeScreenshot("gems-worn-midnight-12-night-vision-gems");
	}

	/** One real sword swing. */
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

	/** A zombie in gemmed iron armor. */
	private static void mobs(ClientGameTestContext context, TestServerContext server) {
		server.runCommand("difficulty easy");
		server.runCommand("time set noon");
		BlockPos at = stationPos(server).offset(-2, 0, 3);
		server.runOnServer(s -> {
			ServerPlayer p = player(s);
			for (EquipmentSlot slot : io.github.bubba8587.bubbas_infused_gems.effect.GemTotals.ARMOR) {
				p.setItemSlot(slot, ItemStack.EMPTY);
			}
			p.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
			p.getInventory().clearContent();
			net.minecraft.world.entity.monster.zombie.Zombie z = net.minecraft.world.entity.EntityTypes.ZOMBIE.create(p.level(),
					net.minecraft.world.entity.EntitySpawnReason.COMMAND);
			z.setPos(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
			z.setNoAi(true);
			z.setPersistenceRequired();
			z.setYRot(180.0F);
			z.setYHeadRot(180.0F);
			z.setYBodyRot(180.0F);
			z.setItemSlot(EquipmentSlot.HEAD, GemKit.socketed(Items.IRON_HELMET, GemKit.gem("speed", 0.4)));
			z.setItemSlot(EquipmentSlot.CHEST, GemKit.socketed(Items.IRON_CHESTPLATE, GemKit.gem("glowing", 0.4)));
			z.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
			z.setItemSlot(EquipmentSlot.FEET, GemKit.socketed(Items.IRON_BOOTS, GemKit.gem("regeneration", 0.3)));
			p.level().addFreshEntity(z);
		});
		context.waitTicks(40);
		context.getInput().lookAt(at.above());
		context.waitTicks(5);
		context.takeScreenshot("gems-mobs-zombie-gemmed-armor-glowing");
		// Closer, from the side
		server.runOnServer(s -> {
			ServerPlayer p = player(s);
			p.teleportTo(at.getX() + 3.5, at.getY(), at.getZ() + 1.5);
		});
		context.waitTicks(10);
		context.getInput().lookAt(at.above());
		context.waitTicks(5);
		context.takeScreenshot("gems-mobs-zombie-gemmed-armor-close");
		server.runOnServer(s -> {
			ServerPlayer p = player(s);
			var table = s.reloadableRegistries().getLootTable(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,
					io.github.bubba8587.bubbas_infused_gems.BubbasInfusedGems.id("gems/hard")));
			var params = new net.minecraft.world.level.storage.loot.LootParams.Builder(p.level())
					.create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.EMPTY);
			int slot = 0;
			for (long seed = 1; slot < 18 && seed < 400; seed++) {
				for (ItemStack st : table.getRandomItems(params, seed)) {
					if (slot < 18 && (slot < 9 || st.has(io.github.bubba8587.bubbas_infused_gems.registry.ModComponents.SOCKETED_GEMS))) {
						p.getInventory().setItem(slot++, st);
					}
				}
			}
		});
		context.waitTicks(5);
		context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
		context.waitTicks(5);
		hover(context, 9);
		context.takeScreenshot("gems-mobs-hard-loot-gear-tooltip");
		context.runOnClient(mc -> mc.player.closeContainer());
		server.runCommand("difficulty peaceful");
	}

	/** Darkness and Blindness gems worn: a steady, partial dim or fog by strength, same scene. */
	private static void screen(ClientGameTestContext context, TestServerContext server) {
		server.runCommand("time set noon");
		context.runOnClient(mc -> mc.gui.hud.getChat().clearMessages(false));
		BlockPos wall = stationPos(server).offset(4, 0, 0);
		server.runCommand("fill %d %d %d %d %d %d minecraft:red_wool".formatted(wall.getX(), wall.getY(), wall.getZ() - 2, wall.getX(), wall.getY() + 2, wall.getZ() + 2));
		context.getInput().lookAt(wall.above());
		for (String key : List.of("darkness", "blindness")) {
			for (String build : List.of("weak", "mid", "best")) {
				if (key.equals("blindness") && build.equals("mid")) {
					continue;
				}
				server.runOnServer(s -> {
					ServerPlayer p = player(s);
					p.getInventory().clearContent();
					for (EquipmentSlot slot : io.github.bubba8587.bubbas_infused_gems.effect.GemTotals.ARMOR) {
						p.setItemSlot(slot, ItemStack.EMPTY);
					}
					switch (build) {
						case "weak" -> p.setItemSlot(EquipmentSlot.CHEST, GemKit.socketed(Items.NETHERITE_CHESTPLATE, GemKit.gem(key, 0.5)));
						case "mid" -> {
							Gem g = GemKit.gem(key, 0.75);
							p.setItemSlot(EquipmentSlot.HEAD, GemKit.socketed(Items.DIAMOND_HELMET, g, g));
							p.setItemSlot(EquipmentSlot.CHEST, GemKit.socketed(Items.DIAMOND_CHESTPLATE, g, g));
							p.setItemSlot(EquipmentSlot.LEGS, GemKit.socketed(Items.DIAMOND_LEGGINGS, g, g));
							p.setItemSlot(EquipmentSlot.FEET, GemKit.socketed(Items.DIAMOND_BOOTS, g, g));
						}
						default -> {
							Gem g = GemKit.gem(key, 1.0);
							p.setItemSlot(EquipmentSlot.HEAD, GemKit.socketed(Items.NETHERITE_HELMET, g, g, g));
							p.setItemSlot(EquipmentSlot.CHEST, GemKit.socketed(Items.NETHERITE_CHESTPLATE, g, g, g));
							p.setItemSlot(EquipmentSlot.LEGS, GemKit.socketed(Items.NETHERITE_LEGGINGS, g, g, g));
							p.setItemSlot(EquipmentSlot.FEET, GemKit.socketed(Items.NETHERITE_BOOTS, g, g, g));
						}
					}
				});
				context.waitTicks(30);
				double s = context.computeOnClient(mc -> ((io.github.bubba8587.bubbas_infused_gems.effect.GemEntity) mc.player).bubbas$wornGems().value(key));
				context.takeScreenshot("gems-screen-" + key + "-" + build + "-" + Math.round(s * 100));
			}
		}
		server.runOnServer(s -> {
			for (EquipmentSlot slot : io.github.bubba8587.bubbas_infused_gems.effect.GemTotals.ARMOR) {
				player(s).setItemSlot(slot, ItemStack.EMPTY);
			}
		});
	}

	/** A Levitation set lifts the player continuously. */
	private static void levitation(ClientGameTestContext context, TestServerContext server) {
		for (String mode : List.of("survival", "creative")) {
			for (String build : List.of("best", "weak")) {
				server.runCommand("gamemode " + mode + " @a");
				server.runOnServer(s -> {
					ServerPlayer p = player(s);
					for (EquipmentSlot slot : io.github.bubba8587.bubbas_infused_gems.effect.GemTotals.ARMOR) {
						p.setItemSlot(slot, ItemStack.EMPTY);
					}
					p.getAbilities().flying = false;
					p.onUpdateAbilities();
				});
				context.waitTicks(40);
				server.runOnServer(s -> {
					ServerPlayer p = player(s);
					if (build.equals("weak")) {
						p.setItemSlot(EquipmentSlot.CHEST, GemKit.socketed(Items.NETHERITE_CHESTPLATE, GemKit.gem("levitation", 0.5)));
					} else {
						Gem g = GemKit.gem("levitation", 1.0);
						p.setItemSlot(EquipmentSlot.HEAD, GemKit.socketed(Items.NETHERITE_HELMET, g, g, g));
						p.setItemSlot(EquipmentSlot.CHEST, GemKit.socketed(Items.NETHERITE_CHESTPLATE, g, g, g));
						p.setItemSlot(EquipmentSlot.LEGS, GemKit.socketed(Items.NETHERITE_LEGGINGS, g, g, g));
						p.setItemSlot(EquipmentSlot.FEET, GemKit.socketed(Items.NETHERITE_BOOTS, g, g, g));
					}
				});
				context.waitTicks(5);
				double y0 = context.computeOnClient(mc -> mc.player.getY());
				context.waitTicks(80);
				double y1 = context.computeOnClient(mc -> mc.player.getY());
				double serverY = server.computeOnServer(s -> player(s).getY());
				boolean flying = context.computeOnClient(mc -> mc.player.getAbilities().flying);
				System.out.println("[levitation test] " + mode + " " + build + ": rose " + (y1 - y0) + " blocks in 4 s (server sees " + serverY + ", client " + y1 + ", flying " + flying + ")");
				if (y1 - y0 < (build.equals("best") ? 1.6 : 0.5)) {
					throw new AssertionError("Levitation " + build + " in " + mode + " rose only " + (y1 - y0) + " blocks in 4 s");
				}
				if (build.equals("best") && mode.equals("survival")) {
					context.takeScreenshot("gems-levitation-best-survival-after-4s");
				}
			}
		}
		server.runOnServer(s -> {
			for (EquipmentSlot slot : io.github.bubba8587.bubbas_infused_gems.effect.GemTotals.ARMOR) {
				player(s).setItemSlot(slot, ItemStack.EMPTY);
			}
		});
		server.runCommand("gamemode survival @a");
	}

	/** Gems beyond potions: a Weaving sword webs a pig in place. */
	private static void beyond(ClientGameTestContext context, TestServerContext server) {
		server.runCommand("time set noon");
		context.runOnClient(mc -> mc.gui.hud.getChat().clearMessages(false));
		BlockPos at = stationPos(server).offset(2, 0, 0);
		server.runOnServer(s -> {
			ServerPlayer p = player(s);
			for (EquipmentSlot slot : io.github.bubba8587.bubbas_infused_gems.effect.GemTotals.ARMOR) {
				p.setItemSlot(slot, ItemStack.EMPTY);
			}
			p.setItemSlot(EquipmentSlot.MAINHAND, GemKit.socketed(Items.NETHERITE_SWORD, GemKit.gem("weaving", 1.0), GemKit.gem("weaving", 1.0)));
			for (int i = 0; i < 2; i++) {
				net.minecraft.world.entity.animal.pig.Pig pig = net.minecraft.world.entity.EntityTypes.PIG.create(p.level(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
				pig.setPos(at.getX() + 0.5, at.getY(), at.getZ() + 0.5 + i * 2 - 1);
				pig.setNoAi(true);
				pig.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(500.0);
				pig.setHealth(i == 0 ? 500.0F : 1.0F);
				p.level().addFreshEntity(pig);
			}
		});
		context.waitTicks(10);
		context.getInput().lookAt(at);
		server.runOnServer(s -> {
			ServerPlayer p = player(s);
			var pigs = p.level().getEntitiesOfClass(net.minecraft.world.entity.animal.pig.Pig.class, p.getBoundingBox().inflate(8));
			var webbed = pigs.stream().filter(x -> x.getHealth() > 100).findFirst().orElseThrow();
			for (int t = 0; t < 60 && !p.level().getBlockState(webbed.blockPosition()).is(net.minecraft.world.level.block.Blocks.COBWEB); t++) {
				webbed.damageCooldownTime = 0;
				io.github.bubba8587.bubbas_infused_gems.effect.GemHits.onWeaponHit(p.level(), p, p, webbed, p.getMainHandItem());
			}
			p.setItemSlot(EquipmentSlot.MAINHAND, GemKit.socketed(Items.NETHERITE_SWORD, GemKit.gem("oozing", 1.0), GemKit.gem("oozing", 1.0)));
			var doomed = pigs.stream().filter(x -> x.getHealth() < 100).findFirst().orElseThrow();
			p.attack(doomed);
		});
		context.waitTicks(60);
		context.takeScreenshot("gems-beyond-weaving-web-and-oozing-slimes");
	}

	/** A perfect Jump Boost set clears a 4-high pillar from a standing jump. */
	private static void jump(ClientGameTestContext context, TestServerContext server) {
		server.runCommand("gamemode survival @a");
		BlockPos feet = server.computeOnServer(s -> player(s).blockPosition());
		BlockPos pillar = feet.offset(1, 0, 0);
		server.runCommand("fill %d %d %d %d %d %d minecraft:stone".formatted(pillar.getX(), pillar.getY(), pillar.getZ() - 1, pillar.getX() + 15, pillar.getY() + 3, pillar.getZ() + 1));
		server.runOnServer(s -> {
			ServerPlayer p = player(s);
			p.teleportTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5);
			Gem g = GemKit.gem("jump_boost", 1.0);
			p.setItemSlot(EquipmentSlot.HEAD, GemKit.socketed(Items.NETHERITE_HELMET, g, g, g));
			p.setItemSlot(EquipmentSlot.CHEST, GemKit.socketed(Items.NETHERITE_CHESTPLATE, g, g, g));
			p.setItemSlot(EquipmentSlot.LEGS, GemKit.socketed(Items.NETHERITE_LEGGINGS, g, g, g));
			p.setItemSlot(EquipmentSlot.FEET, GemKit.socketed(Items.NETHERITE_BOOTS, g, g, g));
		});
		context.waitTicks(20);
		context.getInput().lookAt(-90.0F, 0.0F);
		context.waitFor(mc -> mc.player.onGround());
		context.waitTicks(3);
		double y0 = context.computeOnClient(mc -> mc.player.getY());
		context.getInput().holdKey(o -> o.keyUp);
		context.getInput().holdKeyFor(o -> o.keyJump, 2);
		double top = y0;
		for (int t = 0; t < 40; t++) {
			context.waitTicks(1);
			top = Math.max(top, context.computeOnClient(mc -> mc.player.getY()));
		}
		context.getInput().releaseKey(o -> o.keyUp);
		context.waitTicks(10);
		double y1 = context.computeOnClient(mc -> mc.player.getY());
		if (y1 < y0 + 3.99) {
			throw new AssertionError("a perfect Jump Boost set did not land on a 4-block pillar: " + y0 + " -> " + y1);
		}
		context.takeScreenshot("gems-jump-perfect-set-on-4-block-pillar");
		server.runOnServer(s -> {
			for (EquipmentSlot slot : io.github.bubba8587.bubbas_infused_gems.effect.GemTotals.ARMOR) {
				player(s).setItemSlot(slot, ItemStack.EMPTY);
			}
		});
	}

	/** Sprints a real client player along +x from start for 40 ticks. */
	private static double sprint(ClientGameTestContext context, TestServerContext server, BlockPos start) {
		server.runOnServer(s -> {
			ServerPlayer p = player(s);
			p.teleportTo(start.getX() + 0.5, start.getY(), start.getZ() + 0.5);
			p.getFoodData().setFoodLevel(20);
		});
		context.waitTicks(10);
		context.getInput().lookAt(-90.0F, 0.0F);
		context.waitFor(mc -> mc.player.onGround());
		context.getInput().holdKey(o -> o.keyUp);
		context.getInput().holdKey(o -> o.keySprint);
		context.waitTicks(20);
		double x0 = context.computeOnClient(mc -> mc.player.getX());
		context.waitTicks(20);
		double x1 = context.computeOnClient(mc -> mc.player.getX());
		boolean sprinting = context.computeOnClient(mc -> mc.player.isSprinting());
		context.getInput().releaseKey(o -> o.keySprint);
		context.getInput().releaseKey(o -> o.keyUp);
		if (!sprinting) {
			throw new AssertionError("the player stopped sprinting in the lane from " + start);
		}
		return (x1 - x0) / 20.0;
	}

	/** a perfect Weaving set moves through cobwebs completely unimpeded. */
	private static void weaving(ClientGameTestContext context, TestServerContext server) {
		server.runCommand("gamemode survival @a");
		BlockPos base = server.computeOnServer(s -> player(s).blockPosition()).offset(0, 20, 0);
		int x = base.getX(), y = base.getY(), z = base.getZ();
		server.runCommand("fill %d %d %d %d %d %d minecraft:stone".formatted(x - 2, y - 1, z - 2, x + 40, y - 1, z + 4));
		server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 2, y, z - 2, x + 40, y + 3, z + 4));
		server.runCommand("fill %d %d %d %d %d %d minecraft:cobweb".formatted(x + 1, y, z + 3, x + 40, y + 1, z + 3));
		server.runCommand("fill %d %d %d %d %d %d minecraft:cobweb".formatted(x - 2, y, z - 2, x - 2, y + 11, z - 2));
		server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 1, y, z - 1, x - 1, y + 11, z - 1));
		server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 2, y + 4, z - 2, x + 40, y + 11, z + 4));
		server.runCommand("fill %d %d %d %d %d %d minecraft:cobweb".formatted(x - 2, y, z - 2, x - 2, y + 11, z - 2));
		double open = sprint(context, server, new BlockPos(x, y, z));
		double plainWeb = sprint(context, server, new BlockPos(x, y, z + 3));
		server.runOnServer(s -> {
			ServerPlayer p = player(s);
			Gem g = GemKit.gem("weaving", 1.0);
			p.setItemSlot(EquipmentSlot.HEAD, GemKit.socketed(Items.NETHERITE_HELMET, g, g, g));
			p.setItemSlot(EquipmentSlot.CHEST, GemKit.socketed(Items.NETHERITE_CHESTPLATE, g, g, g));
			p.setItemSlot(EquipmentSlot.LEGS, GemKit.socketed(Items.NETHERITE_LEGGINGS, g, g, g));
			p.setItemSlot(EquipmentSlot.FEET, GemKit.socketed(Items.NETHERITE_BOOTS, g, g, g));
		});
		context.waitTicks(10);
		double openGems = sprint(context, server, new BlockPos(x, y, z));
		double webGems = sprint(context, server, new BlockPos(x, y, z + 3));
		org.slf4j.LoggerFactory.getLogger("bubbas_infused_gems-test").info("Weaving sprint, blocks/tick: open {}, cobwebs {}; perfect set: open {}, cobwebs {}",
				open, plainWeb, openGems, webGems);
		if (!(plainWeb < open * 0.5)) {
			throw new AssertionError("the plain cobweb lane did not slow a sprint: " + plainWeb + " vs " + open);
		}
		if (Math.abs(webGems - openGems) > openGems * 0.03) {
			throw new AssertionError("a perfect Weaving set sprinted through cobwebs at " + webGems + " blocks/tick, open ground " + openGems);
		}
		int[] ticks = new int[2];
		for (int i = 0; i < 2; i++) {
			int col = i == 0 ? -1 : -2;
			server.runOnServer(s -> player(s).teleportTo(x + col + 0.5, y + 11.5, z + col + 0.5));
			context.waitFor(mc -> mc.player.getY() > y + 10);
			int t = 0;
			while (t < 200 && context.computeOnClient(mc -> mc.player.getY()) > y + 0.001) {
				context.waitTicks(1);
				t++;
			}
			ticks[i] = t;
		}
		org.slf4j.LoggerFactory.getLogger("bubbas_infused_gems-test").info("Weaving fall, ticks: open {}, web shaft {}", ticks[0], ticks[1]);
		if (ticks[0] < 10 || Math.abs(ticks[1] - ticks[0]) > 2) {
			throw new AssertionError("a perfect Weaving set fell through webs in " + ticks[1] + " ticks, open air " + ticks[0]);
		}
		server.runOnServer(s -> player(s).teleportTo(x + 20.5, y, z + 3.5));
		context.waitTicks(10);
		context.getInput().lookAt(-90.0F, 10.0F);
		context.waitTicks(5);
		context.takeScreenshot("gems-weaving-perfect-set-in-cobweb-lane");
		server.runOnServer(s -> {
			for (EquipmentSlot slot : io.github.bubba8587.bubbas_infused_gems.effect.GemTotals.ARMOR) {
				player(s).setItemSlot(slot, ItemStack.EMPTY);
			}
		});
	}

	private static net.minecraft.world.entity.monster.zombie.Zombie standingZombie(ServerPlayer p, BlockPos at, Gem g, boolean vanillaInvisibility) {
		net.minecraft.world.entity.monster.zombie.Zombie z = net.minecraft.world.entity.EntityTypes.ZOMBIE.create(p.level(),
				net.minecraft.world.entity.EntitySpawnReason.COMMAND);
		z.setPos(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
		z.setNoAi(true);
		z.setPersistenceRequired();
		z.setYRot(180.0F);
		z.setYHeadRot(180.0F);
		z.setYBodyRot(180.0F);
		z.setItemSlot(EquipmentSlot.HEAD, GemKit.socketed(Items.NETHERITE_HELMET, g, g, g));
		z.setItemSlot(EquipmentSlot.CHEST, GemKit.socketed(Items.NETHERITE_CHESTPLATE, g, g, g));
		z.setItemSlot(EquipmentSlot.LEGS, GemKit.socketed(Items.NETHERITE_LEGGINGS, g, g, g));
		z.setItemSlot(EquipmentSlot.FEET, GemKit.socketed(Items.NETHERITE_BOOTS, g, g, g));
		z.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
		if (vanillaInvisibility) {
			z.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.INVISIBILITY, 20 * 600, 0));
		}
		p.level().addFreshEntity(z);
		io.github.bubba8587.bubbas_infused_gems.effect.WornEffects.refresh(z);
		return z;
	}

	/** The perfect Invisibility set hides the wearer's armor too. */
	private static void invisible(ClientGameTestContext context, TestServerContext server) {
		server.runCommand("difficulty easy");
		server.runCommand("time set noon");
		BlockPos at = stationPos(server).offset(0, 0, 4);
		server.runOnServer(s -> {
			ServerPlayer p = player(s);
			for (EquipmentSlot slot : io.github.bubba8587.bubbas_infused_gems.effect.GemTotals.ARMOR) {
				p.setItemSlot(slot, ItemStack.EMPTY);
			}
			p.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
			p.teleportTo(at.getX() + 0.5, at.getY(), at.getZ() - 3.5);
			standingZombie(p, at.offset(1, 0, 0), GemKit.gem("invisibility", 1.0), false);
			standingZombie(p, at.offset(-1, 0, 0), GemKit.gem("invisibility", 0.9), true);
		});
		context.waitTicks(60);
		boolean[] seen = context.computeOnClient(mc -> {
			boolean[] r = new boolean[2];
			for (var e : mc.level.entitiesForRendering()) {
				if (e instanceof net.minecraft.world.entity.monster.zombie.Zombie z && z.isInvisible()) {
					boolean hidden = io.github.bubba8587.bubbas_infused_gems.effect.HiddenArmor.isHidden(z.getItemBySlot(EquipmentSlot.CHEST));
					r[hidden ? 0 : 1] = true;
				}
			}
			return r;
		});
		if (!seen[0] || !seen[1]) {
			throw new AssertionError("expected one invisible zombie with hidden armor and one with armor showing: " + seen[0] + ", " + seen[1]);
		}
		context.getInput().lookAt(at.above());
		context.waitTicks(5);
		context.takeScreenshot("gems-invisible-perfect-set-hides-armor-left-near-perfect-right");
		server.runOnServer(s -> {
			for (var e : player(s).level().getEntitiesOfClass(net.minecraft.world.entity.monster.zombie.Zombie.class, new net.minecraft.world.phys.AABB(at).inflate(5))) {
				e.discard();
			}
		});
		// The wearer's own inventory in the perfect set
		server.runOnServer(s -> {
			try {
				io.github.bubba8587.bubbas_infused_gems.registry.ModCommands.kit(player(s).createCommandSourceStack(), "invisibility", "netherite");
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
		context.waitTicks(40);
		context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
		context.waitTicks(5);
		context.takeScreenshot("gems-invisible-own-inventory-perfect-set");
		context.setScreen(() -> null);
		server.runOnServer(s -> {
			for (EquipmentSlot slot : io.github.bubba8587.bubbas_infused_gems.effect.GemTotals.ARMOR) {
				player(s).setItemSlot(slot, ItemStack.EMPTY);
			}
			player(s).removeAllEffects();
			player(s).getInventory().clearContent();
		});
	}

	/** Gem effects look like vanilla's on the HUD. */
	private static void looks(ClientGameTestContext context, TestServerContext server) {
		server.runCommand("gamemode survival @a");
		server.runOnServer(s -> {
			ServerPlayer p = player(s);
			p.removeAllEffects();
			p.addEffect(new net.minecraft.world.effect.MobEffectInstance(io.github.bubba8587.bubbas_infused_gems.registry.ModEffects.gem("poison"), 20 * 60,
					io.github.bubba8587.bubbas_infused_gems.effect.GemMobEffect.amplifier(0.5)));
			p.setItemSlot(EquipmentSlot.CHEST, GemKit.socketed(Items.NETHERITE_CHESTPLATE, GemKit.gem("hunger", 1.0)));
			p.setHealth(14.0F);
		});
		context.waitTicks(30);
		context.takeScreenshot("gems-looks-poisoned-hearts-green-food-bar");
		server.runOnServer(s -> {
			ServerPlayer p = player(s);
			p.removeAllEffects();
			p.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
			p.setHealth(p.getMaxHealth());
		});
	}

	/** Hit dials by eye. */
	private static void dials(ClientGameTestContext context, TestServerContext server) {
		server.runCommand("time set noon");
		BlockPos at = stationPos(server).offset(0, 0, 5);
		server.runOnServer(s -> {
			ServerPlayer p = player(s);
			p.teleportTo(at.getX() + 0.5, at.getY(), at.getZ() - 4.5);
			var level = p.level();
			net.minecraft.world.entity.animal.pig.Pig webbed = net.minecraft.world.entity.EntityTypes.PIG.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
			webbed.setPos(at.getX() - 1.5, at.getY(), at.getZ() + 0.5);
			webbed.setNoAi(true);
			level.addFreshEntity(webbed);
			for (int i = 0; i < 40 && level.getBlockStates(webbed.getBoundingBox().inflate(1.5)).noneMatch(b -> b.is(net.minecraft.world.level.block.Blocks.COBWEB)); i++) {
				io.github.bubba8587.bubbas_infused_gems.effect.GemHits.apply(level, webbed, p, p,
						List.of(new io.github.bubba8587.bubbas_infused_gems.effect.GemHits.Entry("weaving", io.github.bubba8587.bubbas_infused_gems.balance.Balance.get().cap, false)));
			}
			net.minecraft.world.entity.animal.pig.Pig lifted = net.minecraft.world.entity.EntityTypes.PIG.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
			lifted.setPos(at.getX() + 2.5, at.getY(), at.getZ() + 0.5);
			level.addFreshEntity(lifted);
			io.github.bubba8587.bubbas_infused_gems.effect.GemHits.apply(level, lifted, p, p,
					List.of(new io.github.bubba8587.bubbas_infused_gems.effect.GemHits.Entry("levitation", io.github.bubba8587.bubbas_infused_gems.balance.Balance.get().cap, false)));
		});
		context.waitTicks(30);
		context.getInput().lookAt(at.above());
		context.waitTicks(2);
		context.takeScreenshot("gems-dials-levitation-hit-left-perfect-weaving-webs-right");
		server.runOnServer(s -> {
			for (var e : player(s).level().getEntitiesOfClass(net.minecraft.world.entity.animal.pig.Pig.class, new net.minecraft.world.phys.AABB(at).inflate(8, 20, 8))) {
				e.discard();
			}
		});
	}
}
