<!-- dte:C7,B16 -->
# Quartz tool textures: report (builder: agent 3)

The ask (orchestrator, 2026-11-22): quartz tool heads read mid-grey in the hotbar and in hand, while the armor reads properly white. Make the tools bright quartz white at 16x and 32x.

- **Cause:** vanilla's iron tool heads are thin, and about half of their pixels are the near-black outline and the dark inner edge. The quartz mapping kept both of those dark.
- **Fix:** in `mods/bubbas_reworked_mining/tools-local/make_quartz_textures.py`, for the tools only (sword, shovel, pickaxe, axe, hoe):
  - the head outline is now a soft warm grey, (132,122,121) instead of (60,50,52);
  - the inner dark edge is now the quartz shadow tone, (190,179,174);
  - the 32x twins use the same mapping.
  - The armor is unchanged.
- **Checked:**
  - Item sheet: iron, quartz 16x and quartz 32x side by side.
  - Client part `quartz` at 16x and 32x: the hotbar, the held pickaxe and sword, and an armor stand holding the pickaxe next to one holding iron's.
  - Result: the quartz heads read white, while iron's read near-black beside them. Held in first person they're still somewhat shaded. That's vanilla's hand lighting; iron darkens the same way.
- **Housekeeping:** `texcheck` OK. The art-todo row is updated.
- **Where:** branch `quartz-tools` (worktree ../mc-quartz), off develop. Commit 5892ccc has the textures; this report is the next commit.
