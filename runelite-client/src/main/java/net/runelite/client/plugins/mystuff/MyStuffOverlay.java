package net.runelite.client.plugins.mystuff;

import java.awt.Font;
import java.awt.geom.Point2D;

import net.runelite.api.*;
import net.runelite.client.game.LootManager;
import net.runelite.client.plugins.barragehelper.BarrageHelperPlugin;
import net.runelite.client.plugins.playerindicators.PlayerNameLocation;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

import javax.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Map;

import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.util.Text;

public class MyStuffOverlay extends Overlay
{
    private final Client client;

    @Inject
    private MyStuffOverlay(Client client)
    {
        this.client = client;
        setPosition(OverlayPosition.DYNAMIC);
        setPriority(OverlayPriority.LOW);
        setLayer(OverlayLayer.ALWAYS_ON_TOP);
    }

    public long lastGameTickMillis = 0;
    public int lastFishingSpotTickTime = 0;

    public volatile float eatDelay = 0;

    public String playerOverheadText = null;

    private static final int ACTOR_OVERHEAD_TEXT_MARGIN = 40;
    @Override
    public Dimension render(Graphics2D graphics)
    {
        for (Map.Entry<NPC, Integer> entry : BarrageHelperPlugin.predictedNpcHealthCache.entrySet()) {
            NPC actor = entry.getKey();
            Integer health = entry.getValue();
//            System.out.println("drawing health " + health + " for actor " + actor.getName());
            int zOffset;
            zOffset = actor.getLogicalHeight() + ACTOR_OVERHEAD_TEXT_MARGIN;

//            final String name = Text.sanitize(actor.getName());
            Point textLocation = actor.getCanvasTextLocation(graphics, Integer.toString(health), zOffset);

            if (textLocation == null)
            {
                System.out.println("textlocation is null");
                continue;
            }

            Color color = (health <= 0) ? Color.RED : Color.WHITE;
            OverlayUtil.renderTextLocation(graphics, textLocation, Integer.toString(health), color);
        }

        if (playerOverheadText != null)
        {
            Player localPlayer = client.getLocalPlayer();
            Font font = graphics.getFont();
            graphics.setFont(FontManager.getRunescapeSmallFont().deriveFont(36f));
            Point textLocation = localPlayer.getCanvasTextLocation(graphics, playerOverheadText, localPlayer.getLogicalHeight() + 40);
            if (textLocation != null)
            {
                OverlayUtil.renderTextLocation(graphics, textLocation, playerOverheadText, Color.GREEN);
            }
            graphics.setFont(font);
        }

        Point runeliteMousePoint = client.getMouseCanvasPosition();

//		if (eatDelay >= 0)
//		{
//			int length = (int) (((1 - (System.currentTimeMillis() - lastGameTickMillis) / 600D) + eatDelay - 1) * 30);
//			graphics.setColor(Color.RED);
//			graphics.fillRect(runeliteMousePoint.getX(), runeliteMousePoint.getY() - 20, length, 10);
//		}
//		else
//		{
        graphics.setColor(Color.GREEN);
//			graphics.drawString("READY", runeliteMousePoint.getX(), runeliteMousePoint.getY() - 20);
//        int numHits = getNumHits(LootManager.lastcerbanimation);
//		graphics.drawString("" + numHits + " " + LootManager.cerbhp, runeliteMousePoint.getX(), runeliteMousePoint.getY() - 20);
        graphics.drawString("" + MyStuff.ticks, runeliteMousePoint.getX(), runeliteMousePoint.getY() - 20);
//		}

        Point2D.Float mouse = new Point2D.Float(runeliteMousePoint.getX(), runeliteMousePoint.getY());

        return null;
    }

//    public static void printCerbProgress()
//    {
//        int numHits = getNumHits(LootManager.lastcerbanimation);
////		System.out.println(" 1  2  3  4  5  6  7  8  9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32 33 34 35");
////		System.out.println(" T                             T        S                 L  T           L        G     L");
////		System.out.println(" T  _  _  _  _  _  _  _  _  _  T  _  _  S  _  _  _  _  _  _  T  _  _  _  L  _  _  G  _  L");
//        String s = "";
//        for (int i = 0; i < numHits; i++) {
//            s += "   ";
//        }
//        String s2 = new String(s);
//        s += " ^";
//        s2 += " |";
////		System.out.println(s);
////		System.out.println(s2);
//    }
//

    public static int getNumHits(int numHitSplats)
    {
        int numHits = numHitSplats;
        if (numHits > 1) numHits--;
        if (numHits > 1) numHits--;
        if (numHits > 11) numHits--;
        if (numHits > 11) numHits--;
        if (numHits > 21) numHits--;
        if (numHits > 21) numHits--;
        return numHits;
    }
}
