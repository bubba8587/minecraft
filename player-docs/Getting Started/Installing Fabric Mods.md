# Installing Fabric Mods

This tutorial explains how to install the mods in this guide.

- They run on **Fabric**, a lightweight mod loader.
- Most need **Fabric API** as well.

## Using the Modrinth App

- The easiest method, because it installs a mod's required mods automatically.

1. Install the [Modrinth App](https://modrinth.com/app).
2. Create a profile for the current Minecraft version (see [[Version Status]]) with **Fabric** as the loader.
3. Search for the mod and click **Install**. Required mods, such as Fabric API, are installed with it.

## Using the official launcher

1. Download the Fabric installer from [fabricmc.net/use](https://fabricmc.net/use/) and run it. Select the current Minecraft version and click **Install**. That Minecraft version must have been launched at least once before.
2. Open the `.minecraft` folder (in the launcher: **Installations**, hover over the Fabric profile, then the folder icon) and open the `mods` folder inside it. Create `mods` if it does not exist.
3. Download the mod's `.jar` from its Modrinth page, along with **Fabric API** and any other mod the page lists as required, and place them all in `mods`.
4. Start the game with the **fabric-loader** profile.

## Servers

- Mods that change gameplay must also be installed on the server.
- Each mod's page states whether it is needed on the client, the server, or both.
- Place the same `.jar` files in the server's `mods` folder.

## Troubleshooting

- If the game crashes on start, check that every required mod is installed and that all of them are made for the same Minecraft version.
- The crash screen or `logs/latest.log` usually names the missing mod.

## See also

- [[Installing Datapacks]]
- [[Installing Resource Packs]]
