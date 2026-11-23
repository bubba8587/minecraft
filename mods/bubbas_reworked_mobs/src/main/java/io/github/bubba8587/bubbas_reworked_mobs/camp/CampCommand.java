// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.camp;

import io.github.bubba8587.bubbas_reworked_mobs.worldgen.ClippedBuild;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import io.github.bubba8587.bubbas_reworked_mobs.BubbasReworkedMobs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * {@code /bubbas_reworked_mobs camp build <kind> <size> [seed]} builds a camp centred where you stand and registers it
 * (its roster appears when you're in range, as for a natural camp). {@code camp reset} runs the daily reset check now (whoever is near),
 * sure to reset every cleared, armed camp nobody is near; {@code camp locate <kind>} finds the nearest natural camp of a kind; {@code camp list} lists the registered camps.
 */
public final class CampCommand {
	private CampCommand() {
	}

	public static LiteralArgumentBuilder<CommandSourceStack> build() {
		return Commands.literal("camp")
				.then(Commands.literal("build").then(Commands.argument("kind", StringArgumentType.word())
						.then(Commands.argument("size", StringArgumentType.word()).executes(c -> place(c, c.getSource().getLevel().getRandom().nextLong()))
								.then(Commands.argument("seed", LongArgumentType.longArg()).executes(c -> place(c, LongArgumentType.getLong(c, "seed")))))))
				.then(Commands.literal("reset").executes(c -> {
					int n = CampSites.resetCheck(c.getSource().getLevel(), true);
					c.getSource().sendSuccess(() -> Component.literal("Reset " + n + " camp(s)."), true);
					return n;
				}))
				.then(Commands.literal("locate").then(Commands.argument("kind", StringArgumentType.word()).executes(CampCommand::locate)))
				.then(Commands.literal("list").executes(c -> {
					var all = CampSites.all(c.getSource().getLevel());
					for (CampSite site : all) {
						c.getSource().sendSuccess(() -> Component.literal("%s at %s: %s, %s, %d alive".formatted(site.isOutpost() ? "outpost" : site.kind().id(),
								site.centre().toShortString(), site.armed() ? "armed" : "no banner", site.spawned() ? "spawned" : "waiting",
								site.members().size())), false);
					}
					return all.size();
				}));
	}

	private static int locate(CommandContext<CommandSourceStack> c) {
		CampKind kind;
		try {
			kind = CampKind.valueOf(StringArgumentType.getString(c, "kind").toUpperCase(java.util.Locale.ROOT));
		} catch (IllegalArgumentException e) {
			c.getSource().sendFailure(Component.literal("Kinds: skeleton, zombie, husk, stray, bogged."));
			return 0;
		}
		BlockPos at = BlockPos.containing(c.getSource().getPosition());
		CampPiece piece = nearest(c.getSource().getLevel(), at, kind, LOCATE_RADIUS);
		if (piece == null) {
			c.getSource().sendFailure(Component.literal("No %s camp within %d blocks.".formatted(kind.id(), LOCATE_RADIUS)));
			return 0;
		}
		BlockPos o = piece.centre();
		c.getSource().sendSuccess(() -> Component.literal("Nearest %s camp at %d %d %d, %d blocks away.".formatted(kind.id(),
				o.getX(), o.getY(), o.getZ(), (int) Math.sqrt(o.distSqr(at)))), false);
		return 1;
	}

	private static final int LOCATE_RADIUS = 6144;

	/** The nearest natural camp of {@code kind} within {@code radius} (horizontal), read from structure starts; or null. */
	public static CampPiece nearest(ServerLevel level, BlockPos at, CampKind kind, int radius) {
		var structure = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getValue(Camps.CAMP_KEY);
		var set = level.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET).getValue(
				ResourceKey.create(Registries.STRUCTURE_SET, BubbasReworkedMobs.id("monster_camps")));
		if (structure == null || set == null || !(set.placement() instanceof RandomSpreadStructurePlacement placement)) {
			return null;
		}
		long seed = level.getSeed();
		int cell = placement.spacing() * 16;
		int gx0 = Math.floorDiv(at.getX(), cell);
		int gz0 = Math.floorDiv(at.getZ(), cell);
		int rings = radius / cell + 1;
		CampPiece best = null;
		double bestD = (double) radius * radius;
		for (int ring = 0; ring <= rings; ring++) {
			if (best != null && (double) (ring - 1) * cell * (ring - 1) * cell > bestD) {
				break;
			}
			for (int gx = gx0 - ring; gx <= gx0 + ring; gx++) {
				for (int gz = gz0 - ring; gz <= gz0 + ring; gz++) {
					if (Math.max(Math.abs(gx - gx0), Math.abs(gz - gz0)) != ring) {
						continue;
					}
					ChunkPos chunk = placement.getPotentialStructureChunk(seed, gx * placement.spacing(), gz * placement.spacing());
					ChunkAccess access = level.getChunk(chunk.x(), chunk.z(), ChunkStatus.STRUCTURE_STARTS, true);
					StructureStart start = access == null ? null : access.getStartForStructure(structure);
					if (start != null && start.isValid() && start.getPieces().getFirst() instanceof CampPiece piece && piece.kind() == kind) {
						double dx = piece.centre().getX() - at.getX();
						double dz = piece.centre().getZ() - at.getZ();
						if (dx * dx + dz * dz < bestD) {
							bestD = dx * dx + dz * dz;
							best = piece;
						}
					}
				}
			}
		}
		return best;
	}

	private static int place(CommandContext<CommandSourceStack> c, long seed) {
		try {
			CampKind kind = CampKind.valueOf(StringArgumentType.getString(c, "kind").toUpperCase(java.util.Locale.ROOT));
			CampTuning.Size size = CampTuning.Size.valueOf(StringArgumentType.getString(c, "size").toUpperCase(java.util.Locale.ROOT));
			CampPiece piece = build(c.getSource().getLevel(), BlockPos.containing(c.getSource().getPosition()), kind, size, seed);
			c.getSource().sendSuccess(() -> Component.literal("Built a %s %s camp at %s (seed %d).".formatted(size.id(), kind.id(),
					piece.centre().toShortString(), seed)), true);
			return 1;
		} catch (IllegalArgumentException e) {
			c.getSource().sendFailure(Component.literal("Kinds: skeleton, zombie, husk, stray, bogged. Sizes: small, medium, large."));
			return 0;
		}
	}

	/** Builds a camp centred on {@code at}'s column (on its ground) and registers it; returns its piece. */
	public static CampPiece build(ServerLevel level, BlockPos at, CampKind kind, CampTuning.Size size, long seed) {
		int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at.getX(), at.getZ()) - 1;
		CampPiece piece = new CampPiece(seed, new BlockPos(at.getX(), ground, at.getZ()), kind, size);
		ClippedBuild.buildNow(level, piece, at);
		CampSites.register(level, piece);
		return piece;
	}
}
