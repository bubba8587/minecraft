// dte:A3
package io.github.bubba8587.postershots.test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import net.minecraft.client.CameraType;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.GraphicsPreset;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.commands.CommandSource;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.levelgen.Heightmap;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.fabric.impl.client.gametest.world.TestWorldSaveImpl;

/**
 * Live poster camera (specs/posters.md). Opens a copy of the natural test world, then waits for scene scripts:
 * every *.txt dropped into <dir>/queue runs line by line (commands, camera moves, screenshots), its replies go to
 * <dir>/queue/<name>.log and the file is renamed *.done. A script named quit.txt ends the run. Screenshots land in
 * <dir>/shots/<name>.png.
 */
@SuppressWarnings("UnstableApiUsage")
public class PosterShots implements FabricClientGameTest {
	private static final double EYE = 1.62;
	private static final int IDLE_LIMIT_TICKS = 20 * 60 * 120;

	private ClientGameTestContext context;
	private TestSingleplayerContext world;
	private Path shots;
	private final List<String> log = new ArrayList<>();
	private int width = 1920;
	private int height = 1080;
	private int settle = 10;
	private int chunkWait = 20 * 180;
	private String dim = "minecraft:overworld";
	private final java.util.Map<String, String> vars = new java.util.TreeMap<>(java.util.Comparator.comparingInt(String::length).reversed().thenComparing(s -> s));

