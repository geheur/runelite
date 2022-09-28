package net.runelite.client.plugins.barragehelper;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

/*
The way I would like config to work is:
whitelist.
mark highest health target.
mark hp - <Off, Numerical, Health Bar> // Health bar is just a bunch of +++ for how much health it has i guess.
Tile location color toggle.

TODO - compatibility with opponent information opponent marker.

TODO deal with npcs that memory leak into the cache.
 */

@ConfigGroup("barrageHelper")
public interface BarrageHelperConfig extends Config
{
	// TODO allow "*" wildcards.
	@ConfigItem(
		keyName = "npcWhitelist",
		name = "Affected npc names",
		description = "Enemies that this plugin affects. Allows wildcard (*)."
	)
	default String getNpcWhitelist()
	{
		return "smoke devil, dust devil, greater nechryael";
	}

	@ConfigItem(
		keyName = "markHighestHealthTarget",
		name = "Mark highest health",
		description = "In the right click menu, indicates the highest health target on each tile"
	)
	default boolean markHighestHealthTarget()
	{
		return true;
	}

	enum HealthMarkingStyle { OFF, NUMERIC, NUMERIC_SHORT, ASCII_ART }

	@ConfigItem(
		keyName = "healthMarkingStyle",
		name = "Hp style",
		description = "How the hp shows up on the right-click menu"
	)
	default HealthMarkingStyle healthMarkingStyle()
	{
		return HealthMarkingStyle.OFF;
	}

	@ConfigItem(
		keyName = "colorMenuEntriesByTile",
		name = "Color by tile",
		description = "Colors the npc's name in the right-click menu based on the tile it is standing on"
	)
	default boolean colorMenuEntriesByTile()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showHealthOnAllEnemies",
		name = "Always show health",
		description = "Shows the health of the npc even if that npc is not on the whitelist"
	)
	default boolean showHealthOnAllEnemies()
	{
		return true;
	}

	@ConfigItem(
		keyName = "hideNpcLevels",
		name = "Hide npc levels",
		description = "Hides the (level-xx) text in the right-click menu."
	)
	default boolean hideNpcLevels()
	{
		return true;
	}
}
