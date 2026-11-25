// dte:C4
package io.github.bubba8587.bubbas_infused_gems.station;

import java.util.List;
import java.util.Optional;

import io.github.bubba8587.bubbas_infused_gems.balance.Balance;
import io.github.bubba8587.bubbas_infused_gems.gem.Gem;
import io.github.bubba8587.bubbas_infused_gems.gem.GemText;
import io.github.bubba8587.bubbas_infused_gems.registry.ModComponents;
import io.github.bubba8587.bubbas_infused_gems.registry.ModItems;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/** Gem inversion: a finished gem with a drawback + a fermented spider eye at the brewing. */
public final class GemInversion {
	private static final List<String> ORDER = List.of("weak", "medium", "strongest");

	private GemInversion() {
	}

	/** The grade a main strength was rolled at on this Infusion level. */
	public static @Nullable String mainGrade(Gem gem) {
		Balance b = Balance.get();
		double x = gem.main() / b.infusion(gem.infusion()).factor();
		String best = null;
		for (String g : ORDER) {
			double v = x / b.grades.get(g);
			if (v >= b.variantMin - 1e-4 && v <= 1.0 + 1e-4) {
				best = g;
			}
		}
		return best;
	}

	public static boolean canInvert(@Nullable Gem gem) {
		if (gem == null || gem.drawback().isEmpty() || gem.infusion() <= 1 || !Balance.get().gemEffects.contains(gem.drawback().get().key())) {
			return false;
		}
		if (!gem.hasMain()) {
			return true;
		}
		String grade = mainGrade(gem);
		return grade != null && !grade.equals("weak") && Balance.get().gemEffects.contains(gem.drawback().get().key());
	}

	public static boolean canInvert(ItemStack stack) {
		return stack.is(ModItems.GEM) && canInvert(stack.get(ModComponents.GEM));
	}

	public static Gem invert(Gem gem) {
		Balance b = Balance.get();
		if (!gem.hasMain()) {
			int infusion = gem.infusion() - 1;
			float main = (float) (b.infusion(infusion).factor() * b.grades.get("weak"));
			return new Gem(gem.drawback().get().effect(), infusion, gem.tier(), main, gem.other(), Optional.empty(), 3, 1);
		}
		String grade = mainGrade(gem);
		String lower = ORDER.get(ORDER.indexOf(grade) - 1);
		int infusion = gem.infusion() - 1;
		float main = (float) (gem.main() * b.grades.get(lower) / b.grades.get(grade) * b.infusion(infusion).factor() / b.infusion(gem.infusion()).factor());
		Gem.Roll old = gem.drawback().get();
		return new Gem(old.effect(), infusion, gem.tier(), main, gem.other(), Optional.of(new Gem.Roll(gem.effect(), old.strength())),
				gem.infusionLevel(), Gem.gradeLevel(lower));
	}

	public static ItemStack invert(ItemStack stack) {
		ItemStack out = new ItemStack(ModItems.GEM);
		out.set(ModComponents.GEM, invert(stack.get(ModComponents.GEM)));
		return out;
	}

	public static String describe(Gem gem) {
		return GemText.key(gem.effect());
	}
}
