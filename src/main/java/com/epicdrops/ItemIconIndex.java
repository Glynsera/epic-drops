package com.epicdrops;

import net.runelite.client.game.ItemManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Singleton
public class ItemIconIndex
{
    private static final int MAX_ITEM_ID = 30000;
    private static final int ITEMS_PER_TICK = 500;

    private final ItemManager itemManager;
    private final Map<String, Integer> knownItems = new HashMap<>();

    private int nextItemId = 1;

    @Inject
    public ItemIconIndex(ItemManager itemManager)
    {
        this.itemManager = itemManager;
    }

    public void request(String itemName)
    {
        // Index is built automatically during client ticks.
    }

    public int getItemId(String itemName)
    {
        if (itemName == null)
        {
            return -1;
        }

        return knownItems.getOrDefault(
                itemName.trim().toLowerCase(Locale.ROOT),
                -1);
    }

    public void update()
    {
        int processed = 0;

        while (nextItemId < MAX_ITEM_ID
                && processed < ITEMS_PER_TICK)
        {
            int id = nextItemId++;
            processed++;

            try
            {
                String name = itemManager
                        .getItemComposition(id)
                        .getName();

                if (name != null && !name.isBlank())
                {
                    knownItems.putIfAbsent(
                            name.trim().toLowerCase(Locale.ROOT),
                            id);
                }
            }
            catch (Exception ignored)
            {
                // Ignore invalid definitions.
            }
        }
    }
}