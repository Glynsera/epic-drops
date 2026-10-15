package com.epicdrops;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("npcdropinspector")
public interface EpicDropsConfig extends Config
{
    enum LootSort
    {
        WIKI_ORDER,
        RAREST_FIRST,
        COMMON_FIRST
    }

    @ConfigItem(
        keyName = "enabled",
        name = "Enable Epic Drops",
        description = "Show NPC drop tooltips",
        position = 0
    )
    default boolean enabled()
    {
        return true;
    }

    @Range(min = 70, max = 150)
    @ConfigItem(
        keyName = "tooltipSize",
        name = "Tooltip size (%)",
        description = "Scale the entire tooltip from 70% to 150%",
        position = 1
    )
    default int tooltipSize()
    {
        return 100;
    }

    @Range(min = 30, max = 100)
    @ConfigItem(
        keyName = "backgroundOpacity",
        name = "Background opacity (%)",
        description = "Change how transparent the tooltip is",
        position = 2
    )
    default int backgroundOpacity()
    {
        return 95;
    }

    @Range(min = 1, max = 20)
    @ConfigItem(
        keyName = "maxDrops",
        name = "Visible drops",
        description = "Maximum drops shown",
        position = 3
    )
    default int maxDrops()
    {
        return 5;
    }

    @ConfigItem(
        keyName = "lootSort",
        name = "Loot sorting",
        description = "Sort drops by Wiki order or drop chance",
        position = 4
    )
    default LootSort lootSort()
    {
        return LootSort.WIKI_ORDER;
    }

    @ConfigItem(
        keyName = "showIcons",
        name = "Show item icons",
        description = "Display OSRS item icons",
        position = 5
    )
    default boolean showIcons()
    {
        return true;
    }

    @ConfigItem(
        keyName = "showDropRates",
        name = "Show drop rates",
        description = "Display drop chance percentages",
        position = 6
    )
    default boolean showDropRates()
    {
        return true;
    }
}