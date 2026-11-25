// dte:C1,B15,B17,B21
package io.github.bubba8587.bubbas_reworked_mobs.camp;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import io.github.bubba8587.bubbas_reworked_mobs.mixin.HoglinHuntInvoker;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jspecify.annotations.Nullable;

/**
 * A camp's roster (specs/monster-camps.md): its kind's mobs, {@link CampTuning.Size#roster} of them plus a leader.
 * Archers take the tower platforms first (roofed); the rest stand round the fire, or by day in the camp's shade (the
 * room, the lean-to, under the towers) since they burn in the sun like any other (camp/CampShade). They stay at the
 * camp (a home radius), never despawn, and are tagged with their camp so their deaths are counted.
 *
 * <p>The leader (owner: slightly larger, tougher, better gear, no banner): scale {@link CampTuning#LEADER_SCALE},
 * {@link CampTuning#LEADER_HEALTH} times the health, more damage, knockback resistance, iron armour, an iron axe or a
 * Power II bow; drops its weapon half the time.
 */
public final class CampRoster {
	/** Entity tag on a camp's leader. */
	public static final String LEADER_TAG = CampTuning.CAMP_TAG + ".leader";

	private CampRoster() {
	}

	/** Spawns the camp's roster now; returns the mobs. */
	public static List<Mob> spawn(ServerLevel level, CampSite site) {
		List<Mob> mobs = new ArrayList<>();
		CampKind kind = site.kind;
		if (kind == null || site.size == null) {
			return mobs;
		}
		RandomSource random = level.getRandom();
		int count = site.size.roster;
		int ground = 0;
		int tower = 0;
		// The leader first, by the fire.
		Mob leader = one(level, site, kind, kind.nether() ? EntityTypes.PIGLIN_BRUTE : kind.mob(), groundSpot(level, site, ground++), random, true, false);
		if (leader != null) {
			mobs.add(leader);
		}
		for (int i = 0; i < count; i++) {
			boolean lookout = kind.archers() && tower < site.towers.size();
			BlockPos at = lookout ? site.towers.get(tower++) : groundSpot(level, site, ground++);
			EntityType<? extends Mob> type = kind.nether() && !lookout && random.nextFloat() < CampTuning.OUTPOST_BRUTE ? EntityTypes.PIGLIN_BRUTE : kind.mob();
			Mob mob = one(level, site, kind, type, at, random, false, lookout);
			if (mob != null) {
				mobs.add(mob);
				if (lookout) {
					mob.setHomeTo(at, 2); // the lookout stays up its tower
				}
			}
		}
		site.spawned = true;
		site.rearmed = false;
		for (Mob mob : mobs) {
			site.members.add(mob.getUUID());
		}
		livestock(level, site, random);
		return mobs;
	}

	/** Entity tag on an outpost's hoglins (not on its roster: they are livestock, not counted). */
	public static final String LIVESTOCK_TAG = CampTuning.CAMP_TAG + ".livestock";

	/**
	 * Tethered livestock, in each pen or stable that has none left and whose tether post stands: a Nether outpost's
	 * hoglins, {@link CampTuning#OUTPOST_HOGLINS} of them, the first leashed to the pen's post, never hunted by the
	 * piglins (as in a bastion's stable); a camp stable's mounts ({@link #mounts}).
	 */
	private static void livestock(ServerLevel level, CampSite site, RandomSource random) {
		for (BlockPos post : site.pens) {
			if (!(level.getBlockState(post).getBlock() instanceof net.minecraft.world.level.block.FenceBlock)) {
				continue; // the pen was taken down
			}
			var box = new net.minecraft.world.phys.AABB(post).inflate(4.0, 2.0, 4.0);
			if (!level.getEntitiesOfClass(Mob.class, box, h -> h.entityTags().contains(LIVESTOCK_TAG)).isEmpty()) {
				continue;
			}
			if (site.kind != null && !site.kind.nether()) {
				mounts(level, site, site.kind, post, random);
				continue;
			}
			int count = site.size == null ? 1 : CampTuning.OUTPOST_HOGLINS[site.size.ordinal()];
			for (int i = 0; i < count; i++) {
				Hoglin hoglin = EntityTypes.HOGLIN.create(level, EntitySpawnReason.STRUCTURE);
				if (hoglin == null) {
					continue;
				}
				int dx = i == 1 ? 1 : i == 2 ? -1 : 0;
				int dz = i == 0 ? 1 : i == 2 ? -1 : -1;
				hoglin.snapTo(post.getX() + dx + 0.5, post.getY(), post.getZ() + dz + 0.5, random.nextFloat() * 360.0F, 0.0F);
				hoglin.finalizeSpawn(level, level.getCurrentDifficultyAt(post), EntitySpawnReason.STRUCTURE, null);
				hoglin.setAge(0);
				hoglin.setPersistenceRequired();
				hoglin.addTag(LIVESTOCK_TAG);
				((HoglinHuntInvoker) hoglin).bubbas_reworked_mobs$setCannotBeHunted(true);
				if (level.environmentAttributes().getValue(EnvironmentAttributes.PIGLINS_ZOMBIFY, hoglin.position())) {
					hoglin.setImmuneToZombification(true);
				}
				if (level.addFreshEntity(hoglin) && i == 0) {
					hoglin.setLeashedTo(LeashFenceKnotEntity.getOrCreateKnot(level, post), true);
				}
			}
		}
	}

