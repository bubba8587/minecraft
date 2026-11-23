// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.grave;

import net.minecraft.util.RandomSource;

/** The kinds a crypt yard comes in (specs/graveyards.md): its size, its state of breakdown and its crypt's wealth. */
public final class YardKinds {
	private YardKinds() {
	}

	/** How big the yard is: from a lone family plot to a large yard. */
	public enum Size {
		/** A lone family plot: 7-9 square, 2-3 graves, no crypt. */
		LONE(7, 9, 0.0F),
		SMALL(11, 13, 0.5F),
		MEDIUM(15, 19, 0.6F),
		LARGE(21, 27, 0.8F);

		final int min;
		final int max;
		/** The chance it has a crypt. */
		final float cryptChance;

		Size(int min, int max, float cryptChance) {
			this.min = min;
			this.max = max;
			this.cryptChance = cryptChance;
		}

		/** The largest yard of this size (previews and the build command). */
		public int maxSize() {
			return max;
		}

		static Size pick(RandomSource random) {
			float r = random.nextFloat();
			return r < 0.2F ? LONE : r < 0.5F ? SMALL : r < 0.85F ? MEDIUM : LARGE;
		}

		/** The crypt's wealth for a yard of this size: poor yards have ossuaries, large ones noble crypts. */
		Wealth wealth(RandomSource random) {
			float r = random.nextFloat();
			return switch (this) {
				case LONE, SMALL -> r < 0.5F ? Wealth.PAUPER : Wealth.FAMILY;
				case MEDIUM -> r < 0.2F ? Wealth.PAUPER : r < 0.8F ? Wealth.FAMILY : Wealth.NOBLE;
				case LARGE -> r < 0.4F ? Wealth.FAMILY : Wealth.NOBLE;
			};
		}
	}

	/**
	 * A yard's state of breakdown (owner: kept, overgrown, sunken and tilted, collapsed, nearly gone), as the numbers
	 * that build it.
	 */
	public enum State {
		//              bars gone, one high, mossy posts, broken posts, posts gone, askew, fallen, open graves, stones gone, flowers
		KEPT(0.0F, 0.0F, 0.1F, 0.0F, 0.0F, 0.1F, 0.0F, 0.0F, 0.0F, 0.35F),
		OVERGROWN(0.12F, 0.18F, 0.35F, 0.0F, 0.0F, 0.25F, 0.1F, 0.0F, 0.0F, 0.15F),
		SUNKEN(0.2F, 0.3F, 0.4F, 0.1F, 0.0F, 0.5F, 0.12F, 0.0F, 0.0F, 0.1F),
		COLLAPSED(0.45F, 0.35F, 0.3F, 0.4F, 0.1F, 0.3F, 0.35F, 0.3F, 0.1F, 0.05F),
		NEARLY_GONE(0.85F, 0.5F, 0.5F, 0.5F, 0.5F, 0.4F, 0.3F, 0.0F, 0.6F, 0.0F);

		final float barsGone;
		final float oneHigh;
		final float mossyPosts;
		final float brokenPosts;
		final float postsGone;
		final float askew;
		final float fallen;
		final float openGraves;
		final float stonesGone;
		final float flowers;

		State(float barsGone, float oneHigh, float mossyPosts, float brokenPosts, float postsGone, float askew, float fallen, float openGraves,
				float stonesGone, float flowers) {
			this.barsGone = barsGone;
			this.oneHigh = oneHigh;
			this.mossyPosts = mossyPosts;
			this.brokenPosts = brokenPosts;
			this.postsGone = postsGone;
			this.askew = askew;
			this.fallen = fallen;
			this.openGraves = openGraves;
			this.stonesGone = stonesGone;
			this.flowers = flowers;
		}

		static State pick(RandomSource random) {
			float r = random.nextFloat();
			return r < 0.15F ? KEPT : r < 0.45F ? OVERGROWN : r < 0.65F ? SUNKEN : r < 0.85F ? COLLAPSED : NEARLY_GONE;
		}
	}

	/** A crypt's wealth (specs/graveyards.md): each with its own layout, sleepers and loot table. */
	public enum Wealth {
		/** A cramped dug tunnel lined with niches: bone, cobble and dirt, many sleepers with little gear, poor loot. */
		PAUPER,
		/** Stone brick chambers with coffin niches, candles, modest loot (the first crypt). */
		FAMILY,
		/** Polished and chiseled stone: an entry hall, a sarcophagus chamber, side halls of armoured sleepers, the best loot. */
		NOBLE
	}
}
