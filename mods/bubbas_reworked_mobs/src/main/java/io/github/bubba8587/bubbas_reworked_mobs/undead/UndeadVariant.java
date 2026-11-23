// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.undead;

import java.util.List;
import java.util.function.Supplier;

import io.github.bubba8587.bubbas_reworked_mobs.undead.entity.WokenDrowned;
import io.github.bubba8587.bubbas_reworked_mobs.undead.entity.WokenHusk;
import io.github.bubba8587.bubbas_reworked_mobs.undead.entity.WokenSkeleton;
import io.github.bubba8587.bubbas_reworked_mobs.undead.entity.WokenStray;
import io.github.bubba8587.bubbas_reworked_mobs.undead.entity.WokenZombie;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

/**
 * The undead variant table. Every variant gets a sleeper block + item ({@code <name>_sleeper}) and a woken
 * entity type ({@code woken_<name>}); adding stray, husk, drowned... is one entry here plus one in the client
 * table ({@code SleeperRenderer}) and the lang/texture lists in datagen.
 */
public enum UndeadVariant {
	SKELETON("skeleton", List.of(SleeperPose.SITTING, SleeperPose.LYING), SoundType.BONE_BLOCK, MapColor.QUARTZ, Items.BONE,
			() -> EntityType.Builder.of(WokenSkeleton::new, MobCategory.MONSTER).sized(0.6F, 1.99F).eyeHeight(1.74F).ridingOffset(-0.7F)
					.clientTrackingRange(8).notInPeaceful(),
			UndeadTuning::wokenSkeletonAttributes),
	ZOMBIE("zombie", List.of(SleeperPose.SITTING, SleeperPose.LYING), SoundType.MUD, MapColor.COLOR_GREEN, Items.ROTTEN_FLESH,
			() -> EntityType.Builder.<WokenZombie>of(WokenZombie::new, MobCategory.MONSTER).sized(0.6F, 1.95F).eyeHeight(1.74F)
					.passengerAttachments(2.0125F).ridingOffset(-0.7F).clientTrackingRange(8).notInPeaceful(),
			UndeadTuning::wokenZombieAttributes),
	/** Deserts (owner, 2026-11-22): lies buried in sand and bursts out in a puff of it that briefly blinds. */
	HUSK("husk", List.of(SleeperPose.LYING, SleeperPose.SITTING), SoundType.SAND, MapColor.SAND, Items.ROTTEN_FLESH,
			() -> EntityType.Builder.<WokenHusk>of(WokenHusk::new, MobCategory.MONSTER).sized(0.6F, 1.95F).eyeHeight(1.74F)
					.passengerAttachments(2.075F).ridingOffset(-0.7F).clientTrackingRange(8).notInPeaceful(),
			UndeadTuning::wokenZombieAttributes),
	/** Shipwrecks: the ship's crew. */
	DROWNED("drowned", List.of(SleeperPose.LYING, SleeperPose.SITTING), SoundType.WET_GRASS, MapColor.WATER, Items.ROTTEN_FLESH,
			() -> EntityType.Builder.<WokenDrowned>of(WokenDrowned::new, MobCategory.MONSTER).sized(0.6F, 1.95F).eyeHeight(1.74F)
					.passengerAttachments(2.075F).ridingOffset(-0.7F).clientTrackingRange(8).notInPeaceful(),
			UndeadTuning::wokenZombieAttributes),
	/** Snow (owner, 2026-11-22): lies frozen in ice or packed snow that cracks as a warning before it breaks free. */
	STRAY("stray", List.of(SleeperPose.LYING, SleeperPose.SITTING), SoundType.BONE_BLOCK, MapColor.SNOW, Items.BONE,
			() -> EntityType.Builder.of(WokenStray::new, MobCategory.MONSTER).sized(0.6F, 1.99F).eyeHeight(1.74F).ridingOffset(-0.7F)
					.clientTrackingRange(8).notInPeaceful(),
			UndeadTuning::wokenSkeletonAttributes);

	private final String name;
	private final List<SleeperPose> poses;
	private final SoundType sound;
	private final MapColor mapColor;
	private final Item remains;
	private final Supplier<EntityType.Builder<? extends Monster>> wokenType;
	private final Supplier<AttributeSupplier.Builder> attributes;

	UndeadVariant(String name, List<SleeperPose> poses, SoundType sound, MapColor mapColor, Item remains,
			Supplier<EntityType.Builder<? extends Monster>> wokenType, Supplier<AttributeSupplier.Builder> attributes) {
		this.name = name;
		this.poses = poses;
		this.sound = sound;
		this.mapColor = mapColor;
		this.remains = remains;
		this.wokenType = wokenType;
		this.attributes = attributes;
	}

	public String variantName() {
		return name;
	}

	public String sleeperId() {
		return name + "_sleeper";
	}

	public String wokenId() {
		return "woken_" + name;
	}

	/** Poses offered in the creative tab; the first is the default pose of a placed sleeper item. */
	public List<SleeperPose> poses() {
		return poses;
	}

	/** The groan a sleeper of this kind gives when it stirs (its mob's ambient sound, played quiet and low). */
	public net.minecraft.sounds.SoundEvent stirSound() {
		return switch (this) {
			case SKELETON -> net.minecraft.sounds.SoundEvents.SKELETON_AMBIENT;
			case ZOMBIE -> net.minecraft.sounds.SoundEvents.ZOMBIE_AMBIENT;
			case HUSK -> net.minecraft.sounds.SoundEvents.HUSK_AMBIENT;
			case DROWNED -> net.minecraft.sounds.SoundEvents.DROWNED_AMBIENT_WATER;
			case STRAY -> net.minecraft.sounds.SoundEvents.STRAY_AMBIENT;
		};
	}

	/** Whether its woken mob is a skeleton (bow, arrows) rather than a zombie. */
	public boolean isSkeleton() {
		return this == SKELETON || this == STRAY;
	}

	public SoundType sound() {
		return sound;
	}

	public MapColor mapColor() {
		return mapColor;
	}

	/** What breaking a sleeper without a tool leaves behind (gear still drops). */
	public Item remains() {
		return remains;
	}

	public EntityType.Builder<? extends Monster> wokenTypeBuilder() {
		return wokenType.get();
	}

	public AttributeSupplier.Builder wokenAttributes() {
		return attributes.get();
	}
}
