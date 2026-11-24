// dte:C6
package io.github.bubba8587.bubbas_totems_artifacts.test;

import java.util.Optional;

import io.github.bubba8587.bubbas_totems_artifacts.BubbasTotemsArtifacts;
import io.github.bubba8587.bubbas_totems_artifacts.registry.ModItems;
import io.github.bubba8587.bubbas_totems_artifacts.seeking.SeekingTypes;
import io.github.bubba8587.bubbas_totems_artifacts.seeking.StructureSeeker;
import io.github.bubba8587.bubbas_totems_artifacts.seeking.TotemOfSeekingItem;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/**
 * The totems by eye (client test part {@code totems}): all of them in the hotbar at 16x and 32x, the Beginning and the End held; the
 * Totem of Gathering pulling dropped items in; the Totem of Seeking held, its needle pointing at the nearest structure
 * (straight ahead, then to the side when the player turns). In a normally generated world, the seeker's answer is
 * checked against vanilla's /locate search. Screenshots in build/run/clientGameTest/screenshots/.
 */
@SuppressWarnings("UnstableApiUsage")
public class TotemsClientGameTest implements FabricClientGameTest {
	private static final String ID = BubbasTotemsArtifacts.MOD_ID;
	private static final String PACK_32X = ID + ":" + ID + "_32x";

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!ClientTests.run("totems")) {
			return;
		}
		try (TestSingleplayerContext sp = context.worldBuilder()
				// The test's flat world, with its structures on (villages), so the Totem of Seeking has something to find.
				.adjustSettings(s -> {
					s.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
					s.setGenerateStructures(true);
				})
				.create()) {
			sp.getConnection().waitForChunksRender();
			TestServerContext server = sp.getServer();
			BlockPos base = context.computeOnClient(mc -> mc.player.blockPosition());
			server.runCommand("gamerule send_command_feedback false");
			server.runCommand("gamerule spawn_mobs false");
			server.runCommand("gamerule show_advancement_messages false");
			server.runCommand("gamerule advance_time false");
			server.runCommand("time set noon");
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(base.getX() - 10, base.getY(), base.getZ() - 10, base.getX() + 10, base.getY() + 6, base.getZ() + 10));
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(base.getX() - 10, base.getY() - 1, base.getZ() - 10, base.getX() + 10, base.getY() - 1, base.getZ() + 10));
			server.runCommand("tp @p %d.5 %d %d.5 0 25".formatted(base.getX(), base.getY(), base.getZ()));

			// --- all five in the hotbar (slot 1 selected and empty) -------------------------------------------
			Item[] totems = {ModItems.TOTEM_OF_KEEPING, ModItems.TOTEM_OF_GATHERING, ModItems.TOTEM_OF_FORTIFICATION, ModItems.TOTEM_OF_GROWTH,
					ModItems.TOTEM_OF_HERDING, ModItems.TOTEM_OF_CLEAR_SKIES, ModItems.TOTEM_OF_THE_BEGINNING, ModItems.TOTEM_OF_THE_END};
			context.runOnClient(mc -> mc.player.getInventory().setSelectedSlot(0));
			server.runOnServer(s -> {
				ServerPlayer p = s.getPlayerList().getPlayers().getFirst();
				for (int i = 0; i < totems.length; i++) {
					p.getInventory().setItem(i + 1, new ItemStack(totems[i]));
				}
			});
			context.waitTicks(10);
			context.takeScreenshot("totems-hotbar");
			// The Totems of the Beginning and the End (2026-11-22) held in first person, 16x then 32x.
			context.runOnClient(mc -> mc.player.getInventory().setSelectedSlot(7));
			context.waitTicks(10);
			context.takeScreenshot("totems-held-beginning");
			context.runOnClient(mc -> mc.player.getInventory().setSelectedSlot(8));
			context.waitTicks(10);
			context.takeScreenshot("totems-held-end");
			setPack(context, true);
			context.takeScreenshot("totems-held-end-32x");
			context.runOnClient(mc -> mc.player.getInventory().setSelectedSlot(7));
			context.waitTicks(10);
			context.takeScreenshot("totems-held-beginning-32x");
			context.runOnClient(mc -> mc.player.getInventory().setSelectedSlot(0));
			context.waitTicks(10);
			context.takeScreenshot("totems-hotbar-32x");
			// The rest beside vanilla's Totem of Undying, 32x then 16x.
			Item[] rest = {ModItems.TOTEM_OF_WARDING, ModItems.TOTEM_OF_SEEKING, ModItems.TOTEM_OF_NORTH_WIND, Items.TOTEM_OF_UNDYING};
			server.runOnServer(s -> {
				ServerPlayer p = s.getPlayerList().getPlayers().getFirst();
				for (int i = 0; i < totems.length; i++) {
					p.getInventory().setItem(i + 1, i < rest.length ? new ItemStack(rest[i]) : ItemStack.EMPTY);
				}
			});
			context.waitTicks(10);
			context.takeScreenshot("totems-hotbar2-32x");
			setPack(context, false);
			context.takeScreenshot("totems-hotbar2");
			server.runOnServer(s -> {
				ServerPlayer p = s.getPlayerList().getPlayers().getFirst();
				for (int i = 0; i < totems.length; i++) {
					p.getInventory().setItem(i + 1, new ItemStack(totems[i]));
				}
			});

			// --- Beginning: the revert puff on the blocks a pulse changes (Reworked Mobs only) -----------------
			if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("bubbas_reworked_mobs")) {
				for (int t = 0; t < 4; t++) {
					int wave = t;
					server.runOnServer(s -> {
						java.util.Random random = new java.util.Random(wave);
						for (int i = 0; i < 12; i++) {
							BlockPos at = base.offset(random.nextInt(11) - 5, -1, 2 + random.nextInt(7));
							io.github.bubba8587.bubbas_reworked_mobs.api.Endermen.revertEffect(s.overworld(), at);
						}
					});
					context.waitTicks(4);
				}
				context.takeScreenshot("totems-beginning-pulse");
			}

			// --- Gathering: dropped items fly in ------------------------------------------------------------
			server.runOnServer(s -> {
				ServerPlayer p = s.getPlayerList().getPlayers().getFirst();
				p.getInventory().clearContent();
			});
			server.runCommand("tp @p %d.5 %d %d.5 0 30".formatted(base.getX(), base.getY(), base.getZ()));
			String[] drops = {"minecraft:diamond", "minecraft:gold_ingot", "minecraft:emerald", "minecraft:redstone", "minecraft:iron_ingot", "minecraft:apple"};
			for (int i = 0; i < drops.length; i++) {
				double angle = Math.PI * (0.25 + 0.5 * i / (drops.length - 1)); // a fan in front (south)
				double dx = Math.cos(angle) * 6.5;
				double dz = Math.sin(angle) * 6.5;
				server.runCommand("summon item %.2f %d %.2f {Item:{id:\"%s\",count:1},Motion:[0.0,0.0,0.0]}".formatted(base.getX() + 0.5 + dx, base.getY(), base.getZ() + 0.5 + dz, drops[i]));
			}
			context.waitTicks(10);
			context.takeScreenshot("totems-gathering-before");
			server.runOnServer(s -> s.getPlayerList().getPlayers().getFirst().setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.TOTEM_OF_GATHERING)));
			context.waitTicks(8);
			context.takeScreenshot("totems-gathering-pulling");
			context.waitTicks(60);
			context.takeScreenshot("totems-gathering-after");
			server.runCommand("kill @e[type=item]");

			// --- Gathering ring round a beacon, a side hopper into a chest ---
			server.runOnServer(s -> s.getPlayerList().getPlayers().getFirst().setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY));
			server.runCommand("gamemode spectator @p");
			BlockPos ringBeacon = base.offset(0, 0, 5);
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(ringBeacon.getX() - 6, ringBeacon.getY(), ringBeacon.getZ() - 4, ringBeacon.getX() + 6, ringBeacon.getY() + 4, ringBeacon.getZ() + 6));
			server.runCommand("setblock %d %d %d minecraft:beacon".formatted(ringBeacon.getX(), ringBeacon.getY(), ringBeacon.getZ()));
			server.runCommand("setblock %d %d %d minecraft:hopper[facing=east]".formatted(ringBeacon.getX() + 1, ringBeacon.getY(), ringBeacon.getZ()));
			server.runCommand("setblock %d %d %d minecraft:chest".formatted(ringBeacon.getX() + 2, ringBeacon.getY(), ringBeacon.getZ()));
			for (int i = 0; i < drops.length; i++) {
				double angle = 2 * Math.PI * i / drops.length;
				server.runCommand("summon item %.2f %d %.2f {Item:{id:\"%s\",count:1},Motion:[0.0,0.0,0.0]}".formatted(ringBeacon.getX() + 0.5 + Math.cos(angle) * 5.0,
						ringBeacon.getY(), ringBeacon.getZ() + 0.5 + Math.sin(angle) * 5.0, drops[i]));
			}
			server.runCommand("summon experience_orb %d %d %d {Value:7}".formatted(ringBeacon.getX() - 4, ringBeacon.getY(), ringBeacon.getZ() + 3));
			server.runOnServer(s -> io.github.bubba8587.bubbas_totems_artifacts.gathering.GatheringRing.put(s.overworld(), ringBeacon,
					new net.minecraft.world.phys.AABB(ringBeacon).inflate(8.0), 400));
			server.runCommand("tp @p %.1f %d %.1f 0 30".formatted(ringBeacon.getX() + 0.5, ringBeacon.getY() + 3, ringBeacon.getZ() - 3.0));
			context.waitTicks(30);
			context.takeScreenshot("totems-gathering-ring");
			context.waitTicks(17);
			context.takeScreenshot("totems-gathering-ring-later");
			server.runCommand("kill @e[type=item]");
			server.runCommand("kill @e[type=experience_orb]");

			// --- Gathering statue ring: items within its radius circle the statue ---
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(ringBeacon.getX() - 6, ringBeacon.getY(), ringBeacon.getZ() - 4, ringBeacon.getX() + 6, ringBeacon.getY() + 4, ringBeacon.getZ() + 6));
			server.runOnServer(s -> s.overworld().setBlockAndUpdate(ringBeacon,
					io.github.bubba8587.bubbas_totems_artifacts.statue.TotemStatueBlock.stateFor(io.github.bubba8587.bubbas_totems_artifacts.statue.TotemType.GATHERING)));
			for (int i = 0; i < drops.length; i++) {
				double angle = 2 * Math.PI * i / drops.length;
				server.runCommand("summon item %.2f %d %.2f {Item:{id:\"%s\",count:1},Motion:[0.0,0.0,0.0]}".formatted(ringBeacon.getX() + 0.5 + Math.cos(angle) * 4.0,
						ringBeacon.getY(), ringBeacon.getZ() + 0.5 + Math.sin(angle) * 4.0, drops[i]));
			}
			server.runCommand("tp @p %.1f %.1f %.1f 0 35".formatted(ringBeacon.getX() + 0.5, ringBeacon.getY() + 2.2, ringBeacon.getZ() - 2.2));
			context.waitTicks(90);
			context.takeScreenshot("totems-gathering-statue-ring");
			server.runCommand("setblock %d %d %d minecraft:air".formatted(ringBeacon.getX(), ringBeacon.getY(), ringBeacon.getZ()));
			server.runCommand("kill @e[type=item]");
			server.runCommand("gamemode survival @p");

			// --- Seeking: points at the nearest structure, checked against vanilla's /locate ---------------
			BlockPos target = server.computeOnServer(s -> {
				ServerPlayer p = s.getPlayerList().getPlayers().getFirst();
				ServerLevel level = p.level();
				p.getInventory().clearContent();
				StructureSeeker.clear();
				for (SeekingTypes.SeekType type : SeekingTypes.ALL) {
					var set = type.resolve(level.registryAccess());
					if (set.size() == 0) {
						continue;
					}
					Optional<BlockPos> ours = StructureSeeker.lookupNow(level, type, p.blockPosition());
					if (ours.isEmpty()) {
						continue;
					}
					var vanilla = level.getChunkSource().getGenerator().findNearestMapStructure(level, set, p.blockPosition(), 100, false);
					BubbasTotemsArtifacts.LOGGER.info("Seeking {}: ours {} (from {}), vanilla /locate {}", type.id().getPath(), ours.get(), p.blockPosition(),
							vanilla == null ? null : vanilla.getFirst());
					if (vanilla == null || Math.sqrt(ours.get().distSqr(p.blockPosition())) > Math.sqrt(vanilla.getFirst().distSqr(p.blockPosition())) + 1.0) {
						throw new AssertionError("Seeking " + type.id() + " found " + ours.get() + " but vanilla /locate found " + (vanilla == null ? null : vanilla.getFirst()));
					}
					ItemStack totem = new ItemStack(ModItems.TOTEM_OF_SEEKING);
					TotemOfSeekingItem.choose(totem, type);
					TotemOfSeekingItem.update(totem, level, p.blockPosition());
					p.setItemInHand(InteractionHand.MAIN_HAND, totem);
					p.getInventory().setItem(1, new ItemStack(Items.COMPASS));
					return ours.get();
				}
				throw new AssertionError("no seekable structure within reach in this world");
			});
			float yaw = (float) Math.toDegrees(-Math.atan2(target.getX() + 0.5 - (base.getX() + 0.5), target.getZ() + 0.5 - (base.getZ() + 0.5)));
			server.runCommand("tp @p %d.5 %d %d.5 %.1f 40".formatted(base.getX(), base.getY(), base.getZ(), yaw));
			context.waitTicks(40);
			context.takeScreenshot("totems-seeking-ahead");
			server.runCommand("tp @p %d.5 %d %d.5 %.1f 40".formatted(base.getX(), base.getY(), base.getZ(), yaw + 90.0F));
			context.waitTicks(40);
			context.takeScreenshot("totems-seeking-turned-right");
			setPack(context, true);
			context.waitTicks(20);
			context.takeScreenshot("totems-seeking-turned-right-32x");
			setPack(context, false);
		}
	}

	/** Turns the built-in 32x pack on or off and waits for the reload. */
	private static void setPack(ClientGameTestContext context, boolean on) {
		boolean changed = context.computeOnClient(mc -> {
			PackRepository repo = mc.getResourcePackRepository();
			if (!repo.getAvailableIds().contains(PACK_32X)) {
				throw new AssertionError("no resource pack " + PACK_32X + "; available: " + repo.getAvailableIds());
			}
			if (repo.getSelectedIds().contains(PACK_32X) == on) {
				return false;
			}
			if (on) {
				repo.addPack(PACK_32X);
			} else {
				repo.removePack(PACK_32X);
			}
			mc.options.updateResourcePacks(repo);
			return true;
		});
		if (changed) {
			context.waitTicks(2);
			context.waitFor(mc -> mc.gui.overlay() == null, 1200);
			context.waitTicks(5);
		}
	}
}
