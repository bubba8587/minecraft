// dte:A2,A4
package io.github.bubba8587.refmod.pseudoblock;

import java.util.UUID;

import com.mojang.math.Transformation;
import io.github.bubba8587.refmod.RefMod;
import io.github.bubba8587.refmod.registry.ModAttachments;
import io.github.bubba8587.refmod.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Brightness;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import net.fabricmc.fabric.api.event.player.UseEntityCallback;

/**
 * A block-like object made of two vanilla entities, no new block (spec requirement 17a, B15 fewNewBlocks):
 * a {@code minecraft:item_display} draws the look (a custom item model chosen with the {@code minecraft:item_model}
 * component) and a {@code minecraft:interaction} is its hitbox. Both are ordinary saved entities. Right-clicking the
 * hitbox removes both and drops the charm. Works for vanilla clients too: nothing here is a new synced registry entry
 * (the look needs the mod's resource pack, which a server can send).
 */
public final class CharmPseudoBlock {
	/** Item model definition {@code assets/refmod/items/lantern_charm_placed.json}; there is no item with this id. */
	public static final Identifier PLACED_MODEL = RefMod.id("lantern_charm_placed");
	public static final float SIZE = 0.5F;

	private CharmPseudoBlock() {
	}

	/** Places a charm on top of {@code below}. Returns the hitbox, or null if the space above is not free. */
	public static @Nullable Interaction place(ServerLevel level, BlockPos below, float yRot) {
		BlockPos at = below.above();
		if (!level.getBlockState(at).canBeReplaced()) {
			return null;
		}
		Vec3 base = Vec3.atBottomCenterOf(at);

		Display.ItemDisplay display = new Display.ItemDisplay(EntityTypes.ITEM_DISPLAY, level);
		ItemStack look = new ItemStack(ModItems.LANTERN_CHARM);
		look.set(DataComponents.ITEM_MODEL, PLACED_MODEL);
		display.setItemStack(look);
		display.setItemTransform(ItemDisplayContext.FIXED);
		// An item_display draws the model centred on its position; lift it half a block so it sits on the surface.
		display.setTransformation(new Transformation(new Vector3f(0.0F, 0.5F, 0.0F), null, null, null));
		display.setBrightnessOverride(new Brightness(15, 15)); // glows; real light would need a vanilla minecraft:light block
		// Without a width and height a display entity is never frustum-culled (Display#updateCulling).
		display.setWidth(SIZE);
		display.setHeight(SIZE);
		display.snapTo(base.x, base.y, base.z, yRot, 0.0F);

		Interaction hitbox = new Interaction(EntityTypes.INTERACTION, level);
		hitbox.setWidth(SIZE);
		hitbox.setHeight(SIZE);
		hitbox.setResponse(true); // the client swings the arm on click
		hitbox.snapTo(base.x, base.y, base.z, yRot, 0.0F);
		hitbox.setAttached(ModAttachments.CHARM_DISPLAY, display.getUUID());

		level.addFreshEntity(display);
		level.addFreshEntity(hitbox);
		return hitbox;
	}

	/** Removes a charm through its hitbox and drops the item. Returns false if {@code entity} is not a charm hitbox. */
	public static boolean pickUp(ServerLevel level, Entity entity) {
		if (!(entity instanceof Interaction hitbox) || !hitbox.hasAttached(ModAttachments.CHARM_DISPLAY)) {
			return false;
		}
		UUID displayId = hitbox.getAttached(ModAttachments.CHARM_DISPLAY);
		Entity display = level.getEntity(displayId);
		if (display != null) {
			display.discard();
		}
		hitbox.spawnAtLocation(level, new ItemStack(ModItems.LANTERN_CHARM));
		hitbox.discard();
		return true;
	}

	public static void initialize() {
		// UseEntityCallback fires from ServerGamePacketListenerImpl#handleInteract (the right-click packet), before Interaction#interact.
		UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
			if (player.isSpectator() || !(level instanceof ServerLevel serverLevel)) {
				return InteractionResult.PASS; // client: the attachment is not synced, so let vanilla send the packet
			}
			return pickUp(serverLevel, entity) ? InteractionResult.SUCCESS : InteractionResult.PASS;
		});
	}
}
