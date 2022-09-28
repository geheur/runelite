package net.runelite.client.plugins.itemstats;

import java.awt.Color;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.api.Point;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.itemstats.stats.EnergyStat;
import net.runelite.client.plugins.itemstats.stats.SkillStat;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.WidgetItemOverlay;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;

public class ItemStatsWidgetItemOverlay extends WidgetItemOverlay
{
    private final Client client;
    private final TooltipManager tooltipManager;

    @Inject private ItemManager itemManager;
    @Inject private ItemStatChanges statChanges;
    @Inject private ItemStatConfig config;
    @Inject private ItemStatChangesService service;

    @Inject
    ItemStatsWidgetItemOverlay(Client client, TooltipManager tooltipManager)
    {
        this.tooltipManager = tooltipManager;
        this.client = client;
        showOnInventory();
//		showOnBank();
    }

    @Override
    public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem itemWidget)
    {
        Effect change = service.getItemStatChanges(itemId);
        if (change != null)
        {
			graphics.setFont(FontManager.getRunescapeSmallFont().deriveFont(36f));

			StringBuilder b = new StringBuilder();
            StatsChanges statsChanges = change.calculate(client);

            for (StatChange c : statsChanges.getStatChanges())
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

			Point location = itemWidget.getCanvasLocation();
			graphics.drawString(tooltip, location.getX(), location.getY() + 36);
        }
    }
}
