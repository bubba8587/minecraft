// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.camp;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
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
 * Archers take the tower platforms first; the rest stand round the fire. They stay at the camp (a home radius), don't
 * burn by day (they keep to camp in the daylight; mixin/CampMemberMixin), never despawn, and are tagged with their camp
 * so their deaths are counted.
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
		Mob leader = one(level, site, kind, groundSpot(level, site, ground++), random, true);
		if (leader != null) {
			mobs.add(leader);
		}
		for (int i = 0; i < count; i++) {
			boolean lookout = kind.archers() && tower < site.towers.size();
			BlockPos at = lookout ? site.towers.get(tower++) : groundSpot(level, site, ground++);
			Mob mob = one(level, site, kind, at, random, false);
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
		return mobs;
	}

	private static BlockPos groundSpot(ServerLevel level, CampSite site, int i) {
		BlockPos column = site.ground.isEmpty() ? site.centre : site.ground.get(i % site.ground.size());
		int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column.getX(), column.getZ());
		return new BlockPos(column.getX(), y, column.getZ());
	}

	private static @Nullable Mob one(ServerLevel level, CampSite site, CampKind kind, BlockPos at, RandomSource random, boolean leader) {
		Mob mob = kind.mob().create(level, EntitySpawnReason.STRUCTURE);
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
		if (leader) {
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
		if (random.nextFloat() < CampTuning.MEMBER_HELMET) {
			mob.setItemSlot(EquipmentSlot.HEAD, new ItemStack(random.nextBoolean() ? Items.LEATHER_HELMET : Items.CHAINMAIL_HELMET));
		}
		if (!kind.archers() && mob.getMainHandItem().isEmpty() && random.nextFloat() < CampTuning.MEMBER_WEAPON) {
			mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(random.nextBoolean() ? Items.STONE_SWORD : Items.STONE_AXE));
		}
	}

	private static void lead(ServerLevel level, Mob mob, CampKind kind) {
		mob.addTag(LEADER_TAG);
		set(mob, Attributes.SCALE, CampTuning.LEADER_SCALE);
		AttributeInstance health = mob.getAttribute(Attributes.MAX_HEALTH);
		if (health != null) {
			health.setBaseValue(health.getBaseValue() * CampTuning.LEADER_HEALTH);
			mob.setHealth(mob.getMaxHealth());
		}
		AttributeInstance damage = mob.getAttribute(Attributes.ATTACK_DAMAGE);
		if (damage != null && !kind.archers()) {
			damage.setBaseValue(damage.getBaseValue() + CampTuning.LEADER_DAMAGE);
		}
		set(mob, Attributes.KNOCKBACK_RESISTANCE, CampTuning.LEADER_KNOCKBACK_RESISTANCE);
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