	/**
	 * A camp stable's mounts: 1 to {@link CampTuning#STABLE_MOUNTS_MAX}, each the kind's undead mount or a captive horse
	 * or donkey, all leashed to the post (a player takes the leads at the knot). Persistent; not on the roster.
	 */
	private static void mounts(ServerLevel level, CampSite site, CampKind kind, BlockPos post, RandomSource random) {
		net.minecraft.core.Direction front = net.minecraft.core.Direction.getApproximateNearest(site.centre.getX() - post.getX(), 0.0,
				site.centre.getZ() - post.getZ());
		int count = 1 + random.nextInt(CampTuning.STABLE_MOUNTS_MAX);
		LeashFenceKnotEntity knot = LeashFenceKnotEntity.getOrCreateKnot(level, post);
		for (int i = 0; i < count; i++) {
			EntityType<? extends Mob> type = random.nextFloat() < CampTuning.STABLE_UNDEAD ? kind.undeadMount()
					: random.nextFloat() < CampTuning.STABLE_DONKEY ? EntityTypes.DONKEY : EntityTypes.HORSE;
			Mob mount = type.create(level, EntitySpawnReason.STRUCTURE);
			if (mount == null) {
				continue;
			}
			BlockPos at = post.relative(front, 2).relative(i == 0 ? front.getClockWise() : front.getCounterClockWise());
			mount.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
			mount.finalizeSpawn(level, level.getCurrentDifficultyAt(at), EntitySpawnReason.STRUCTURE, null);
			if (mount instanceof net.minecraft.world.entity.AgeableMob young) {
				young.setAge(0);
			}
			mount.setPersistenceRequired();
			mount.addTag(LIVESTOCK_TAG);
			if (level.addFreshEntity(mount)) {
				mount.setLeashedTo(knot, true);
			}
		}
	}

