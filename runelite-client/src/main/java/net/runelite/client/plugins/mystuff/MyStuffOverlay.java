package net.runelite.client.plugins.mystuff;

import java.awt.Font;
import java.awt.Rectangle;
import java.awt.geom.Point2D;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;
import net.runelite.api.*;
import net.runelite.api.clan.ClanTitle;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.mystuff.MyStuff.ExhumedWithCount;
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

import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.util.Text;

public class MyStuffOverlay extends Overlay
{
    private final Client client;
	private final MyStuff plugin;
	public boolean firstPrayerWasMage;

    @Inject
    private MyStuffOverlay(Client client, MyStuff plugin)
    {
        this.client = client;
		this.plugin = plugin;
		setPosition(OverlayPosition.DYNAMIC);
        setPriority(OverlayPriority.LOW);
        setLayer(OverlayLayer.ALWAYS_ON_TOP);
    }

    public long lastGameTickMillis = 0;

    public volatile float eatDelay = 0;

    public String playerOverheadText = null;

    private static final int ACTOR_OVERHEAD_TEXT_MARGIN = 40;
    @Override
    public Dimension render(Graphics2D graphics)
    {
    	renderFairyRingStallTimer(graphics);
		renderKqShortcutStallTimer(graphics);
		renderKqPhaseChangeTimer(graphics);
		renderXarpusExhumeds(graphics);
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

        graphics.setColor(Color.GREEN);
        boolean atHydra = false;
        boolean atGauntlet = false;
        boolean atCerberus = false;
        boolean atZulrah = false;
        for (int mapRegion : client.getMapRegions()) {
            if (mapRegion == 5536) {
                atHydra = true;
                break;
            }
            if (mapRegion == 7768) {
                atGauntlet = true;
                break;
            }
            if (mapRegion == 4883) {
                atCerberus = true;
                break;
            }
            if (mapRegion == 9007 || mapRegion == 9008) {
                atZulrah = true;
                break;
            }

            if (mapRegion == 14642 && client.getVarbitValue(4070) != 1) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setColor(Color.YELLOW);
                Font font = graphics.getFont();
                g.setFont(FontManager.getRunescapeSmallFont().deriveFont(48f));
                g.drawString("ANCIENTS YOU FUCKING MORON", runeliteMousePoint.getX(), runeliteMousePoint.getY());
                break;
            }
        }
        if (atHydra) {
            HydraState hydraState = getHydraAttackIndex(MyStuff.hydraFightDuration, hydraPhase, hydraPhaseBlue, hydraPhaseRed, hydraPhaseGray);
            boolean shouldPrayMage = hydraState.prayerIsOdd ^ !firstPrayerWasMage;
            boolean magePray = client.isPrayerActive(Prayer.PROTECT_FROM_MAGIC);
//            System.out.println(shouldPrayMage + " " + hydraState.prayerIsOdd + " " + firstPrayerWasMage + " " + magePray);
            if (magePray ^ shouldPrayMage && MyStuff.hydraFightDuration >= 13) {
                graphics.setColor(Color.RED);
                graphics.drawString("SWITCH !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!", runeliteMousePoint.getX() + 20, runeliteMousePoint.getY() + 20);
            }
            graphics.setColor(Color.GREEN);

//            graphics.drawString("" + ((client.getTickCount() - hydrageysertick) % 8), runeliteMousePoint.getX(), runeliteMousePoint.getY() - 20);
            graphics.drawString("" + MyStuff.hydraFightDuration, runeliteMousePoint.getX() + 20, runeliteMousePoint.getY() - 4);

            graphics.drawString("" + (hydraticks % 3) + " " + ((client.getTickCount() - hydrageysertick) % 8), runeliteMousePoint.getX(), runeliteMousePoint.getY() - 20);
//            graphics.drawString("" + (hydraticks % 3) + ((lastPrayerWasMage != null) ? ((lastPrayerWasMage) ? " mage" : " range") : "") + " " + ((client.getTickCount() - hydrageysertick) % 8), runeliteMousePoint.getX(), runeliteMousePoint.getY() - 20);
            graphics.drawString("" + hydraState.prayerIsOdd, runeliteMousePoint.getX() + 20, runeliteMousePoint.getY() + 4);
        } else if (atGauntlet) {
            graphics.setFont(graphics.getFont().deriveFont(24));
            graphics.drawString("" + (MyStuff.playerattackcounter % 6) + " " + MyStuff.hunllefattackcounter, runeliteMousePoint.getX(), runeliteMousePoint.getY() - 20);
            //  + " " + ((client.getTickCount() - hydrageysertick) % 7)
        } else if (atCerberus)
        {
            graphics.setFont(graphics.getFont().deriveFont(24));
            String nextEvent = null;
            int ticksUntil = 0;
            int ticksIntoFight = MyStuff.cerbfighttimer - client.getTickCount();
//            System.out.println("ticks into fight: " + ticksIntoFight);
//            if (ticksIntoFight < 67) {
//                nextEvent = "triple (2)";
//                ticksUntil = -1 * (ticksIntoFight - 67);
//            } else if (ticksIntoFight < 67 + 24) {
//                nextEvent = "start";
//                ticksUntil = -1 * (ticksIntoFight - (67 + 24));
//            } else if (ticksIntoFight < 67 + 24 + 36) {
//                nextEvent = "lava (1)";
//                ticksUntil = -1 * (ticksIntoFight - (67 + 24 + 36));
//            } else if (ticksIntoFight < 67 + 24 + 36 + 5) {
//                nextEvent = "triple (3)";
//                ticksUntil = -1 * (ticksIntoFight - (67 + 24 + 36 + 5));
//            } else if (ticksIntoFight < 67 + 24 + 36 + 5 + 30) {
//                nextEvent = "lava";
//                ticksUntil = -1 * (ticksIntoFight - (67 + 24 + 36 + 5 + 30));
//            }
//            if (nextEvent != null) {
//                graphics.drawString("" + ticksUntil + " " + nextEvent, runeliteMousePoint.getX(), runeliteMousePoint.getY() - 20);
//                System.out.println("ticksUntil: " + ticksUntil);
//            } else {
            graphics.drawString("" + (MyStuff.countdowntimer - client.getTickCount()) + " " + MyStuff.countingdownto, runeliteMousePoint.getX(), runeliteMousePoint.getY() - 20);
//            }
            //  + " " + ((client.getTickCount() - hydrageysertick) % 7)
        } else if (atZulrah) {
            int ticksSinceLastDown = client.getTickCount() - MyStuff.lastZulrahDown;
            if (ticksSinceLastDown < 7) {
                graphics.drawString("" + (7 - ticksSinceLastDown), runeliteMousePoint.getX(), runeliteMousePoint.getY());
            }
        } else {
            graphics.drawString("" + MyStuff.ticks, runeliteMousePoint.getX(), runeliteMousePoint.getY() - 20);
        }
//		}

