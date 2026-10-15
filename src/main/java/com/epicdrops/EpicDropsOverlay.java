package com.epicdrops;

import net.runelite.api.Client;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

import javax.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class EpicDropsOverlay extends Overlay
{
    private static final int WIDTH = 305;
    private static final int PADDING = 12;
    private static final int ROW_HEIGHT = 32;

    private static final Color PURPLE = new Color(151, 94, 213);
    private static final Color GOLD = new Color(235, 184, 91);
    private static final Color GREEN = new Color(105, 225, 155);
    private static final Color BLUE = new Color(105, 165, 255);
    private static final Color EPIC = new Color(205, 140, 255);
    private static final Color TEXT = new Color(234, 226, 240);

    private final Client client;
    private final EpicDropsConfig config;
    private final DropDataService dropDataService;
    private final ItemManager itemManager;
    private final ItemIconIndex itemIconIndex;

    @Inject
    public EpicDropsOverlay(
            Client client,
            EpicDropsConfig config,
            DropDataService dropDataService,
            ItemManager itemManager,
            ItemIconIndex itemIconIndex)
    {
        this.client = client;
        this.config = config;
        this.dropDataService = dropDataService;
        this.itemManager = itemManager;
        this.itemIconIndex = itemIconIndex;

        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPriority(OverlayPriority.HIGH);
        setMovable(false);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!config.enabled() || client.isMenuOpen())
        {
            return null;
        }

        NPC npc = findHoveredNpc();

        if (npc == null)
        {
            return null;
        }

        net.runelite.api.Point mouse = client.getMouseCanvasPosition();

        if (mouse == null)
        {
            return null;
        }

        List<DropEntry> drops = new ArrayList<>();

        for (String raw : dropDataService.getDrops(npc.getId(), npc.getName()))
        {
            drops.add(new DropEntry(raw));
        }

        sortDrops(drops);

        int visible = Math.min(Math.max(1, config.maxDrops()), drops.size());
        boolean hasMore = drops.size() > visible;
        int rows = Math.max(1, visible);
        int height = 102 + rows * ROW_HEIGHT + (hasMore ? 20 : 0);

        for (int i = 0; i < visible; i++)
        {
            itemIconIndex.request(drops.get(i).name);
        }

        double scale = Math.max(0.7, Math.min(1.5,
                config.tooltipSize() / 100.0));

        int scaledWidth = (int) Math.ceil(WIDTH * scale);
        int scaledHeight = (int) Math.ceil(height * scale);

        int x = mouse.getX() + 16;
        int y = mouse.getY() + 42;

        Dimension canvas = client.getCanvas().getSize();

        if (x + scaledWidth > canvas.width - 5)
        {
            x = mouse.getX() - scaledWidth - 16;
        }

        if (y + scaledHeight > canvas.height - 5)
        {
            y = mouse.getY() - scaledHeight - 28;
        }

        x = Math.max(5, Math.min(x,
                Math.max(5, canvas.width - scaledWidth - 5)));

        y = Math.max(5, Math.min(y,
                Math.max(5, canvas.height - scaledHeight - 5)));

        Graphics2D g = (Graphics2D) graphics.create();

        try
        {
            g.translate(x, y);
            g.scale(scale, scale);

            g.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            g.setRenderingHint(
                    RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int opacity = Math.max(30,
                    Math.min(100, config.backgroundOpacity()));

            int alpha = (int) Math.round(255 * opacity / 100.0);

            g.setColor(new Color(17, 13, 24, alpha));
            g.fillRoundRect(0, 0, WIDTH, height, 10, 10);

            g.setColor(PURPLE);
            g.drawRoundRect(0, 0, WIDTH - 1, height - 1, 10, 10);

            g.setColor(GOLD);
            g.fillRoundRect(PADDING, 5,
                    WIDTH - PADDING * 2, 2, 2, 2);

            Font originalFont = g.getFont();

            g.setFont(originalFont.deriveFont(Font.BOLD, 15f));
            g.setColor(GOLD);
            g.drawString("EPIC DROPS", PADDING, 27);

            g.setFont(originalFont.deriveFont(Font.PLAIN, 11f));
            g.setColor(new Color(175, 160, 190));

            g.setFont(originalFont.deriveFont(Font.BOLD, 16f));
            g.setColor(new Color(255, 218, 145));
            g.drawString(
                    fitText(g, npc.getName(), WIDTH - 110),
                    PADDING, 55);

            g.setFont(originalFont.deriveFont(Font.BOLD, 12f));
            g.setColor(new Color(195, 170, 220));
            g.drawString("Lvl " + npc.getCombatLevel(),
                    WIDTH - 62, 54);

            g.setColor(new Color(90, 70, 110));
            g.drawLine(PADDING, 66, WIDTH - PADDING, 66);

            g.setFont(originalFont.deriveFont(Font.BOLD, 11f));
            g.setColor(new Color(175, 155, 195));
            g.drawString("POSSIBLE LOOT", PADDING, 83);

            if (config.showDropRates())
            {
                g.drawString("DROP RATE", WIDTH - 85, 83);
            }

            int rowY = 90;

            if (drops.isEmpty())
            {
                drawRowBackground(g, rowY, 0);

                g.setFont(originalFont.deriveFont(Font.PLAIN, 12f));
                g.setColor(TEXT);
                g.drawString(
                        fitText(g, dropDataService.getStatus(npc.getName()),
                                WIDTH - 30),
                        18, rowY + 21);
            }
            else
            {
                for (int i = 0; i < visible; i++)
                {
                    drawDropRow(g, rowY + i * ROW_HEIGHT,
                            drops.get(i), i);
                }
            }

            if (hasMore)
            {
                g.setFont(originalFont.deriveFont(Font.PLAIN, 11f));
                g.setColor(new Color(170, 155, 190));
                g.drawString("+" + (drops.size() - visible)
                        + " more drops",
                        PADDING, rowY + visible * ROW_HEIGHT + 15);
            }
        }
        finally
        {
            g.dispose();
        }

        return null;
    }

    private void sortDrops(List<DropEntry> drops)
    {
        if (config.lootSort()
                == EpicDropsConfig.LootSort.RAREST_FIRST)
        {
            drops.sort(Comparator.comparingDouble(
                    d -> d.probability < 0
                            ? Double.MAX_VALUE : d.probability));
        }
        else if (config.lootSort()
                == EpicDropsConfig.LootSort.COMMON_FIRST)
        {
            drops.sort((a, b) -> Double.compare(
                    b.probability < 0 ? -1 : b.probability,
                    a.probability < 0 ? -1 : a.probability));
        }
    }

    private void drawRowBackground(Graphics2D g, int y, int index)
    {
        g.setColor(index % 2 == 0
                ? new Color(41, 30, 54, 235)
                : new Color(29, 22, 41, 235));

        g.fillRoundRect(8, y, WIDTH - 16,
                ROW_HEIGHT - 2, 5, 5);
    }

    private void drawDropRow(
            Graphics2D g, int y, DropEntry drop, int index)
    {
        drawRowBackground(g, y, index);

        Color rarityColor = getRarityColor(drop);

        g.setColor(rarityColor);
        g.fillRoundRect(8, y + 5, 2,
                ROW_HEIGHT - 12, 2, 2);

        if (config.showIcons())
        {
            int itemId = itemIconIndex.getItemId(drop.name);

            if (itemId > 0)
            {
                BufferedImage icon = itemManager.getImage(itemId);

                if (icon != null)
                {
                    g.drawImage(icon, 14, y + 5, 22, 22, null);
                }
            }
        }

        Font baseFont = g.getFont();
        g.setFont(baseFont.deriveFont(Font.PLAIN, 13f));
        g.setColor(TEXT);

        String itemText = drop.name;

        if (!drop.quantity.isBlank())
        {
            itemText += " x" + drop.quantity;
        }

        int nameX = config.showIcons() ? 42 : 18;
        int nameWidth = config.showDropRates() ? 157 : 255;

        g.drawString(
                fitText(g, itemText, nameWidth),
                nameX, y + 21);

        if (config.showDropRates())
        {
            String rateText = drop.rarity;

            if (drop.rarity.equalsIgnoreCase("Always"))
            {
                rateText = "100%";
            }
            else if (drop.probability >= 0)
            {
                rateText = String.format(Locale.ROOT,
                        "%.2f%%", drop.probability * 100.0);
            }

            g.setFont(new Font("SansSerif", Font.BOLD, 13));
            g.setColor(rarityColor);

            rateText = fitText(g, rateText, 83);
            FontMetrics metrics = g.getFontMetrics();

            g.drawString(rateText,
                    WIDTH - 13 - metrics.stringWidth(rateText),
                    y + 21);
        }
    }

    private Color getRarityColor(DropEntry drop)
    {
        double chance = drop.probability;

        if (chance < 0)
        {
            return TEXT;
        }
        if (chance <= 0.001)
        {
            return GOLD;
        }
        if (chance <= 0.01)
        {
            return EPIC;
        }
        if (chance <= 0.05)
        {
            return BLUE;
        }
        if (chance < 1.0)
        {
            return new Color(230, 192, 110);
        }

        return GREEN;
    }

    private NPC findHoveredNpc()
    {
        MenuEntry[] entries = client.getMenuEntries();

        if (entries == null)
        {
            return null;
        }

        for (int i = entries.length - 1; i >= 0; i--)
        {
            NPC npc = entries[i].getNpc();

            if (npc != null
                    && npc.getName() != null
                    && !npc.getName().isBlank())
            {
                return npc;
            }
        }

        return null;
    }

    private String fitText(Graphics2D g, String text, int maxWidth)
    {
        FontMetrics metrics = g.getFontMetrics();

        if (metrics.stringWidth(text) <= maxWidth)
        {
            return text;
        }

        while (!text.isEmpty()
                && metrics.stringWidth(text + "...") > maxWidth)
        {
            text = text.substring(0, text.length() - 1);
        }

        return text + "...";
    }

    private static class DropEntry
    {
        private final String name;
        private final String quantity;
        private final String rarity;
        private final double probability;

        private DropEntry(String raw)
        {
            String item = raw;
            String rate = "";

            int separator = raw.lastIndexOf(" - ");

            if (separator >= 0)
            {
                item = raw.substring(0, separator);
                rate = raw.substring(separator + 3).trim();
            }

            String amount = "";
            int quantityIndex = item.lastIndexOf(" x");

            if (quantityIndex >= 0)
            {
                amount = item.substring(quantityIndex + 2);
                item = item.substring(0, quantityIndex);
            }

            name = item.trim();
            quantity = amount;
            rarity = rate;
            probability = parseProbability(rate);
        }

        private static double parseProbability(String rarity)
        {
            if (rarity.equalsIgnoreCase("Always"))
            {
                return 1.0;
            }

            try
            {
                String value = rarity.replace(",", "").trim();

                if (value.endsWith("%"))
                {
                    return Double.parseDouble(
                            value.substring(0, value.length() - 1)
                                    .trim()) / 100.0;
                }

                String[] parts = value.split("/");

                if (parts.length == 2)
                {
                    double numerator =
                            Double.parseDouble(parts[0].trim());
                    double denominator =
                            Double.parseDouble(parts[1].trim());

                    if (denominator > 0
                            && numerator >= 0
                            && numerator <= denominator)
                    {
                        return numerator / denominator;
                    }
                }
            }
            catch (NumberFormatException ignored)
            {
                // Keep unknown Wiki rates unchanged.
            }

            return -1;
        }
    }
}