# Installing Fabric Mods

Mods from this repo run on **Fabric**, a lightweight mod loader.

## Easiest: the Modrinth App

1. Install the [Modrinth App](https://modrinth.com/app).
2. Create a profile for the current Minecraft version (see [[Version Status]]) with **Fabric** as the loader.
3. Search for the mod and click Install. The app also installs anything it needs, like **Fabric API**.

## With the official launcher

1. Download the Fabric installer from [fabricmc.net/use](https://fabricmc.net/use/) and run it. Pick the current Minecraft version and click Install. You need to have launched that Minecraft version once before.
2. Open your `.minecraft` folder (in the launcher: Installations, hover the Fabric profile, the folder icon) and go into `mods` (create it if it's missing).
3. Download the mod's `.jar` from its Modrinth page, plus **Fabric API** and any other mods its page lists as required, and put them all in `mods`.
4. Start the game with the "fabric-loader" profile.

## Servers

Mods that change gameplay need to be on the server too. The mod's page says whether it's needed on the client, the server, or both. Put the same `.jar` files in the server's `mods` folder.

## If it crashes

Check that every required mod is installed and that they're all made for the same Minecraft version. The crash screen or `logs/latest.log` usually names the missing mod.