	private static BlockPos groundSpot(ServerLevel level, CampSite site, int i) {
		// By day, the shade instead (in order: the room first, then the lean-to and under the towers).
		if (CampShade.sunBurns(level, site.centre) && site.kind != CampKind.HUSK) {
			int seen = 0;
			for (BlockPos spot : site.shade) {
				BlockPos at = CampShade.resolve(level, spot);
				if (at != null && seen++ == i) {
					return at;
				}
			}
		}
		BlockPos column = site.ground.isEmpty() ? site.centre : site.ground.get(i % site.ground.size());
		if (site.kind != null && site.kind.nether()) {
			return standNear(level, column.atY(site.centre.getY() + 1));
		}
		int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column.getX(), column.getZ());
		return new BlockPos(column.getX(), y, column.getZ());
	}

	/** Under a ceiling: the nearest height to {@code spot} (up to 6 away) with ground below and three free blocks; else the spot. */
	private static BlockPos standNear(ServerLevel level, BlockPos spot) {
		for (int i = 0; i <= 12; i++) {
			BlockPos at = spot.above((i % 2 == 0 ? 1 : -1) * ((i + 1) / 2));
			if (level.getBlockState(at.below()).isFaceSturdy(level, at.below(), net.minecraft.core.Direction.UP) && free(level, at) && free(level, at.above())
					&& free(level, at.above(2))) {
				return at;
			}
		}
		return spot;
	}

	private static boolean free(ServerLevel level, BlockPos pos) {
		return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty() && level.getFluidState(pos).isEmpty();
	}

	private static @Nullable Mob one(ServerLevel level, CampSite site, CampKind kind, EntityType<? extends Mob> type, BlockPos at, RandomSource random,
			boolean leader, boolean lookout) {
		Mob mob = type.create(level, EntitySpawnReason.STRUCTURE);
		if (mob == null) {
			return null;
		}
		mob.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
		mob.finalizeSpawn(level, level.getCurrentDifficultyAt(at), EntitySpawnReason.STRUCTURE, null);
		if (mob instanceof Zombie zombie && zombie.isBaby()) {
			zombie.setBaby(false); // no babies in a camp
		}
		if (mob.isPassenger()) {
			mob.stopRiding(); // no chicken jockeys either
		}
		mob.setPersistenceRequired();
		mob.addTag(CampTuning.CAMP_TAG);
		mob.addTag(CampTuning.CAMP_TAG + ":" + site.id);
		mob.setHomeTo(site.centre, site.size == null ? 8 : site.size.radius + 2);
		if (mob instanceof AbstractPiglin piglin && level.environmentAttributes().getValue(EnvironmentAttributes.PIGLINS_ZOMBIFY, piglin.position())) {
			piglin.setImmuneToZombification(true); // an outpost built outside the Nether (the command, tests)
		}
		if (kind.nether()) {
			arm(level, mob, random, leader, lookout);
		} else if (leader) {
			lead(level, mob, kind);
		} else {
			gear(mob, kind, random);
		}
		if (!level.addFreshEntity(mob)) {
			return null;
		}
		return mob;
	}

	private static void gear(Mob mob, CampKind kind, RandomSource random) {
		if (!kind.archers() && mob.getMainHandItem().isEmpty() && random.nextFloat() < CampTuning.MEMBER_WEAPON) {
			mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(random.nextBoolean() ? Items.STONE_SWORD : Items.STONE_AXE));
		}
	}

	/**
	 * A Nether outpost's piglins: lookouts hold crossbows, the rest a crossbow or golden sword (brutes their golden axe).
	 * The leader is a brute in enchanted gold armour with a Sharpness III golden axe, scaled and toughened like any leader.
	 */
	private static void arm(ServerLevel level, Mob mob, RandomSource random, boolean leader, boolean lookout) {
		if (leader) {
			mob.addTag(LEADER_TAG);
			toughen(mob, true);
			var enchantments = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
			for (EquipmentSlot slot : new EquipmentSlot[] { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS }) {
				ItemStack armour = new ItemStack(slot == EquipmentSlot.HEAD ? Items.GOLDEN_HELMET : slot == EquipmentSlot.CHEST ? Items.GOLDEN_CHESTPLATE
						: Items.GOLDEN_LEGGINGS);
				armour.enchant(enchantments.getOrThrow(Enchantments.PROTECTION), 2);
				mob.setItemSlot(slot, armour);
				mob.setDropChance(slot, CampTuning.LEADER_ARMOR_DROP);
			}
			ItemStack axe = new ItemStack(Items.GOLDEN_AXE);
			axe.enchant(enchantments.getOrThrow(Enchantments.SHARPNESS), 3);
			mob.setItemSlot(EquipmentSlot.MAINHAND, axe);
			mob.setDropChance(EquipmentSlot.MAINHAND, CampTuning.LEADER_WEAPON_DROP);
			return;
		}
		if (mob.getType() == EntityTypes.PIGLIN_BRUTE) {
			mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_AXE));
		} else {
			mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(lookout || random.nextBoolean() ? Items.CROSSBOW : Items.GOLDEN_SWORD));
		}
	}

	private static void toughen(Mob mob, boolean melee) {
		set(mob, Attributes.SCALE, CampTuning.LEADER_SCALE);
		AttributeInstance health = mob.getAttribute(Attributes.MAX_HEALTH);
		if (health != null) {
			health.setBaseValue(health.getBaseValue() * CampTuning.LEADER_HEALTH);
			mob.setHealth(mob.getMaxHealth());
		}
		AttributeInstance damage = mob.getAttribute(Attributes.ATTACK_DAMAGE);
		if (damage != null && melee) {
			damage.setBaseValue(damage.getBaseValue() + CampTuning.LEADER_DAMAGE);
		}
		set(mob, Attributes.KNOCKBACK_RESISTANCE, CampTuning.LEADER_KNOCKBACK_RESISTANCE);
	}

	private static void lead(ServerLevel level, Mob mob, CampKind kind) {
		mob.addTag(LEADER_TAG);
		toughen(mob, !kind.archers());
		mob.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
		mob.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
		ItemStack weapon;
		if (kind.archers()) {
			weapon = new ItemStack(Items.BOW);
			weapon.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.POWER), 2);
		} else {
			weapon = new ItemStack(Items.IRON_AXE);
		}
		mob.setItemSlot(EquipmentSlot.MAINHAND, weapon);
		mob.setDropChance(EquipmentSlot.MAINHAND, CampTuning.LEADER_WEAPON_DROP);
		mob.setDropChance(EquipmentSlot.HEAD, CampTuning.LEADER_ARMOR_DROP);
		mob.setDropChance(EquipmentSlot.CHEST, CampTuning.LEADER_ARMOR_DROP);
	}

	private static void set(Mob mob, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, double value) {
		AttributeInstance instance = mob.getAttribute(attribute);
		if (instance != null) {
			instance.setBaseValue(value);
		}
	}

	/** Whether this mob is a camp's leader. */
	public static boolean isLeader(Mob mob) {
		return mob.entityTags().contains(LEADER_TAG);
	}
}
