// dte:C8
package io.github.bubba8587.bubbas_world_tweaks.fortress;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.structures.NetherFortressPieces;
import org.jspecify.annotations.Nullable;

/** Prismarine trim and sea lanterns by piece type and local position, in place of a nether brick. */
final class FortressTrim {
	private static final BlockState LIGHT = Blocks.SEA_LANTERN.defaultBlockState();
	private static final BlockState TRIM = Blocks.PRISMARINE_BRICKS.defaultBlockState();
	private static final BlockState DARK = Blocks.DARK_PRISMARINE.defaultBlockState();
	/** Lantern spacing along bridge parapets. */
	private static final int PARAPET_LIGHT_SPACING = 6;
	/** Stairs room lifts: their columns (local x), row (z), the landing they pass through (y) and their top cell (y). */
	static final int LIFT_UP_X = 2;
	static final int LIFT_DOWN_X = 4;
	static final int LIFT_Z = 2;
	static final int LIFT_LANDING = 7;
	static final int LIFT_TOP = 9;

	private FortressTrim() {
	}

	static @Nullable BlockState trim(StructurePiece piece, int x, int y, int z) {
		if (y == -1) {
			return TRIM;
		}
		if (piece instanceof NetherFortressPieces.BridgeCrossing || piece instanceof NetherFortressPieces.BridgeStraight
				|| piece instanceof NetherFortressPieces.BridgeEndFiller) {
			return bridge(x, y, z);
		}
		if (piece instanceof NetherFortressPieces.CastleSmallCorridorCrossingPiece) {
			return corridor(x, y, z, true);
		}
		if (piece instanceof NetherFortressPieces.CastleSmallCorridorPiece || piece instanceof NetherFortressPieces.CastleSmallCorridorLeftTurnPiece
				|| piece instanceof NetherFortressPieces.CastleSmallCorridorRightTurnPiece) {
			return corridor(x, y, z, false);
		}
		if (piece instanceof NetherFortressPieces.CastleCorridorTBalconyPiece) {
			return balcony(x, y, z);
		}
		if (piece instanceof NetherFortressPieces.CastleCorridorStairsPiece) {
			return stairs(x, y, z);
		}
		if (piece instanceof NetherFortressPieces.CastleEntrance) {
			return hall(x, y, z, true);
		}
		if (piece instanceof NetherFortressPieces.CastleStalkRoom) {
			return hall(x, y, z, false);
		}
		if (piece instanceof NetherFortressPieces.MonsterThrone) {
			return throne(x, y, z);
		}
		if (piece instanceof NetherFortressPieces.RoomCrossing) {
			return room(x, y, z, true);
		}
		if (piece instanceof NetherFortressPieces.StairsRoom) {
			BlockState lift = lift(x, y, z);
			return lift != null ? lift : y == LIFT_LANDING && x == 3 && z == 2 ? LIGHT : room(x, y, z, false);
		}
		return null;
	}

	/** Whether a vanilla fence here is a window in a wall (glazed) rather than a railing (prismarine wall). */
	static boolean isWindow(StructurePiece piece, int x, int y, int z) {
		if (piece instanceof NetherFortressPieces.BridgeStraight || piece instanceof NetherFortressPieces.MonsterThrone) {
			return false;
		}
		if (piece instanceof NetherFortressPieces.CastleEntrance || piece instanceof NetherFortressPieces.CastleStalkRoom) {
			return y == 10 || y == 11;
		}
		if (piece instanceof NetherFortressPieces.CastleCorridorTBalconyPiece) {
			return y >= 4 || z == 0;
		}
		return true;
	}

	/** What a deep-sea fortress puts where the vanilla piece clears to air, other than water. */
	static @Nullable BlockState inWater(StructurePiece piece, int x, int y, int z) {
		if (piece instanceof NetherFortressPieces.StairsRoom && y == LIFT_TOP + 1 && z == LIFT_Z && (x == LIFT_UP_X || x == LIFT_DOWN_X)) {
			return LIGHT;
		}
		return null;
	}

