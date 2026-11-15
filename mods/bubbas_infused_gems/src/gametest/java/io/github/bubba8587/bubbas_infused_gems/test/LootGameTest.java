// dte:C4
package io.github.bubba8587.bubbas_infused_gems.test;

import static io.github.bubba8587.bubbas_infused_gems.test.GemKit.check;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.github.bubba8587.bubbas_infused_gems.balance.Balance;
import io.github.bubba8587.bubbas_infused_gems.gem.Gem;
import io.github.bubba8587.bubbas_infused_gems.loot.GemLoot;
import io.github.bubba8587.bubbas_infused_gems.registry.ModComponents;
import io.github.bubba8587.bubbas_infused_gems.station.Sockets;
import io.github.bubba8587.bubbas_infused_gems.test.mixin.MobEquipmentInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.util.context.ContextKey;
import net.minecraft.util.context.ContextKeySet;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;

/**
 * Gems as loot (owner): vanilla tables keep their loot and get one extra roll of gems or gemmed armor; the found-only
 * gems come from their places; mobs spawn with gemmed armor at the model's rates and get its effects; the pieces drop
 * with their gems. Tables are rolled many times with fixed seeds, so the counts are the same every run.
 */
public class LootGameTest {
	private static LootTable table(GameTestHelper helper, String id) {
		return helper.getLevel().getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, Identifier.parse(id)));
	}

	/** Loot params for a table's own context type, from a few stand-ins. */
	private static LootParams params(GameTestHelper helper, LootTable table, Entity self, ServerPlayer killer) {
		ContextKeySet set = table.getParamSet();
		LootParams.Builder b = new LootParams.Builder(helper.getLevel());
		for (ContextKey<?> key : set.allowed()) {
			boolean required = set.required().contains(key);
			if (key == LootContextParams.ORIGIN) {
				b.withParameter(LootContextParams.ORIGIN, net.minecraft.world.phys.Vec3.atCenterOf(helper.absolutePos(new BlockPos(1, 1, 1))));
			} else if (key == LootContextParams.THIS_ENTITY && self != null) {
				b.withParameter(LootContextParams.THIS_ENTITY, self);
			} else if (key == LootContextParams.TOOL) {
				b.withParameter(LootContextParams.TOOL, new ItemStack(Items.FISHING_ROD));
			} else if (key == LootContextParams.DAMAGE_SOURCE) {
				b.withParameter(LootContextParams.DAMAGE_SOURCE, killer != null ? helper.getLevel().damageSources().playerAttack(killer)
						: helper.getLevel().damageSources().generic());
			} else if (key == LootContextParams.LAST_DAMAGE_PLAYER && killer != null) {
				b.withParameter(LootContextParams.LAST_DAMAGE_PLAYER, killer);
			} else if (key == LootContextParams.ATTACKING_ENTITY && killer != null) {
				b.withParameter(LootContextParams.ATTACKING_ENTITY, killer);
			} else if (required) {
				throw new IllegalStateException("test can't fill " + key);
			}
		}
		return b.create(set);
	}

	private static List<ItemStack> roll(GameTestHelper helper, String id, int times, Entity self, ServerPlayer killer) {
		LootTable t = table(helper, id);
		LootParams p = params(helper, t, self, killer);
		List<ItemStack> out = new ArrayList<>();
		for (int seed = 1; seed <= times; seed++) {
			out.addAll(t.getRandomItems(p, (long) seed * 7919L));
		}
		return out;
	}

	/** Every gem in these items: loose gems and gems in gear. */
	private static List<Gem> gems(List<ItemStack> items) {
		List<Gem> out = new ArrayList<>();
		for (ItemStack s : items) {
			Gem g = s.get(ModComponents.GEM);
			if (g != null) {
				out.add(g);
			}
			out.addAll(Sockets.list(s));
		}
		return out;
	}

	private static Set<String> keys(List<Gem> gems) {
		Set<String> out = new HashSet<>();
		gems.forEach(g -> out.add(g.key()));
		return out;
	}

	private static Entity standIn(GameTestHelper helper, String table) {
		if (table.contains("elder_guardian")) {
			return EntityTypes.ELDER_GUARDIAN.create(helper.getLevel(), EntitySpawnReason.COMMAND);
		}
		if (table.contains("hero_of_the_village")) {
			return EntityTypes.VILLAGER.create(helper.getLevel(), EntitySpawnReason.COMMAND);
		}
		return null;
	}

	/** Every listed vanilla table still rolls its own loot and now sometimes adds a gem, at about the model's chance. */
	@GameTest(maxTicks = 400)
	public void everyListedTableSometimesHoldsGems(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		Map<String, Double> chance = new HashMap<>();
		for (JsonElement e : placeList()) {
			chance.put(e.getAsJsonObject().get("table").getAsString(), e.getAsJsonObject().get("chance").getAsDouble());
		}
		check(helper, GemLoot.injected().size() == chance.size() && chance.size() >= 40, "listed tables " + GemLoot.injected().size());
		int n = 400;
		for (Identifier vanilla : GemLoot.injected().keySet()) {
			String id = vanilla.toString();
			LootTable t = table(helper, id);
			LootParams p = params(helper, t, standIn(helper, id), player);
			int withGems = 0;
			int vanillaItems = 0;
			for (int seed = 1; seed <= n; seed++) {
				List<ItemStack> items = t.getRandomItems(p, (long) seed * 7919L);
				if (!gems(items).isEmpty()) {
					withGems++;
				}
				vanillaItems += (int) items.stream().filter(s -> !s.has(ModComponents.GEM) && !s.has(ModComponents.SOCKETED_GEMS)).count();
			}
			double expected = chance.get(vanilla.getPath()) * n;
			check(helper, withGems > expected * 0.4 && withGems < expected * 1.8 + 3, id + ": gems in " + withGems + " of " + n + " rolls, expected about " + expected);
			check(helper, vanillaItems > 0 || id.contains("elder_guardian"), id + ": vanilla loot gone");
		}
		GemKit.remove(helper, player);
		helper.succeed();
	}

	private static Iterable<JsonElement> placeList() {
		List<JsonElement> out = new ArrayList<>();
		JsonObject inject = Balance.get().raw.getAsJsonObject("loot_inject");
		JsonObject chances = Balance.get().raw.getAsJsonObject("loot_chance");
		for (String k : inject.keySet()) {
			JsonObject o = new JsonObject();
			o.addProperty("table", Identifier.parse(k).getPath());
			o.addProperty("chance", chances.get(k).getAsDouble());
			out.add(o);
		}
		return out;
	}

	/** Each level's gems: within its Infusion and tier lists, craftable gems only and never Infusion VI, gear with 1+ gems. */
	@GameTest(maxTicks = 200)
	public void levelTablesMakeWeakRareGems(GameTestHelper helper) {
		Balance b = Balance.get();
		JsonObject loot = b.raw.getAsJsonObject("loot");
		int previousBest = 0;
		for (String level : loot.keySet()) {
			List<ItemStack> items = roll(helper, "bubbas_infused_gems:gems/" + level, 600, null, null);
			List<Gem> gems = gems(items);
			check(helper, items.size() == 600, level + ": " + items.size() + " items from 600 rolls");
			int gear = 0;
			for (ItemStack s : items) {
				if (s.has(ModComponents.SOCKETED_GEMS)) {
					gear++;
					check(helper, Sockets.list(s).size() >= 1 && Sockets.list(s).size() <= Sockets.slots(s), level + ": gear with " + Sockets.list(s).size() + " gems");
				}
			}
			check(helper, gear > 80 && gear < 230, level + ": " + gear + " gemmed pieces of 600 (model: a quarter)");
			Set<Integer> infusions = new HashSet<>();
			loot.getAsJsonObject(level).getAsJsonArray("bases").forEach(x -> b.infusions.stream()
					.filter(i -> i.base().equals(x.getAsString())).forEach(i -> infusions.add(i.level())));
			Set<Integer> tiers = new HashSet<>();
			loot.getAsJsonObject(level).getAsJsonArray("tiers").forEach(x -> tiers.add(x.getAsInt()));
			int best = 0;
			for (Gem g : gems) {
				check(helper, !"found".equals(b.effect(g.key()).source()), level + ": found-only " + g.key() + " in the general pool");
				check(helper, infusions.contains(g.infusion()) && g.infusion() < 6, level + ": Infusion " + g.infusion());
				check(helper, tiers.contains(g.tier()), level + ": tier " + g.tier());
				check(helper, g.main() < 1.0F, level + ": a perfect craftable gem from loot");
				best = Math.max(best, Math.round(g.main() * 100));
			}
			check(helper, keys(gems).size() >= 20, level + ": only " + keys(gems).size() + " gem effects");
			check(helper, best > previousBest, level + ": best gem " + best + "% not above the level below (" + previousBest + "%)");
			previousBest = best;
		}
		// Fixed seed, same loot.
		LootTable t = table(helper, "bubbas_infused_gems:gems/common");
		LootParams p = params(helper, t, null, null);
		check(helper, ItemStack.matches(t.getRandomItems(p, 42L).getFirst(), t.getRandomItems(p, 42L).getFirst()), "same seed, different loot");
		helper.succeed();
	}

	/** Found-only gems (Haste, Luck, Health Boost, Hero of the Village, Mining Fatigue, Resistance) come from their places. */
	@GameTest(maxTicks = 400)
	public void foundOnlyGemsHaveSources(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		Set<String> ominous = keys(gems(roll(helper, "minecraft:chests/trial_chambers/reward_ominous", 1500, null, player)));
		for (String k : List.of("haste", "luck", "health_boost", "hero_of_the_village", "mining_fatigue", "resistance")) {
			check(helper, ominous.contains(k), "ominous vaults never gave " + k);
		}
		// Only ominous vaults reach a perfect found-only gem.
		List<Gem> ancient = gems(roll(helper, "minecraft:chests/ancient_city", 800, null, player));
		check(helper, keys(ancient).contains("haste") && keys(ancient).contains("health_boost"), "ancient city found-only gems: " + keys(ancient));
		check(helper, ancient.stream().noneMatch(g -> g.main() >= 1.0F), "a perfect gem from an ancient city");
		check(helper, keys(gems(roll(helper, "minecraft:chests/abandoned_mineshaft", 1500, null, player))).contains("haste"), "no Haste from mineshafts");
		check(helper, keys(gems(roll(helper, "minecraft:gameplay/fishing/treasure", 1500, null, player))).contains("luck"), "no Luck from fishing treasure");
		check(helper, keys(gems(roll(helper, "minecraft:chests/woodland_mansion", 800, null, player))).contains("resistance"), "no Resistance from mansions");
		Entity villager = standIn(helper, "hero_of_the_village");
		check(helper, keys(gems(roll(helper, "minecraft:gameplay/hero_of_the_village/farmer_gift", 1500, villager, player))).contains("hero_of_the_village"),
				"no Hero of the Village from hero gifts");
		// Elder guardian: Mining Fatigue only, and only when a player killed it.
		Entity elder = standIn(helper, "elder_guardian");
		List<Gem> byPlayer = gems(roll(helper, "minecraft:entities/elder_guardian", 300, elder, player));
		check(helper, !byPlayer.isEmpty() && keys(byPlayer).equals(Set.of("mining_fatigue")), "elder guardian gems: " + keys(byPlayer));
		check(helper, gems(roll(helper, "minecraft:entities/elder_guardian", 300, elder, null)).isEmpty(), "elder guardian gave a gem without a player kill");
		GemKit.remove(helper, player);
		helper.succeed();
	}

	private static int gemmedPieces(Mob mob) {
		int n = 0;
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR && Sockets.hasGems(mob.getItemBySlot(slot))) {
				n++;
			}
		}
		return n;
	}

	private static int pieces(Mob mob) {
		int n = 0;
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR && !mob.getItemBySlot(slot).isEmpty()) {
				n++;
			}
		}
		return n;
	}

	/** Spawn armor (vanilla's choice) gets gems at the model's chance per piece: most mobs 4 %, piglins 6 %. */
	@GameTest(maxTicks = 200)
	public void spawnArmorSometimesHasGems(GameTestHelper helper) {
		DifficultyInstance hard = new DifficultyInstance(Difficulty.HARD, 3_600_000L, 3_600_000L, 1.0F);
		Map<String, Double> rates = Map.of("zombie", 0.04, "piglin", 0.06);
		for (String who : rates.keySet()) {
			Mob mob = who.equals("zombie") ? helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(1, 1, 1))
					: helper.spawnWithNoFreeWill(EntityTypes.PIGLIN, new BlockPos(2, 1, 1));
			int total = 0;
			int gemmed = 0;
			for (int i = 0; i < 6000 && total < 3000; i++) {
				for (EquipmentSlot slot : EquipmentSlot.values()) {
					mob.setItemSlot(slot, ItemStack.EMPTY);
				}
				((MobEquipmentInvoker) mob).bubbas$populate(RandomSource.create(i), hard);
				total += pieces(mob);
				gemmed += gemmedPieces(mob);
				if (who.equals("piglin")) {
					for (EquipmentSlot slot : EquipmentSlot.values()) {
						for (Gem g : Sockets.list(mob.getItemBySlot(slot))) {
							check(helper, g.infusion() <= 3, "piglin gem above the rare level: Infusion " + g.infusion());
						}
					}
				}
			}
			double rate = gemmed / (double) Math.max(total, 1);
			check(helper, total > 500 && rate > rates.get(who) * 0.5 && rate < rates.get(who) * 1.6,
					who + ": " + gemmed + " gemmed of " + total + " pieces (" + rate + "), model " + rates.get(who));
			mob.discard();
		}
		// Ominous trial spawner mobs: vanilla's trial chamber equipment table gets gems too (model 10 % per piece).
		Zombie wearer = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(3, 1, 1));
		List<ItemStack> trial = roll(helper, "minecraft:equipment/trial_chamber", 800, wearer, null);
		long gemmedTrial = trial.stream().filter(Sockets::hasGems).count();
		check(helper, !trial.isEmpty() && gemmedTrial > trial.size() * 0.05 && gemmedTrial < trial.size() * 0.16,
				"trial chamber equipment: " + gemmedTrial + " gemmed of " + trial.size());
		check(helper, trial.stream().filter(s -> !s.has(ModComponents.SOCKETED_GEMS)).anyMatch(s -> s.has(net.minecraft.core.component.DataComponents.TRIM)),
				"trial chamber armor lost its vanilla trims");
		helper.succeed();
	}

	/** A mob in gemmed armor gets the gems' effects (Speed, Glowing; Regeneration heals a piglin, not an undead skeleton). */
	@GameTest(maxTicks = 200)
	public void mobsInGemmedArmorGetTheirEffects(GameTestHelper helper) {
		Zombie zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(1, 1, 1));
		double baseSpeed = zombie.getAttributeValue(Attributes.MOVEMENT_SPEED);
		zombie.setItemSlot(EquipmentSlot.CHEST, GemKit.socketed(Items.IRON_CHESTPLATE, GemKit.gem("speed", 0.6), GemKit.gem("glowing", 0.4)));
		Piglin piglin = helper.spawnWithNoFreeWill(EntityTypes.PIGLIN, new BlockPos(3, 1, 1));
		piglin.setImmuneToZombification(true);
		piglin.setItemSlot(EquipmentSlot.HEAD, GemKit.socketed(Items.GOLDEN_HELMET, GemKit.gem("regeneration", 0.8)));
		Skeleton skeleton = helper.spawnWithNoFreeWill(EntityTypes.SKELETON, new BlockPos(1, 1, 3));
		skeleton.setItemSlot(EquipmentSlot.HEAD, GemKit.socketed(Items.IRON_HELMET, GemKit.gem("regeneration", 0.8)));
		helper.runAfterDelay(2, () -> {
			piglin.setHealth(8.0F);
			skeleton.setHealth(8.0F);
		});
		helper.succeedWhen(() -> {
			check(helper, zombie.getAttributeValue(Attributes.MOVEMENT_SPEED) > baseSpeed * 1.01, "zombie not faster: " + zombie.getAttributeValue(Attributes.MOVEMENT_SPEED));
			check(helper, zombie.hasEffect(MobEffects.GLOWING), "zombie not glowing");
			check(helper, piglin.getHealth() > 8.2F, "piglin not regenerating: " + piglin.getHealth());
			check(helper, skeleton.getHealth() <= 8.0F, "undead skeleton regenerated: " + skeleton.getHealth());
			check(helper, helper.getTick() > 60, "waiting");
		});
	}

	/** Gemmed mob armor drops with vanilla's drop chance and keeps its gems. */
	@GameTest(maxTicks = 100)
	public void droppedMobArmorKeepsItsGems(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		Zombie zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(1, 1, 1));
		zombie.setItemSlot(EquipmentSlot.HEAD, GemKit.socketed(Items.IRON_HELMET, GemKit.gem("speed", 0.3)));
		zombie.setDropChance(EquipmentSlot.HEAD, 1.0F);
		zombie.hurtServer(helper.getLevel(), helper.getLevel().damageSources().playerAttack(player), 1000.0F);
		helper.runAfterDelay(2, () -> {
			AABB box = new AABB(helper.absolutePos(new BlockPos(1, 1, 1))).inflate(3);
			List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, box, e -> e.getItem().is(Items.IRON_HELMET));
			check(helper, drops.size() == 1 && Sockets.list(drops.getFirst().getItem()).size() == 1
					&& Sockets.list(drops.getFirst().getItem()).getFirst().key().equals("speed"), "helmet dropped without its gem: " + drops);
			GemKit.remove(helper, player);
			helper.succeed();
		});
	}
}
