package net.runelite.client.plugins.itemstats;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.runepouch.Runes;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.WidgetItemOverlay;
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

    @Override
    public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem itemWidget)
    {
        graphics.setFont(FontManager.getRunescapeSmallFont().deriveFont(36f));

        final Effect change = statChanges.get(itemId);
        if (change != null)
        {
            final StringBuilder b = new StringBuilder();
            final StatsChanges statsChanges = change.calculate(client);

            for (final StatChange c : statsChanges.getStatChanges())
            {
                b.append(c.getRelative());
                graphics.setColor(Positivity.getColor(config, c.getPositivity()));
                break;
            }

            final String tooltip = b.toString();

            Point location = itemWidget.getCanvasLocation();
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
