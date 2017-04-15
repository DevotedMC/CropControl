CropControl
===========

Simple (relatively) augmentation of crop and tree drops. (Planned to control as well).

Placements of growth-starting blocks are tracked, including who placed it (if available). Growth-starting blocks that spread also share-forward this placement information, so we can effectively answer if the player breaking a growable was the player who placed the seeds.

Some edge cases, especially for Chorus Fruit, are not well supported at the moment. Known issues:

* Chorus Flowers placed alongside existing Chorus Plant blocks will result in a ghost tree entry when the whole tree is later broken

* Chorus plants broken by block placements over bend-junctions might not be captured at all, depending on nature of placement.

We do currently make a best-effort to capture breaks and drops from pistons, flowing liquids, block placements, "supporting" block breaks,
fire breaks, and direct breaks. 

The nature of the break (was it by a person? Piston? Liquid? ) can be used to tweak the chance and size of any drops, as can the biome. As will
be described, for many plants you can even control drop augments based on lifecycle.

We also internally track a harvestable flag, to prevent players from accelerating augment opportunities by immediately breaking a block they just placed. This applies most strongly to saplings, mushroom, cactus, pumpkins, melons, sugar cane, and chorus fruit as they all "spread". The blocks they spread to will be marked as harvestable. It's configurable however if your augmentation cares. See example config for details.

### Compatibility

This plugin is currently compatible with Spigot / Craftbukkit 1.10.2

Please note that any plugin that modifies crop behavior will interfere with this plugin's operation.

**RealisticBiomes:** To ensure compatibility, modifications were made to RealisticBiomes. Dee the com.programmerdan.minecraft.cropcontrol.handler.RealisticBiomesEventHandler for one route, and the corresponding code in RealisticBiomes. As a result, Realistic Biomes is a compile-time dependency. It is not required during runtime.

## Configuration

Configuration is straightforward.

`database`: section defines the host and other connection primitives. See example config.

`drops`: section defines all the things that can be dropped, leveraging templates. Use SimpleAdminHacks for easier template generation.

`tools`: section defines all the tools you care about. (TODO)

`crops`: section describes for each type of crop and life stage (if meaningful) the drops associated.
  You can hang off of these modifiers for biomes, break type and unique break conditions and eventually, tool used.
  This can be defined for each growth stage, allowing unprecedented augment control.

`saplings`: section describes for each sapling if drops are associated with them. Probably not leveraged
  often but could be used to provide rare unique items from uprooting saplings.

`trees`: section describes for each tree type is drops are associated with them.
  You can hang off of these modifiers for biomes, break type unique break conditions and eventually, tool used.
  Eventually this will be definable for each block type within the tree, for now just the tree as a whole.

That's about it.

## Valid Crops list and stage enumeration

Only ever list the crops and stages you want to augment, don't bother listing everything. If you do want to though, here's the list as of
Minecraft 1.10.2:

`CROPS`. In-game wheat. Stages: `SEEDED`, `GERMINATED`, `VERY_SMALL`, `SMALL`, `MEDIUM`, `TALL`, `VERY_TALL`, `RIPE`.

`CARROT`. In-game carrot. Stages: `SEEDED`, `GERMINATED`, `VERY_SMALL`, `SMALL`, `MEDIUM`, `TALL`, `VERY_TALL`, `RIPE`.

`POTATO`. In-game potato. Stages: `SEEDED`, `GERMINATED`, `VERY_SMALL`, `SMALL`, `MEDIUM`, `TALL`, `VERY_TALL`, `RIPE`.

`BEETROOT_BLOCK`. In-game beetroot. Stages: `SEEDED`, `SMALL`, `TALL`, `RIPE`.

`NETHER_WARTS`. In-game netherwart. Stages: `SEEDED`, `STAGE_ONE`, `STAGE_TWO`, `RIPE`.

`MELON_STEM`. In-game melon stem. Stages: `0`, `1`, `2`, `3`, `4`, `5`, `6`, `7`. Flagged Unharvestable.

`MELON_BLOCK`. In-game melon. No stages.

`PUMPKIN_STEM`. In-game pumpkin stem. Stages: `0`, `1`, `2`, `3`, `4`, `5`, `6`, `7`. Flagged Unharvestable.

`PUMPKIN_BLOCK`. In-game pumpkin. No stages.

`COCOA`. In-game cocoa plant. Stages: `SMALL`, `MEDIUM`, `LARGE`.

`CACTUS`. In-game cacuts. No stages. Base flagged Unharvestable.

`SUGAR_CANE_BLOCK`. In-game sugarcane. No stages. Base flagged Unharvestable.

`BROWN_MUSHROOM`. In-game brown mushroom. No stages. Original placed flagged Unharvestable.

`RED_MUSHROOM`. In-game red mushroom. No stages. Original placed flagged Unharvestable.

## Valid Saplings list

Similar to crops, only list the saplings you care to augment. As of Minecraft 1.10.2:

`OAK_SAPLING`: Oak tree sapling.

`SPRUCE_SAPLING`: Spruce tree sapling. Can be 2x2, any drop attempt will apply to each separately so keep that in mind.

`BIRCH_SAPLING`: Birch tree sapling.

`JUNGLE_SAPLING`: Jungle tree sapling. Same 2x2 warning as Spruce.

`ACACIA_SAPLING`: Acacia tree sapling.

`DARK_OAK_SAPLING`: Dark Oak tree sapling. Same 2x2 warning as Spruce.

## Valid Trees list

Similar to Saplings, only list the trees you care to augment. As of Minecraft 1.10.2:

`CHORUS_PLANT`: Chorus plant part (includes flowers, nodes, and the like)

`TREE`: Oak tree

`BIG_TREE`: Very large Oak tree

`REDWOOD`: Normal Spruce

`TALL_REDWOOD`: 2x2 Spruce

`BIRCH`: Birch tree

`JUNGLE`: 2x2 Jungle tree

`SMALL_JUNGLE`: Jungle tree, 1 block, small

`COCOA_TREE`: Jungle tree with cocoa

`JUNGLE_BUSH`: Tiny Jungle bush

`RED_MUSHROOM`: Giant Red Mushroom

`BROWN_MUSHROOM`: Giant Brown Mushroom

`SWAMP`: Swamp tree (oak with vines)

`ACACIA`: Acacia tree

`DARK_OAK`: 2x2 Dark Oak tree

`MEGA_REDWOOD`: 4x4 redwood tree

`TALL_BIRCH`: Extra tall Birch tree

More Details
============

Check the example `src/main/resources/config.yml` for further details on each individual value and its meaning.