	/** Stairs room bubble lifts from the ground floor through the landing: soul sand up, magma down. */
	private static @Nullable BlockState lift(int x, int y, int z) {
		if (z != LIFT_Z || x != LIFT_UP_X && x != LIFT_DOWN_X) {
			return null;
		}
		if (y == 1) {
			return x == LIFT_UP_X ? Blocks.SOUL_SAND.defaultBlockState() : Blocks.MAGMA_BLOCK.defaultBlockState();
		}
		if (y == LIFT_LANDING) {
			return Blocks.WATER.defaultBlockState();
		}
		return null;
	}

	/** Bridges: prismarine parapets with sea lanterns set in. */
	private static @Nullable BlockState bridge(int x, int y, int z) {
		if (y == 5) {
			return (x + z) % PARAPET_LIGHT_SPACING == 3 ? LIGHT : TRIM;
		}
		return null;
	}

	/** Small corridors: a dark runner down the floor, one ceiling lantern. */
	private static @Nullable BlockState corridor(int x, int y, int z, boolean crossing) {
		if (y == 6 && x == 2 && z == 2) {
			return LIGHT;
		}
		if (y == 1 && (x == 2 || crossing && z == 2)) {
			return DARK;
		}
		return null;
	}

	/** T balcony: runner, ceiling lantern, prismarine balcony sill. */
	private static @Nullable BlockState balcony(int x, int y, int z) {
		if (y == 6 && x == 4 && z == 2) {
			return LIGHT;
		}
		if (y == 1 && x == 4) {
			return DARK;
		}
		if (y == 2 && z == 8) {
			return TRIM;
		}
		return null;
	}

	/** Corridor stairs: two ceiling lanterns over the flight. */
	private static @Nullable BlockState stairs(int x, int y, int z) {
		return x == 2 && (y == 10 && z == 4 || y == 6 && z == 8) ? LIGHT : null;
	}

	/** Entrance hall and garden hall: floor border, door frames, ceiling lanterns, prismarine merlons, the well rim. */
	private static @Nullable BlockState hall(int x, int y, int z, boolean well) {
		if (y == 11 && (x == 4 || x == 8) && (z == 4 || z == 8)) {
			return LIGHT;
		}
		if (y == 13) {
			return TRIM;
		}
		boolean doorSide = z <= 1 || z >= 11;
		if (doorSide && y >= 5 && y <= 8 && (x == 4 || x == 8)) {
			return TRIM;
		}
		if (doorSide && y == 9 && x >= 5 && x <= 7) {
			return DARK;
		}
		if (y == 4 && x >= 2 && x <= 10 && z >= 2 && z <= 10 && (x == 2 || x == 10 || z == 2 || z == 10)) {
			return TRIM;
		}
		if (well && y == 5 && x >= 5 && x <= 7 && z >= 5 && z <= 7) {
			return TRIM;
		}
		return null;
	}

	/** The spawner room: dark prismarine floor, prismarine rim, lanterns in the back corners. */
	private static @Nullable BlockState throne(int x, int y, int z) {
		if (y == 1) {
			return DARK;
		}
		if (y == 5) {
			return z == 8 && (x == 0 || x == 6) ? LIGHT : TRIM;
		}
		return null;
	}

	/** Room crossing and stairs room: floor border, lintels; the crossing also lights its corner pillars and floor centre. */
	private static @Nullable BlockState room(int x, int y, int z, boolean crossing) {
		if (y == 1 && x >= 1 && x <= 5 && z >= 1 && z <= 5 && (x == 1 || x == 5 || z == 1 || z == 5)) {
			return TRIM;
		}
		if (crossing && y == 1 && x == 3 && z == 3) {
			return LIGHT;
		}
		if (crossing && y == 6 && (x == 0 || x == 6) && (z == 0 || z == 6)) {
			return LIGHT;
		}
		if (y == 6 && (x >= 2 && x <= 4 && (z == 0 || z == 6) || crossing && z >= 2 && z <= 4 && (x == 0 || x == 6))) {
			return DARK;
		}
		return null;
	}
}
