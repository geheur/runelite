package net.runelite.client.plugins.itemstats;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.api.Point;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.boosts.BoostsPlugin;
import net.runelite.client.plugins.itemstats.stats.EnergyStat;
import net.runelite.client.plugins.itemstats.stats.SkillStat;
import net.runelite.client.plugins.mystuff.MyStuff;
import net.runelite.client.plugins.runepouch.Runes;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.WidgetItemOverlay;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TextComponent;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;

public class ItemStatsWidgetItemOverlay extends WidgetItemOverlay
{
    private static final Dimension IMAGE_SIZE = new Dimension(11, 11);

    private final Client client;
    private final TooltipManager tooltipManager;

    @Inject
    private ItemManager itemManager;

    @Inject
    private ItemStatChanges statChanges;

    @Inject
    private ItemStatConfig config;

    @Inject
    ItemStatsWidgetItemOverlay(Client client, TooltipManager tooltipManager)
    {
        this.tooltipManager = tooltipManager;
        this.client = client;
        showOnInventory();
//		showOnBank();
    }

    @Inject
    public PluginManager pluginManager;

    private final Map<String, Plugin> pluginsCache = new HashMap<>();
    private Plugin getPlugin(String pluginName) {
        Plugin plugin1 = pluginsCache.get(pluginName);
        if (plugin1 != null) return plugin1;
        for (Plugin plugin : pluginManager.getPlugins()) {
            if (pluginName.equals(plugin.getClass().getSimpleName())) {
                pluginsCache.put(pluginName, plugin);
                return plugin;
            }
        }
        return null;
    }

    @Override
    public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem itemWidget)
    {
        Point location = itemWidget.getCanvasLocation();

//        BoostsPlugin boostsPlugin = (BoostsPlugin) getPlugin("BoostsPlugin");

        final Effect change = statChanges.get(itemId);
        if (change != null)
        {
//            if (boostsPlugin != null) {
//                int nextChange = boostsPlugin.getChangeDownTicks();
//
//                int timeToNextStatChange = -1;
//                if (nextChange != -1) {
//                    timeToNextStatChange = boostsPlugin.getChangeTime(nextChange);
//                } else {
//                    nextChange = boostsPlugin.getChangeUpTicks();
//
//                    if (nextChange != -1) {
//                        timeToNextStatChange = boostsPlugin.getChangeTime(nextChange);
//                    }
//                }
//                if (timeToNextStatChange != -1) {
////                    graphics.setColor(Color.WHITE);
//                    graphics.setFont(FontManager.getRunescapeSmallFont().deriveFont(12f));
////                    graphics.drawString("" + timeToNextStatChange, location.getX() + 15, location.getY() + 15);
//
//                    final Rectangle bounds = itemWidget.getCanvasBounds();
//                    final net.runelite.client.ui.overlay.components.TextComponent textComponent = new TextComponent();
//                    textComponent.setPosition(new java.awt.Point(bounds.x + 15, bounds.y + 15));
//                    textComponent.setText("" + timeToNextStatChange);
//                    textComponent.setColor(Color.WHITE);
//                    textComponent.render(graphics);
//                }
//            }

            graphics.setFont(FontManager.getRunescapeSmallFont().deriveFont(36f));

            final StringBuilder b = new StringBuilder();
            final StatsChanges statsChanges = change.calculate(client);

            for (final StatChange c : statsChanges.getStatChanges())
            {
                int baseStat = c.getStat().getMaximum(client);
                int currentStat = c.getStat().getValue(client);
//                System.out.println("base stat: " + baseStat + " " + currentStat);
                int valueToDisplay = -1;
                boolean statCantBeBoosted = c.getStat() instanceof EnergyStat || (c.getStat() instanceof SkillStat && (((SkillStat) c.getStat()).getName().equalsIgnoreCase("prayer") || ((SkillStat) c.getStat()).getName().equalsIgnoreCase("hitpoints")));
                if (currentStat < baseStat && !statCantBeBoosted) {
                    graphics.setColor(Color.RED);
                    valueToDisplay = currentStat - baseStat;
                } else {
                    graphics.setColor(Positivity.getColor(config, c.getPositivity()));
                    valueToDisplay = c.getRelative();
                }

                ItemComposition comp = client.getItemDefinition(itemId);
//                System.out.println("stat change for item " + comp.getName() + " " + c.getStat() + " " + c.getFormattedTheoretical() + " " + c.getFormattedRelative() + " " + c.getRelative() + " " + c.getAbsolute() + " " + c.getPositivity());
                b.append(valueToDisplay);
                break;
            }

            final String tooltip = b.toString();

            graphics.drawString(tooltip, location.getX(), location.getY() + 36);
        }
    }

    private BufferedImage getRuneImage(Runes rune)
    {
        BufferedImage runeImg = rune.getImage();
        if (runeImg != null)
        {
            return runeImg;
        }

        runeImg = itemManager.getImage(rune.getItemId());
        if (runeImg == null)
        {
            return null;
        }

        BufferedImage resizedImg = new BufferedImage(IMAGE_SIZE.width, IMAGE_SIZE.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = resizedImg.createGraphics();
        g.drawImage(runeImg, 0, 0, IMAGE_SIZE.width, IMAGE_SIZE.height, null);
        g.dispose();

        rune.setImage(resizedImg);
        return resizedImg;
    }

    private static String formatNumber(int amount)
    {
//		return amount < 1000 ? String.valueOf(amount) : amount / 1000 + "K";
        return amount < 1000 ? String.valueOf(amount) : new DecimalFormat("#0.0").format(amount / 1000.0);
    }
}
