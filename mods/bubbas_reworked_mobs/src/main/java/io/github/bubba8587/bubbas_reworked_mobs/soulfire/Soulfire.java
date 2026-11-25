// dte:C1,B15,B17
package io.github.bubba8587.bubbas_reworked_mobs.soulfire;

import io.github.bubba8587.bubbas_reworked_mobs.BubbasReworkedMobs;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.EnchantRandomlyFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProvider;

import com.mojang.serialization.Codec;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableSource;

/** Soulfire burning, Soulguard and the soul blaze items (specs/soulfire.md): registrations and loot. */
public final class Soulfire {
	// --- damage types (data/bubbas_reworked_mobs/damage_type) and the totem tag ---
	public static final ResourceKey<DamageType> SOUL_FIRE = damageType("soul_fire");
	public static final ResourceKey<DamageType> SOUL_CAMPFIRE = damageType("soul_campfire");
	public static final ResourceKey<DamageType> SOUL_BURN = damageType("soul_burn");
	/** The soul part alone (not fire, so Fire Resistance doesn't stop it). */
	public static final ResourceKey<DamageType> SOUL_SEAR = damageType("soul_sear");
	/** Damage that a Totem of Undying or Keeping doesn't save from, unless the victim has Soulguard. */
	public static final TagKey<DamageType> BYPASSES_TOTEMS = TagKey.create(Registries.DAMAGE_TYPE, BubbasReworkedMobs.id("bypasses_totems"));

	// --- Soulguard ---
	public static final ResourceKey<Enchantment> SOULGUARD_ENCHANTMENT = ResourceKey.create(Registries.ENCHANTMENT, BubbasReworkedMobs.id("soulguard"));
	public static final Holder<MobEffect> SOULGUARD = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, BubbasReworkedMobs.id("soulguard"),
			new MobEffect(MobEffectCategory.BENEFICIAL, SoulfireTuning.COLOR) {
			});
	public static final Holder<Potion> SOULGUARD_POTION = potion("soulguard", SoulfireTuning.POTION_TICKS);
	public static final Holder<Potion> LONG_SOULGUARD_POTION = potion("long_soulguard", SoulfireTuning.LONG_POTION_TICKS);

	// --- soul blaze items ---
	public static final Item SOUL_BLAZE_ROD = item("soul_blaze_rod", Item::new,
			new Item.Properties().cookingFuel(provider("cooking/time_soul_blaze_rod")));
	public static final Item SOUL_BLAZE_POWDER = item("soul_blaze_powder", Item::new,
			new Item.Properties().brewingFuel(provider("brewing/uses_soul_blaze_powder")));
	public static final Item SOUL_FIRE_CHARGE = item("soul_fire_charge", SoulFireChargeItem::new,
			new Item.Properties().useCooldown(SoulfireTuning.THROW_COOLDOWN_SECONDS));

	/** Set while an entity burns with Soulfire (saved; synced to everyone tracking it, for the blue flames). */
	public static final AttachmentType<Boolean> SOUL_BURNING = AttachmentRegistry.create(BubbasReworkedMobs.id("soul_burning"),
			b -> b.persistent(Codec.BOOL).syncWith(ByteBufCodecs.BOOL, AttachmentSyncPredicate.all()));

	private Soulfire() {
	}

	private static ResourceKey<DamageType> damageType(String name) {
		return ResourceKey.create(Registries.DAMAGE_TYPE, BubbasReworkedMobs.id(name));
	}

	private static ResourceKey<ContextIntProvider> provider(String path) {
		return ResourceKey.create(Registries.CONTEXT_INT_PROVIDER, BubbasReworkedMobs.id(path));
	}

	private static Holder<Potion> potion(String name, int ticks) {
		return Registry.registerForHolder(BuiltInRegistries.POTION, BubbasReworkedMobs.id(name),
				new Potion(BubbasReworkedMobs.MOD_ID + ".soulguard", new MobEffectInstance(SOULGUARD, ticks)));
	}

	private static Item item(String name, java.util.function.Function<Item.Properties, Item> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, BubbasReworkedMobs.id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
	}

	public static void initialize() {
		DispenserBlock.registerProjectileBehavior(SOUL_FIRE_CHARGE);
		LootTableEvents.MODIFY.register((key, builder, source, registries) -> {
			if (source == LootTableSource.REPLACED || !SoulfireTuning.BOOK_CHESTS.contains(key.identifier())) {
				return;
			}
			registries.lookupOrThrow(Registries.ENCHANTMENT).get(SOULGUARD_ENCHANTMENT).ifPresent(soulguard -> builder.withPool(LootPool.lootPool()
					.add(LootItem.lootTableItem(Items.BOOK).apply(EnchantRandomlyFunction.randomEnchantment().withEnchantment(soulguard)))
					.when(LootItemRandomChanceCondition.randomChance(SoulfireTuning.BOOK_CHANCE))));
		});
		SoulBurning.initialize();
		io.github.bubba8587.bubbas_reworked_mobs.soulfire.phasing.Phasing.initialize();
	}

	/** A loot table key of one of the chests that can hold a Soulguard book. */
	public static boolean holdsBooks(ResourceKey<LootTable> key) {
		return SoulfireTuning.BOOK_CHESTS.contains(key.identifier());
	}
}
