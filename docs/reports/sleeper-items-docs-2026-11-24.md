# Sleeper items: forms kept through the item (docs report, 2026-11-24)

For specs/undead.md (new section "Sleeper items") and the Reworked Mobs player page.

## Bug
Owner playtest (creative): a statue (resting sleeper, the crypt-yard "resting dead") picked up with middle click / pick block
and placed again could be woken. Inertness is the block state property `resting` on the sleeper block. Silk Touch loot
already copied `pose` and `resting`, but `SleeperBlock.getCloneItemStack` (pick block) only wrote `pose` into the item's
`minecraft:block_state` component, so the flag was lost.

## Design (spec facts)
- Still one item per mob kind (`skeleton_sleeper`, `zombie_sleeper`, `husk_sleeper`, `drowned_sleeper`, `stray_sleeper`),
  now class `undead/sleeper/SleeperItem` (a BlockItem). Ids unchanged, so old worlds, inventories, the test world builder,
  loot tables and structures need no alias or data fix.
- The form is the vanilla `minecraft:block_state` data component: `pose` (`sitting` | `lying`) and `resting` (`true` = statue).
  Vanilla BlockItem applies it on placing, `copy_state` loot copies it, pick block now copies both. Gear stays in
  `minecraft:container` as before. Chosen over a custom component because every vanilla path (placing, loot, commands)
  already understands it (B9).
- One item per kind rather than one overall: the kind is the block, and vanilla maps block to item one to one (loot, pick
  block, item models with the mob head icon).
- Names: `SleeperItem.getName` builds "<base> (<pose>)": "Sleeping Skeleton (Sitting)", "Sleeping Zombie (Lying)",
  "Skeleton Statue (Lying)". Lang keys `item.bubbas_reworked_mobs.sleeper.form`, `...sleeper.pose.<pose>`,
  `block.bubbas_reworked_mobs.<kind>_sleeper.statue`. A custom name (anvil) still wins.
- Creative tab: every kind in every pose, sleeping and statue (20 stacks) plus the gravestone. Icons unchanged (mob heads).
- Prone (lying face down) does not exist yet: `lying` is on the back (supine). Adding `prone` needs a pose in
  `SleeperPoses` (client), a shape, and a lang key; the item side then works automatically.

## Player-facing
- Picking up a statue or a sleeper in any way (Silk Touch, middle click in creative) keeps its pose and whether it is a
  statue; placing it puts it back the same. Statues never wake.
- Item names show the form; the creative tab has every kind sitting, lying, and as a statue.

## Milestone test objectives
- `SleeperFormsGameTest.everyFormSurvivesPickUpAndPlace`: every kind x pose x statue survives form stack, pick block and Silk Touch drop; names show the form.
- `SleeperFormsGameTest.statueStaysInertAfterReplaceAndOldItemsPlace`: a re-placed statue ignores explosion, bell and noise; plain items place the default wakeable form.
