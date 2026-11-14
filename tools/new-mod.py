#!/usr/bin/env python3
# dte:B4,B6
"""Create a new Fabric mod under mods/<modid>/, wired into the shared build with game tests.

  python3 tools/new-mod.py <modid> "<Display Name>" [--description "..."]

modid: lowercase letters, digits and underscores, 2-64 chars, starting with a letter.
Then: ./gradlew :<modid>:build
"""
import json, re, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
AUTHOR = "bubba8587"            # until the inbox item `namespace` is answered
GROUP = "io.github.bubba8587"   # until the inbox item `namespace` is answered
LICENSE = "All-Rights-Reserved"  # until the inbox item `license` is answered


def camel(modid):
    return "".join(p.capitalize() for p in modid.split("_"))


def write(path, text):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8")


def main(argv):
    if len(argv) < 2:
        sys.exit(__doc__)
    modid, name = argv[0], argv[1]
    desc = argv[argv.index("--description") + 1] if "--description" in argv else ""
    if not re.fullmatch(r"[a-z][a-z0-9_]{1,63}", modid):
        sys.exit("modid must be lowercase letters, digits, underscores; start with a letter; 2-64 chars")
    d = ROOT / "mods" / modid
    if d.exists():
        sys.exit(f"{d} already exists")
    pkg = f"{GROUP}.{modid}"
    pdir = pkg.replace(".", "/")
    cls = camel(modid)

    write(d / "build.gradle", "plugins {\n\tid 'net.fabricmc.fabric-loom'\n}\napply from: rootProject.file('gradle/fabric-mod.gradle')\n\ndependencies {\n\t// Extra mod dependencies go here; see docs/knowledge/libraries.md.\n}\n")
    write(d / "gradle.properties", f"mod_id={modid}\nmod_version=0.1.0\nmaven_group={GROUP}\n")

    fmj = {
        "schemaVersion": 1, "id": "${mod_id}", "version": "${version}", "name": name,
        "description": desc, "authors": [AUTHOR], "license": LICENSE,
        "icon": f"assets/{modid}/icon.png", "environment": "*",
        "entrypoints": {"main": [f"{pkg}.{cls}"], "client": [f"{pkg}.client.{cls}Client"]},
        "mixins": [f"{modid}.mixins.json", {"config": f"{modid}.client.mixins.json", "environment": "client"}],
        "depends": {"fabricloader": ">=${loader_version}", "minecraft": "~${minecraft_version}",
                    "java": ">=${java_version}", "fabric-api": "*"},
    }
    write(d / "src/main/resources/fabric.mod.json", json.dumps(fmj, indent="\t") + "\n")
    for env, src in (("", "main"), (".client", "client")):
        mixin = {"required": True, "package": f"{pkg}{env}.mixin", "compatibilityLevel": "JAVA_25",
                 "mixins" if not env else "client": [], "injectors": {"defaultRequire": 1}}
        write(d / f"src/{src}/resources/{modid}{env}.mixins.json", json.dumps(mixin, indent="\t") + "\n")
    write(d / f"src/main/resources/assets/{modid}/lang/en_us.json", "{\n}\n")

    write(d / f"src/main/java/{pdir}/{cls}.java", f"""package {pkg};

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class {cls} implements ModInitializer {{
	public static final String MOD_ID = "{modid}";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Identifier id(String path) {{
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}}

	@Override
	public void onInitialize() {{
	}}
}}
""")
    write(d / f"src/client/java/{pdir}/client/{cls}Client.java", f"""package {pkg}.client;

import net.fabricmc.api.ClientModInitializer;

public class {cls}Client implements ClientModInitializer {{
	@Override
	public void onInitializeClient() {{
	}}
}}
""")
    tpkg = f"{pkg}.test"
    tfmj = {"schemaVersion": 1, "id": f"{modid}-test", "version": "1.0.0", "name": f"{name} Tests",
            "environment": "*", "entrypoints": {
                "fabric-gametest": [f"{tpkg}.{cls}GameTest"],
                "fabric-client-gametest": [f"{tpkg}.{cls}ClientGameTest"]}}
    write(d / "src/gametest/resources/fabric.mod.json", json.dumps(tfmj, indent="\t") + "\n")
    write(d / f"src/gametest/java/{pdir}/test/{cls}GameTest.java", f"""package {tpkg};

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

// Server game tests: run by ./gradlew :{modid}:build. Replace this with tests of the mod's behavior.
public class {cls}GameTest {{
	@GameTest
	public void worldWorks(GameTestHelper helper) {{
		helper.setBlock(1, 1, 1, Blocks.STONE);
		helper.assertBlockPresent(Blocks.STONE, 1, 1, 1);
		helper.succeed();
	}}
}}
""")
    write(d / f"src/gametest/java/{pdir}/test/{cls}ClientGameTest.java", f"""package {tpkg};

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

// Client game test: ./gradlew :{modid}:runClientGameTest. Screenshots go to build/run/clientGameTest/screenshots/.
@SuppressWarnings("UnstableApiUsage")
public class {cls}ClientGameTest implements FabricClientGameTest {{
	@Override
	public void runTest(ClientGameTestContext context) {{
		try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {{
			singleplayer.getConnection().waitForChunksRender();
			context.takeScreenshot("{modid}-world");
		}}
	}}
}}
""")
    print(f"created mods/{modid}/ ({pkg})")
    print(f"next: add assets/{modid}/icon.png (made by a person, not AI), then ./gradlew :{modid}:build")


if __name__ == "__main__":
    main(sys.argv[1:])
