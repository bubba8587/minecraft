// dte:C6,B17
package io.github.bubba8587.bubbas_totems_artifacts.keeping;

import io.github.bubba8587.bubbas_totems_artifacts.BubbasTotemsArtifacts;
import io.github.bubba8587.bubbas_totems_artifacts.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gamerules.GameRules;

import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/**
 * The Totem of Keeping (specs/totems-artifacts.md): a Totem of Undying for your things. Carried anywhere in a player's
 * inventory, it is used up when the player dies and that death goes exactly as with vanilla's keep_inventory rule
 * (B17): the whole inventory, XP and score come back on respawn, nothing drops, curse-of-vanishing items stay.
 *
 * <p>How: at the start of the player's death drops ({@code PlayerKeepingMixin}) one totem is taken out and the dead
 * player is tagged {@link #TAG} (an entity tag, so it survives logging out on the death screen); the tag stops the
 * inventory drop and the XP orbs; on respawn the inventory, XP and score are copied over as vanilla's rule does
 * ({@link ServerPlayerEvents#COPY_FROM}), then the tag is cleared and the player sees the totem animation with the
 * Totem of Keeping in it ({@link KeptPayload}). With keep_inventory on, the totem isn't used.
 */
public final class Keeping {
	/** Entity tag on a dead player whose things a Totem of Keeping is holding. */
	public static final String TAG = BubbasTotemsArtifacts.MOD_ID + ".kept";

	private Keeping() {
	}

	public static void initialize() {
		PayloadTypeRegistry.clientboundPlay().register(KeptPayload.TYPE, KeptPayload.CODEC);
		ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
			if (!alive && isKept(oldPlayer)) {
				// What vanilla's restoreFrom does under keep_inventory (ServerPlayer.transferInventoryXpAndScore).
				newPlayer.getInventory().replaceWith(oldPlayer.getInventory());
				newPlayer.experienceLevel = oldPlayer.experienceLevel;
				newPlayer.totalExperience = oldPlayer.totalExperience;
				newPlayer.experienceProgress = oldPlayer.experienceProgress;
				newPlayer.setScore(oldPlayer.getScore());
			}
		});
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			// Vanilla copies entity tags to the new player after restoreFrom: clear it there.
			if (newPlayer.removeTag(TAG) && !alive) {
				ServerPlayNetworking.send(newPlayer, KeptPayload.INSTANCE);
				newPlayer.level().playSound(null, newPlayer.getX(), newPlayer.getY(), newPlayer.getZ(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.6F, 1.2F);
				newPlayer.sendSystemMessage(Component.translatable("message.bubbas_totems_artifacts.kept"), true);
			}
		});
	}

	public static boolean isKept(Player player) {
		return player.entityTags().contains(TAG);
	}

	/**
	 * Called as a player's death drops start. If keep_inventory is off and the player carries a Totem of Keeping, one is
	 * used up and the player is tagged; returns whether this death keeps everything.
	 */
	public static boolean tryKeep(ServerLevel level, ServerPlayer player) {
		if (isKept(player)) {
			return true;
		}
		if (level.getGameRules().get(GameRules.KEEP_INVENTORY) || player.isSpectator()) {
			return false;
		}
		Inventory inventory = player.getInventory();
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack stack = inventory.getItem(i);
			if (stack.is(ModItems.TOTEM_OF_KEEPING)) {
				stack.shrink(1);
				player.addTag(TAG);
				// Seen by everyone around: the Totem of Undying's burst at the place of death.
				level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1.0, player.getZ(), 60, 0.4, 0.6, 0.4, 0.4);
				level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0F, 1.2F);
				return true;
			}
		}
		return false;
	}

	/** Server to client, on respawn after a kept death: play the totem animation with a Totem of Keeping. */
	public record KeptPayload() implements CustomPacketPayload {
		public static final KeptPayload INSTANCE = new KeptPayload();
		public static final Type<KeptPayload> TYPE = new Type<>(BubbasTotemsArtifacts.id("kept"));
		public static final StreamCodec<RegistryFriendlyByteBuf, KeptPayload> CODEC = StreamCodec.unit(INSTANCE);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}

		public static ItemStack shown() {
			return new ItemStack(ModItems.TOTEM_OF_KEEPING);
		}
	}
}
