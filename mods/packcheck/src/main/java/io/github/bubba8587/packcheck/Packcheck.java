// dte:A4
package io.github.bubba8587.packcheck;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Internal test harness. The build copies each datapack in the repo's datapacks/ folder to
 * {@code datapacks/<name>/} in this mod's resources; here each one becomes an always-enabled built-in
 * data pack (id {@code packcheck:<name>}), so dev worlds and game tests load it. specs/pack-testing.md.
 */
public class Packcheck implements ModInitializer {
	public static final String MOD_ID = "packcheck";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	/** Folder inside the mod's resources that holds the bundled datapacks. */
	public static final String DATAPACKS_DIR = "datapacks";
	/** Folder inside the mod's resources that holds the bundled resource packs. */
	public static final String RESOURCEPACKS_DIR = "resourcepacks";

	private static List<String> bundled = List.of();

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	/** Names (folder names under datapacks/) of the packs registered at startup. */
	public static List<String> bundledPacks() {
		return bundled;
	}

	@Override
	public void onInitialize() {
		ModContainer mod = FabricLoader.getInstance().getModContainer(MOD_ID).orElseThrow();
		Path root = mod.findPath(DATAPACKS_DIR).orElse(null);
		if (root == null) {
			LOGGER.warn("No bundled datapacks found (run the bundleDatapacks task)");
			return;
		}
		List<String> names;
		try (Stream<Path> dirs = Files.list(root)) {
			names = dirs.filter(Files::isDirectory)
					.map(p -> p.getFileName().toString().replace("/", ""))
					.sorted().toList();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		for (String name : names) {
			boolean ok = ResourceLoader.registerBuiltinPack(id(name), DATAPACKS_DIR + "/" + name, mod,
					Component.literal("Pack Check: " + name), PackActivationType.ALWAYS_ENABLED);
			if (!ok) {
				throw new IllegalStateException("Could not register bundled datapack " + name + " (no data/ namespaces?)");
			}
		}
		bundled = names;
		LOGGER.info("Registered {} bundled datapack(s): {}", names.size(), names);

		// Resource packs from the repo's resourcepacks/ folder, for client game tests.
		mod.findPath(RESOURCEPACKS_DIR).ifPresent(rpRoot -> {
			try (Stream<Path> dirs = Files.list(rpRoot)) {
				for (String name : dirs.filter(Files::isDirectory).map(p -> p.getFileName().toString().replace("/", "")).sorted().toList()) {
					boolean ok = ResourceLoader.registerBuiltinPack(id("rp_" + name), RESOURCEPACKS_DIR + "/" + name, mod,
							Component.literal("Pack Check RP: " + name), PackActivationType.ALWAYS_ENABLED);
					LOGGER.info("Bundled resource pack {}: {}", name, ok ? "registered" : "not registered");
				}
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		});
	}
}