	@Override
	public void runTest(ClientGameTestContext context) {
		this.context = context;
		Path dir = Path.of(System.getProperty("bubba.posters.dir", "build/run/posters"));
		Path queue = dir.resolve("queue");
		shots = dir.resolve("shots");
		Path source = Path.of(System.getProperty("bubba.posters.world", "../../testworlds/bubba-natural-world"));
		Path saves = context.computeOnClient(mc -> mc.getLevelSource().getBaseDir());
		Path copy = saves.resolve("poster-world");
		try {
			Files.createDirectories(queue);
			Files.createDirectories(shots);
			Files.writeString(dir.resolve("STATUS"), "loading\n");
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		copyWorld(source, copy);
		context.getInput().resizeWindow(320, 180);

		try (TestSingleplayerContext sp = new TestWorldSaveImpl(context, copy).open()) {
			this.world = sp;
			sp.getConnection().waitForChunksRender();
			for (String c : new String[] {"gamerule send_command_feedback false", "gamerule spawn_mobs false",
					"gamerule advance_time false", "gamerule advance_weather false", "weather clear", "gamemode spectator @a"}) {
				command(c);
			}
			context.runOnClient(mc -> {
				mc.options.graphicsPreset().set(GraphicsPreset.FABULOUS);
				GraphicsPreset.FABULOUS.apply(mc);
				mc.options.renderDistance().set(8);
				mc.options.simulationDistance().set(6);
				mc.options.framerateLimit().set(260);
				mc.options.enableVsync().set(false);
				if (!mc.gui.hud.isHidden()) mc.gui.hud.toggle();
			});
			context.getInput().setCursorPos(0, 0);
			status(dir, "ready");
			int idle = 0;
			while (idle < IDLE_LIMIT_TICKS) {
				Path next = nextScript(queue);
				if (next == null) {
					context.waitTicks(10);
					idle += 10;
					continue;
				}
				idle = 0;
				String name = next.getFileName().toString();
				if (name.equals("quit.txt")) {
					command("save-all flush");
					rename(next, "done");
					break;
				}
				status(dir, "running " + name);
				runScript(next);
				status(dir, "ready");
			}
			status(dir, "stopped");
		}
	}

	private static Path nextScript(Path queue) {
		try (Stream<Path> files = Files.list(queue)) {
			return files.filter(p -> p.getFileName().toString().endsWith(".txt")).sorted().findFirst().orElse(null);
		} catch (IOException e) {
			return null;
		}
	}

	private void runScript(Path script) {
		log.clear();
		List<String> lines;
		try {
			lines = Files.readAllLines(script, StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		Path running = rename(script, "running");
		Path logFile = running.resolveSibling(script.getFileName().toString().replace(".txt", ".log"));
		Path abort = running.resolveSibling("abort");
		for (String raw : lines) {
			String line = raw.strip();
			if (line.isEmpty() || line.startsWith("#")) continue;
			if (Files.exists(abort)) {
				log.add("!! aborted");
				break;
			}
			log.add("> " + line);
			try {
				step(line);
			} catch (Throwable t) {
				log.add("!! " + t);
				// A failed menu or a stray screen would block everything after it.
				context.runOnClient(mc -> mc.gui.setScreen(null));
			}
			writeLog(logFile);
		}
		writeLog(logFile);
		try {
			Files.deleteIfExists(abort);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		rename(running, "done");
	}

	private void writeLog(Path file) {
		try {
			Files.write(file, log, StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/** One script line ($NAME is replaced by a variable's value first). */
	private void step(String line) {
		for (var v : vars.entrySet()) line = line.replace("$" + v.getKey(), v.getValue());
		if (line.startsWith("/") || line.startsWith("run ")) {
			StringBuilder b = new StringBuilder();
			for (String tok : line.split(" ", -1)) {
				if (b.length() > 0) b.append(' ');
				b.append(tok.matches("-?[0-9.]+([+-][0-9.]+)+|-?[0-9]+\\.[0-9]+") ? fmt(num(tok)) : tok);
			}
			line = b.toString();
		}
		log.add("  = " + line);
		if (line.startsWith("/")) {
			String c = line.substring(1);
			command(dim.equals("minecraft:overworld") || c.startsWith("execute in ") ? c : "execute in " + dim + " run " + c);
			return;
		}
		String[] a = line.split("\\s+");
		switch (a[0]) {
			case "wait" -> context.waitTicks(Integer.parseInt(a[1]));
			// let NAME x z [offset]: NAME = surface height there (+ offset)
			case "let" -> {
				int x = (int) Math.floor(num(a[2])), z = (int) Math.floor(num(a[3]));
				vars.put(a[1], fmt(ground(x, z) + (a.length > 4 ? num(a[4]) : 0)));
				log.add("  " + a[1] + " = " + vars.get(a[1]));
			}
			case "set" -> vars.put(a[1], fmt(num(a[2])));
			// entity NAME tag: NAMEx NAMEy NAMEz = where the tagged entity is
			case "entity" -> {
				double[] pos = world.getServer().computeOnServer(s -> {
					for (net.minecraft.world.entity.Entity e : level(s).getAllEntities()) {
						if (e.entityTags().contains(a[2])) return new double[] {e.getX(), e.getY(), e.getZ()};
					}
					return null;
				});
				if (pos == null) throw new IllegalStateException("no entity tagged " + a[2]);
				vars.put(a[1] + "x", fmt(pos[0]));
				vars.put(a[1] + "y", fmt(pos[1]));
				vars.put(a[1] + "z", fmt(pos[2]));
				log.add(String.format(Locale.ROOT, "  %s at %.2f %.2f %.2f", a[2], pos[0], pos[1], pos[2]));
			}
			case "settle" -> settle = Integer.parseInt(a[1]);
			case "chunkwait" -> chunkWait = Integer.parseInt(a[1]);
			case "size" -> {
				width = Integer.parseInt(a[1]);
				height = Integer.parseInt(a[2]);
			}
			case "window" -> context.getInput().resizeWindow(Integer.parseInt(a[1]), Integer.parseInt(a[2]));
			case "dim" -> dim = a[1].contains(":") ? a[1] : "minecraft:" + a[1];
			case "render" -> context.runOnClient(mc -> mc.options.renderDistance().set(Integer.parseInt(a[1])));
			case "sim" -> context.runOnClient(mc -> mc.options.simulationDistance().set(Integer.parseInt(a[1])));
			case "fps" -> context.runOnClient(mc -> mc.options.framerateLimit().set(Integer.parseInt(a[1])));
			// fly: a creative player hovers where it is put
			case "fly" -> world.getServer().runOnServer(s -> {
				ServerPlayer p = player(s);
				p.getAbilities().flying = true;
				p.onUpdateAbilities();
			});
			// watch creeperTag targetTag: the tagged creeper watches the tagged entity (Bubba's Reworked Mobs)
			case "watch" -> world.getServer().runOnServer(s -> {
				ServerLevel l = level(s);
				net.minecraft.world.entity.Entity creeper = null, target = null;
				for (net.minecraft.world.entity.Entity e : l.getAllEntities()) {
					if (e.entityTags().contains(a[1])) creeper = e;
					if (e.entityTags().contains(a[2])) target = e;
				}
				if (creeper == null || target == null) throw new IllegalStateException("no creeper or target tagged");
				try {
					Class<?> access = Class.forName("io.github.bubba8587.bubbas_reworked_mobs.creeper.CreeperAccess");
					java.lang.reflect.Method mind = access.getDeclaredMethod("mind", net.minecraft.world.entity.monster.Creeper.class);
					mind.setAccessible(true);
					Object m = mind.invoke(null, creeper);
					java.lang.reflect.Method watch = m.getClass().getMethod("watch", net.minecraft.world.entity.LivingEntity.class, long.class);
					watch.invoke(m, target, l.getGameTime());
				} catch (ReflectiveOperationException e) {
					throw new RuntimeException(e);
				}
			});
			// grab drownedTag victimTag: the tagged drowned grabs the tagged mob now (Bubba's Reworked Mobs)
			case "grab" -> world.getServer().runOnServer(s -> {
				ServerLevel l = level(s);
				net.minecraft.world.entity.Entity drowned = null, victim = null;
				for (net.minecraft.world.entity.Entity e : l.getAllEntities()) {
					if (e.entityTags().contains(a[1])) drowned = e;
					if (e.entityTags().contains(a[2])) victim = e;
				}
				if (drowned == null || victim == null) throw new IllegalStateException("no drowned or victim tagged");
				try {
					Class<?> access = Class.forName("io.github.bubba8587.bubbas_reworked_mobs.drowned.DrownedAccess");
					Object m = access.getMethod("mind", net.minecraft.world.entity.monster.zombie.Drowned.class).invoke(null, drowned);
					m.getClass().getMethod("grab", net.minecraft.world.entity.monster.zombie.Drowned.class, ServerLevel.class,
							net.minecraft.world.entity.LivingEntity.class).invoke(m, drowned, l, victim);
				} catch (ReflectiveOperationException e) {
					throw new RuntimeException(e);
				}
			});
			// yard NAME x z seed width depth state wealth: builds a crypt yard (Bubba's Reworked Mobs) with its gate there;
			// NAMEg* = gate, NAMEv* = vault centre (if it has a crypt)
			case "yard" -> world.getServer().runOnServer(s -> {
				try {
					String pkg = "io.github.bubba8587.bubbas_reworked_mobs.grave.";
					Class<?> cmd = Class.forName(pkg + "GraveyardCommand");
					Class<?> kinds = Class.forName(pkg + "YardKinds");
					Class<?> stateC = Class.forName(pkg + "YardKinds$State");
					Class<?> wealthC = Class.forName(pkg + "YardKinds$Wealth");
					int x = (int) Math.floor(num(a[2])), z = (int) Math.floor(num(a[3]));
					ServerLevel l = level(s);
					BlockPos gate = new BlockPos(x, l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
					Object st = stateC.getMethod("valueOf", String.class).invoke(null, a[7].toUpperCase(Locale.ROOT));
					Object we = wealthC.getMethod("valueOf", String.class).invoke(null, a[8].toUpperCase(Locale.ROOT));
					Object piece = cmd.getMethod("build", ServerLevel.class, BlockPos.class, long.class, int.class, int.class, boolean.class, stateC, wealthC)
							.invoke(null, l, gate, Long.parseLong(a[4]), Integer.parseInt(a[5]), Integer.parseInt(a[6]), true, st, we);
					BlockPos g = (BlockPos) piece.getClass().getMethod("gate").invoke(piece);
					vars.put(a[1] + "gx", fmt(g.getX()));
					vars.put(a[1] + "gy", fmt(g.getY()));
					vars.put(a[1] + "gz", fmt(g.getZ()));
					if ((boolean) piece.getClass().getMethod("hasCrypt").invoke(piece)) {
						BlockPos v = (BlockPos) piece.getClass().getMethod("vaultCentre").invoke(piece);
						vars.put(a[1] + "vx", fmt(v.getX()));
						vars.put(a[1] + "vy", fmt(v.getY()));
						vars.put(a[1] + "vz", fmt(v.getZ()));
						log.add("  vault at " + v.toShortString());
					}
					log.add("  yard gate " + g.toShortString() + ", " + piece.getClass().getMethod("width").invoke(piece) + " x "
							+ piece.getClass().getMethod("depth").invoke(piece));
				} catch (ReflectiveOperationException e) {
					throw new RuntimeException(e);
				}
			});
			// findlava NAME x z r: the open lava surface (lava with air above, y 25..40) within r with the most lava round it
			case "findlava" -> world.getServer().runOnServer(s -> {
				ServerLevel l = level(s);
				int cx = (int) Math.floor(num(a[2])), cz = (int) Math.floor(num(a[3])), r = Integer.parseInt(a[4]);
				int[][] top = new int[2 * r + 1][2 * r + 1];
				for (int dx = -r; dx <= r; dx++) {
					for (int dz = -r; dz <= r; dz++) {
						top[dx + r][dz + r] = Integer.MIN_VALUE;
						for (int y = 40; y >= 25; y--) {
							BlockPos p = new BlockPos(cx + dx, y, cz + dz);
							if (l.getFluidState(p).is(net.minecraft.tags.FluidTags.LAVA) && l.getBlockState(p.above()).isAir()) {
								top[dx + r][dz + r] = y;
								break;
							}
						}
					}
				}
				int best = -1, bx = cx, bz = cz, by = 31;
				for (int dx = -r + 6; dx <= r - 6; dx++) {
					for (int dz = -r + 6; dz <= r - 6; dz++) {
						if (top[dx + r][dz + r] == Integer.MIN_VALUE) continue;
						int n = 0;
						for (int ex = -6; ex <= 6; ex++) for (int ez = -6; ez <= 6; ez++) if (top[dx + r + ex][dz + r + ez] != Integer.MIN_VALUE) n++;
						if (n > best) {
							best = n;
							bx = cx + dx;
							bz = cz + dz;
							by = top[dx + r][dz + r];
						}
					}
				}
				vars.put(a[1] + "x", fmt(bx));
				vars.put(a[1] + "y", fmt(by));
				vars.put(a[1] + "z", fmt(bz));
				log.add("  lava at " + bx + " " + by + " " + bz + " (" + best + " of 169 open)");
			});
			// erupt x y z: a lava geyser erupts now from the lava surface block there (Bubba's World Tweaks)
			case "erupt" -> world.getServer().runOnServer(s -> {
				try {
					Class<?> geysers = Class.forName("io.github.bubba8587.bubbas_world_tweaks.lava.LavaGeysers");
					Object g = geysers.getMethod("erupt", ServerLevel.class, BlockPos.class).invoke(null, level(s),
							BlockPos.containing(num(a[1]), num(a[2]), num(a[3])));
					log.add("  erupt -> " + g);
				} catch (ReflectiveOperationException e) {
					throw new RuntimeException(e);
				}
			});
			case "fov" -> context.runOnClient(mc -> mc.options.fov().set(Integer.parseInt(a[1])));
			case "gui" -> context.runOnClient(mc -> {
				mc.options.guiScale().set(Integer.parseInt(a[1]));
				mc.resizeGui();
			});
			case "clouds" -> context.runOnClient(mc -> mc.options.cloudStatus().set(a[1].equals("off") ? CloudStatus.OFF : CloudStatus.FANCY));
			// attack / release: hold or let go of the attack key (mid-break pictures)
			case "attack" -> context.getInput().holdKey(o -> o.keyAttack);
			case "release" -> context.getInput().releaseKey(o -> o.keyAttack);
			case "hud" -> context.runOnClient(mc -> {
				if (mc.gui.hud.isHidden() == a[1].equals("on")) mc.gui.hud.toggle();
			});
			case "view" -> context.runOnClient(mc -> mc.options.setCameraType(switch (a[1]) {
				case "back" -> CameraType.THIRD_PERSON_BACK;
				case "front" -> CameraType.THIRD_PERSON_FRONT;
				default -> CameraType.FIRST_PERSON;
			}));
			case "ground" -> {
				int x = (int) Math.floor(Double.parseDouble(a[1])), z = (int) Math.floor(Double.parseDouble(a[2]));
				log.add("ground " + x + " " + z + " = " + ground(x, z));
			}
			case "pos" -> log.add("player at " + world.getServer().computeOnServer(s -> {
				ServerPlayer p = player(s);
				return String.format(Locale.ROOT, "%.2f %.2f %.2f yaw %.1f pitch %.1f in %s", p.getX(), p.getY(), p.getZ(), p.getYRot(), p.getXRot(), p.level().dimension().identifier());
			}));
			// pose ex ey ez tx ty tz: put the camera (eye) there looking at the target, no picture
			case "pose" -> aim(coord(a[1], a[1], a[3]), coordY(a[2], a[1], a[3]), coord(a[3], a[1], a[3]),
					coord(a[4], a[4], a[6]), coordY(a[5], a[4], a[6]), coord(a[6], a[4], a[6]));
			// place x y z yaw pitch: put the player's feet there with that facing
			case "place" -> tp(num(a[1]), coordY(a[2], a[1], a[3]), num(a[3]), (float) num(a[4]), (float) num(a[5]));
			// shot name ex ey ez tx ty tz
			case "shot" -> {
				aim(coord(a[2], a[2], a[4]), coordY(a[3], a[2], a[4]), coord(a[4], a[2], a[4]),
						coord(a[5], a[5], a[7]), coordY(a[6], a[5], a[7]), coord(a[7], a[5], a[7]));
				snap(a[1]);
			}
			// orbit name tx ty tz dist up yaw[,yaw...]: shots round a target; yaw 0 = camera south of it looking north
			case "orbit" -> {
				double tx = num(a[2]), tz = num(a[4]);
				double ty = coordY(a[3], a[2], a[4]);
				double dist = num(a[5]), up = num(a[6]);
				for (String y : a[7].split(",")) {
					double yaw = Math.toRadians(num(y));
					double ex = tx - Math.sin(yaw) * dist, ez = tz + Math.cos(yaw) * dist;
					double ey = a[3].startsWith("g") ? Math.max(ty + up, ground((int) Math.floor(ex), (int) Math.floor(ez)) + 1.5) : ty + up;
					aim(ex, ey, ez, tx, ty, tz);
					snap(a[1] + "_" + y);
				}
			}
			case "snap" -> snap(a[1]);
			// run x y z command...: the command runs positioned there (y may be g / g+N)
			case "run" -> command(String.format(Locale.ROOT, "execute in %s positioned %s %s %s run %s", dim, fmt(num(a[1])),
					fmt(coordY(a[2], a[1], a[3])), fmt(num(a[3])), line.split("\\s+", 5)[4]));
			// burst name count interval: quick pictures without waiting for chunks (motion)
			case "burst" -> {
				int n = Integer.parseInt(a[2]), every = Integer.parseInt(a[3]);
				for (int i = 0; i < n; i++) {
					context.waitTicks(every);
					take(a[1] + "_" + i);
				}
			}
			case "glide" -> world.getServer().runOnServer(s -> {
				ServerPlayer p = player(s);
				p.startFallFlying();
			});
			// chest name x y z [slot...]: open the container there, picture it, then one picture per hovered slot
			case "chest" -> chest(a);
			case "close" -> {
				world.getServer().runOnServer(s -> player(s).closeContainer());
				context.waitForScreen(null);
			}
			default -> log.add("?? unknown step " + a[0]);
		}
	}

	private static String fmt(double v) {
		return v == Math.rint(v) ? String.valueOf((long) v) : String.format(Locale.ROOT, "%.2f", v);
	}

	/** A number, or a sum like 12.5+3-1. */
	private static double num(String v) {
		java.util.regex.Matcher m = java.util.regex.Pattern.compile("[+-]?[0-9.]+").matcher(v);
		double sum = 0;
		int end = 0;
		while (m.find()) {
			if (m.start() != end) throw new NumberFormatException(v);
			sum += Double.parseDouble(m.group());
			end = m.end();
		}
		if (end != v.length() || end == 0) throw new NumberFormatException(v);
		return sum;
	}

	/** x or z: a number. */
	private static double coord(String v, String x, String z) {
		return num(v);
	}

	/** y: a number, or g / g+N / g-N for the surface (motion-blocking, no leaves) at that column plus N. */
	private double coordY(String v, String x, String z) {
		if (!v.startsWith("g")) return num(v);
		int bx = (int) Math.floor(num(x)), bz = (int) Math.floor(num(z));
		return ground(bx, bz) + (v.length() > 1 ? num(v.substring(1)) : 0.0);
	}

	private int ground(int x, int z) {
		return world.getServer().computeOnServer(s -> {
			ServerLevel level = level(s);
			level.getChunk(x >> 4, z >> 4);
			return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
		});
	}

	private ServerLevel level(MinecraftServer s) {
		for (ServerLevel l : s.getAllLevels()) {
			if (l.dimension().identifier().toString().equals(dim)) return l;
		}
		return s.overworld();
	}

	private static ServerPlayer player(MinecraftServer s) {
		return s.getPlayerList().getPlayers().getFirst();
	}

	private void aim(double ex, double ey, double ez, double tx, double ty, double tz) {
		double dx = tx - ex, dy = ty - ey, dz = tz - ez;
		float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
		float pitch = (float) Math.toDegrees(-Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
		tp(ex, ey - EYE, ez, yaw, pitch);
	}

	private void tp(double x, double y, double z, float yaw, float pitch) {
		command(String.format(Locale.ROOT, "execute in %s run tp @a %.3f %.3f %.3f %.2f %.2f", dim, x, y, z, yaw, pitch));
	}

	private void snap(String name) {
		context.waitTicks(2);
		waitForSections();
		context.waitTicks(settle);
		take(name);
	}

	/** Until the section build queue is empty and the rendered count holds, or the count stops growing for 4 s. */
	private void waitForSections() {
		int last = -1, same = 0;
		for (int t = 0; t < chunkWait; t += 10) {
			context.waitTicks(10);
			int[] state = context.computeOnClient(mc -> new int[] {mc.levelExtractor.countRenderedSections(), mc.levelRenderer.hasRenderedAllSections() ? 1 : 0});
			same = state[0] == last ? same + 1 : 0;
			last = state[0];
			if ((state[1] == 1 && same >= 2) || same >= 8) {
				log.add("  sections " + last + " after " + t + " ticks" + (state[1] == 1 ? "" : " (queue not empty)"));
				return;
			}
		}
		log.add("!! sections still building after " + chunkWait + " ticks");
	}

	private void take(String name) {
		Path p = context.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix().withSize(width, height).withDestinationDir(shots));
		log.add("shot " + p.getFileName());
	}

	private void chest(String[] a) {
		BlockPos pos = BlockPos.containing(num(a[2]), num(a[3]), num(a[4]));
		world.getServer().runOnServer(s -> {
			ServerPlayer p = player(s);
			if (!(level(s).getBlockEntity(pos) instanceof MenuProvider menu)) {
				throw new IllegalStateException("no container at " + pos.toShortString());
			}
			p.openMenu(menu);
		});
		context.waitForScreen(AbstractContainerScreen.class);
		context.getInput().setCursorPos(0, 0);
		context.waitTicks(3);
		take(a[1]);
		for (int i = 5; i < a.length; i++) {
			int index = Integer.parseInt(a[i]);
			double[] at = context.computeOnClient(mc -> slotCenter(mc, index));
			context.getInput().setCursorPos(at[0], at[1]);
			context.waitTicks(3);
			take(a[1] + "_slot" + index);
		}
		context.getInput().setCursorPos(0, 0);
		world.getServer().runOnServer(s -> player(s).closeContainer());
		context.waitForScreen(null);
	}

	private static double[] slotCenter(Minecraft mc, int index) {
		AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) mc.gui.screen();
		Slot slot = screen.getMenu().slots.get(index);
		double scale = mc.getWindow().getScreenWidth() / (double) mc.getWindow().getGuiScaledWidth();
		return new double[] {(intField(screen, "leftPos") + slot.x + 8) * scale, (intField(screen, "topPos") + slot.y + 8) * scale};
	}

	private static int intField(AbstractContainerScreen<?> screen, String name) {
		try {
			Field f = AbstractContainerScreen.class.getDeclaredField(name);
			f.setAccessible(true);
			return f.getInt(screen);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	/** Runs a server command as the console, keeping its replies for the script's log. */
	private void command(String cmd) {
		List<String> replies = world.getServer().computeOnServer(s -> {
			List<String> out = new ArrayList<>();
			CommandSource capture = new CommandSource() {
				@Override
				public void sendSystemMessage(Component message) {
					out.add(message.getString());
				}

				@Override
				public boolean acceptsSuccess() {
					return true;
				}

				@Override
				public boolean acceptsFailure() {
					return true;
				}

				@Override
				public boolean shouldInformAdmins() {
					return false;
				}
			};
			s.getCommands().performPrefixedCommand(s.createCommandSourceStack().withSource(capture), cmd);
			return out;
		});
		for (String r : replies) log.add("  " + r);
	}

	private static void status(Path dir, String s) {
		try {
			Files.writeString(dir.resolve("STATUS"), s + "\n");
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static Path rename(Path p, String ext) {
		Path to = p.resolveSibling(p.getFileName().toString().replaceAll("\\.(txt|running)$", "") + "." + ext);
		try {
			return Files.move(p, to, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static void copyWorld(Path from, Path to) {
		if (Files.isRegularFile(to.resolve("level.dat")) && !Boolean.getBoolean("bubba.posters.fresh")) {
			return;
		}
		if (!Files.isRegularFile(from.resolve("level.dat"))) {
			throw new AssertionError("no natural world at " + from.toAbsolutePath() + " (python tools/build-naturalworld.py)");
		}
		try (Stream<Path> files = Files.walk(from)) {
			for (Path f : (Iterable<Path>) files::iterator) {
				Path target = to.resolve(from.relativize(f).toString());
				if (Files.isDirectory(f)) {
					Files.createDirectories(target);
				} else if (!f.getFileName().toString().equals("session.lock")) {
					Files.copy(f, target, StandardCopyOption.REPLACE_EXISTING);
				}
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