        Point2D.Float mouse = new Point2D.Float(runeliteMousePoint.getX(), runeliteMousePoint.getY());

        return null;
    }

	private void renderKqPhaseChangeTimer(Graphics2D graphics)
	{
//		NPC kq = plugin.getKq();
//		if (kq == null) return;
//		String kqOverheadText = plugin.getKqOverheadText();
//		if (kqOverheadText == null) return;
//
//		int zOffset = kq.getLogicalHeight() + ACTOR_OVERHEAD_TEXT_MARGIN;
//
//		Point textLocation = kq.getCanvasTextLocation(graphics, kqOverheadText, zOffset);
//
//		if (textLocation == null)
//		{
//			return;
//		}
//
//		OverlayUtil.renderTextLocation(graphics, textLocation, kqOverheadText, Color.PINK);
	}

	private void renderKqShortcutStallTimer(Graphics2D graphics)
	{
		if (plugin.kqShortcutStallTimer < 0) return;
		drawBigText(graphics, new WorldPoint(3506, 9505, 2), "" + plugin.kqShortcutStallTimer);
	}

	private void renderFairyRingStallTimer(Graphics2D graphics)
	{
		GameObject fairyRing = plugin.fairyRing;
		if (fairyRing == null) return;
		int fairyRingStallTimer = plugin.fairyRingStallTimer;
		if (fairyRingStallTimer < 0) return;

		WorldPoint worldLocation = fairyRing.getWorldLocation();
		String text = "" + fairyRingStallTimer;
		drawBigText(graphics, worldLocation, text);
	}

	private void drawBigText(Graphics2D graphics, WorldPoint worldLocation, String text)
	{
		LocalPoint lp = LocalPoint.fromWorld(client, worldLocation);
		Point canvasTextLocation = Perspective.getCanvasTextLocation(client, graphics, lp, text, 0);

		if (canvasTextLocation == null)
		{
			return;
		}

		Font font = graphics.getFont();
		graphics.setFont(FontManager.getRunescapeSmallFont().deriveFont(64f));
		OverlayUtil.renderTextLocation(graphics, canvasTextLocation, text, Color.PINK);
		graphics.setFont(font);
	}

	private void renderXarpusExhumeds(Graphics2D graphics)
	{
		for (Map.Entry<GroundObject, Integer> exhumed : plugin.getExhumeds().entrySet())
		{
			GroundObject exhumedGroundObject = exhumed.getKey();
			Rectangle bounds = exhumedGroundObject.getClickbox().getBounds();
			int x = (int) ((bounds.getMinX() + bounds.getMaxX()) / 2);
			int y = (int) ((bounds.getMinY() + bounds.getMaxY()) / 2);
			graphics.drawString("" + exhumed.getValue(), x - 10, y + 10);
		}
	}

	public static void main(String[] args)
    {
        int bluePhase = 0;
        int redPhase = 0;
        int grayPhase = 0;
        for (int i = 0; i < 200; i++)
        {
            int phase = i > 150 ? 3 : i > 100 ? 2 : i > 50 ? 1 : 0;
//            HydraState hydraState = getHydraAttackIndex(i, phase, 50, 100, 150);
//            System.out.println(i + " " + hydraState.prayerIsOdd);
//            if (i >= 13) {
//                System.out.println(i + " even " + hydraState.prayerIsOdd);
//            if (i >= 13) {
//                System.out.println(i + " even " + hydraState.prayerIsOdd);
//            } else {
//                System.out.println(i + " odd " + hydraState.prayerIsOdd);
//            }
        }
//        int attacks[] = new int[]{};
//        int i = 0;
//        for (int attack : attacks)
//        {
//            getHydraAttackIndex(i, 0, );
//        }
    }

    public static final class HydraState {
        boolean prayerIsOdd = false;
    }

    static HydraState getHydraAttackIndex(int hydraFightDuration, int hydraPhase, int hydraPhaseBlue, int hydraPhaseRed, int hydraPhaseGray)
    {
        return getHydraAttackIndex(hydraFightDuration, hydraPhase, hydraPhaseBlue, hydraPhaseRed, hydraPhaseGray, false);
    }
    static HydraState getHydraAttackIndex(int hydraFightDuration, int hydraPhase, int hydraPhaseBlue, int hydraPhaseRed, int hydraPhaseGray, boolean print)
    {
        HydraState hydraState = new HydraState();
        int attacks = 0;
        int grayAttacks = 0;
        if (hydraPhase >= 0) // green.
        {
            int greenPhaseTicks = hydraPhase == 0 ? MyStuff.hydraFightDuration : hydraPhaseBlue;
            attacks += (greenPhaseTicks - 1) / 6 + 1;
            if (greenPhaseTicks >= 17) attacks--;
            if (print) System.out.println("green phase has " + attacks + " attacks in " + greenPhaseTicks + " ticks.");
        }
        if (hydraPhase >= 1) // blue.
        {
            int bluePhaseTicks = (hydraPhase == 1 ? MyStuff.hydraFightDuration : hydraPhaseRed) - hydraPhaseBlue;
            attacks += Math.max(bluePhaseTicks - 3, 0) / 6;
            if (bluePhaseTicks >= 19) attacks--;
            if (print) System.out.println("blue phase has " + attacks + " attacks in " + bluePhaseTicks + " ticks.");
        }
        if (hydraPhase >= 2) // red.
        {
            int redPhaseTicks = (hydraPhase == 2 ? MyStuff.hydraFightDuration : hydraPhaseGray) - hydraPhaseRed;
            attacks += Math.min(3, Math.max(redPhaseTicks - 2, 0) / 6);
            // 147.
            if (redPhaseTicks >= 52) {
                attacks += (redPhaseTicks - 52) / 6;
            }
            if (print) System.out.println("red phase has " + attacks + " attacks in " + redPhaseTicks + " ticks.");
        }
        if (hydraPhase == 3) // gray.
        {
            int grayPhaseTicks = MyStuff.hydraFightDuration - hydraPhaseGray;
            grayAttacks += (grayPhaseTicks - 3) / 4 + 1;
            if (grayPhaseTicks >= 17) grayAttacks--;
        }
        if (print) System.out.println(MyStuff.hydraFightDuration + " " + attacks + " " + hydraPhase + " " + hydraPhaseBlue + " " + hydraPhaseRed + " " + hydraPhaseGray);

        hydraState.prayerIsOdd = (attacks / 3) % 2 == 0;
        if (grayAttacks > 0 && grayAttacks % 2 == 1) hydraState.prayerIsOdd = !hydraState.prayerIsOdd;
//        System.out.println(MyStuff.hydraFightDuration + " " + attacks + " " + hydraPhase + " " + hydraPhaseBlue + " " + hydraPhaseRed + " " + hydraPhaseGray + " " + hydraState.prayerIsOdd);
        return hydraState;
    }

    int hydraticks = 0;
    int hydraPhase = 0;
    int hydraPhaseBlue = -1;
    int hydraPhaseRed = -1;
    int hydraPhaseGray = -1;
    Boolean lastPrayerWasMage = null;

    int hydrageysertick = 0;

    public static void printCerbProgress()
    {
        if (true) return;

        int numHits = getNumHits(MyStuff.lastcerbanimation);
        System.out.println(" 1  2  3  4  5  6  7  8  9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32 33 34 35");
        System.out.println(" T                             T        S                 L  T           L        G     L");
        System.out.println(" T  _  _  _  _  _  _  _  _  _  T  _  _  S  _  _  _  _  _  _  T  _  _  _  L  _  _  G  _  L");
        String s = "";
        for (int i = 0; i < numHits; i++) {
            s += "   ";
        }
        String s2 = new String(s);
        s += " ^";
        s2 += " |";
        System.out.println(s);
        System.out.println(s2);
    }


    public static int getNumHits(int numHitSplats)
    {
        // factor in that the triple attack hits 3 times but only counts as 1 attack.
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
