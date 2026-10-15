package com.epicdrops;

import com.google.inject.Provides;
import javax.inject.Inject;
import net.runelite.api.events.ClientTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
        name = "Epic Drops",
        description = "Fantasy-style NPC loot inspector",
        tags = {"npc", "drops", "loot", "wiki", "epic"}
)
public class EpicDropsPlugin extends Plugin
{
    @Inject
    private OverlayManager overlayManager;

    @Inject
    private EpicDropsOverlay overlay;

    @Inject
    private DropDataService dropDataService;

    @Inject
    private ItemIconIndex itemIconIndex;

    @Provides
    EpicDropsConfig provideConfig(ConfigManager manager)
    {
        return manager.getConfig(EpicDropsConfig.class);
    }

    @Override
    protected void startUp()
    {
        overlayManager.add(overlay);
    }

    @Override
    protected void shutDown()
    {
        overlayManager.remove(overlay);
        dropDataService.shutdown();
    }

    @Subscribe
    public void onClientTick(ClientTick event)
    {
        itemIconIndex.update();
    }
}