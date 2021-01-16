package net.runelite.client.plugins.barragehelper;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Provides;
import java.awt.Color;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;

import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.StatChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.NPCManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.Text;

@PluginDescriptor(
	name = "Barrage Helper",
	description = "Tells you which enemy in the stack has the most health remaining",
	tags = {"barrage", "burst", "slayer"}
)
public class BarrageHelperPlugin extends Plugin
{

	@Inject
	private Client client;

	@Inject
	private ScheduledExecutorService executor;

	@Inject
	private NPCManager npcManager;

	@Inject
	private BarrageHelperConfig config;

	private static final Set<MenuAction> NPC_MENU_ACTIONS = ImmutableSet.of(MenuAction.NPC_FIRST_OPTION, MenuAction.NPC_SECOND_OPTION,
		MenuAction.NPC_THIRD_OPTION, MenuAction.NPC_FOURTH_OPTION, MenuAction.NPC_FIFTH_OPTION, MenuAction.SPELL_CAST_ON_NPC,
		MenuAction.ITEM_USE_ON_NPC);

	private Map<NPC, Integer> npcHealthCache = new HashMap<>();
	public static Map<NPC, Integer> predictedNpcHealthCache = new HashMap<>();

	private List<String> npcWhiteList = Collections.emptyList();

	@Provides
	BarrageHelperConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BarrageHelperConfig.class);
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied hitsplatApplied)
	{
		Actor actor = hitsplatApplied.getActor();
		if (actor instanceof NPC)
		{
			// This applies if an npc is hit and its healthbar disappears before you mouse it over.
			npcHealthCache.putIfAbsent((NPC) actor, -1);
		}
	}

	private final class TileAndHealth
	{
		public final MenuEntry entry;
		public final Integer health;

		private TileAndHealth(MenuEntry entry, Integer health)
		{
			this.entry = entry;
			this.health = health;
		}
	}

	public static void main(String[] args) {
		String craftCape = "Drop <col=ff9040>Crafting cape(t)";
		System.out.println(
				craftCape.replaceAll("Crafting cape\\(t\\)", "best bank teleport!")

		);
	}

	int lastCacheSize = 0;
	@Subscribe(priority = 10f)
	public void onClientTick(ClientTick event)
	{
		if (client.getGameState() != GameState.LOGGED_IN || client.isMenuOpen()) return;

//		if (lastCacheSize != npcHealthCache.size()) System.out.println("cache size: " + npcHealthCache.size()); lastCacheSize = npcHealthCache.size();

		Set<NPC> npcs = npcHealthCache.keySet();
		Iterator<NPC> iterator = npcs.iterator();
		while (iterator.hasNext())
		{
			NPC npc = iterator.next();
			if (npc.isDead() || npcManager.getNpcInfo((npc.getId())) == null) {
				iterator.remove();
				predictedNpcHealthCache.remove(npc);
			} else {
				Integer health = getHealth(npc);
				if (health != null && health != -1) {
					npcHealthCache.put(npc, health);
				}
			}
		}

		Map<WorldPoint, TileAndHealth> highestHealth = new HashMap<>();
		for (MenuEntry menuEntry : client.getMenuEntries())
		{
			if (!NPC_MENU_ACTIONS.contains(MenuAction.of(menuEntry.getType()))) continue;

			NPC npc = client.getCachedNPCs()[menuEntry.getIdentifier()];

			boolean npcIsOnWhitelist = npcWhiteList.stream().filter(npcName -> menuEntry.getTarget().toLowerCase().contains(npcName.toLowerCase())).findAny().isPresent();

			WorldPoint worldLocation = npc.getWorldLocation();

			if (config.colorMenuEntriesByTile() && npcIsOnWhitelist)
			{
				final Color tileColor = getColorForTile(worldLocation);
				menuEntry.setTarget(ColorUtil.prependColorTag(Text.removeTags(menuEntry.getTarget()), tileColor));
			}

			Integer health = npcHealthCache.get(npc);
//			if (health == null) health = getHealth(npc);
			// This only applies if you enter an area with an enemy that already has an hp bar over its head, and you mouseover it.
			// TODO test if this is even possible.
//				System.out.println("npc added is " + npc);
//				npcHealthCache.put(npc, health);

			if (
					(config.showHealthOnAllEnemies() || npcIsOnWhitelist) &&
					config.healthMarkingStyle() != BarrageHelperConfig.HealthMarkingStyle.OFF &&
					health != null
			) {
				if (config.hideNpcLevels()) menuEntry.setTarget(menuEntry.getTarget().replaceAll("\\(level-\\d*\\)", ""));
				menuEntry.setTarget(menuEntry.getTarget() + " " + getHealthString(npc));
			}

			if (health == null) continue;

			if (config.markHighestHealthTarget() && npcIsOnWhitelist && health != null && health > highestHealth.getOrDefault(worldLocation, new TileAndHealth(null, -1)).health)
			{
				highestHealth.put(worldLocation, new TileAndHealth(menuEntry, health));
			}
		}
		for (TileAndHealth value : highestHealth.values())
		{
			MenuEntry menuEntry = value.entry;
			menuEntry.setTarget("--" + menuEntry.getTarget() + "--");
		}

		for (MenuEntry menuEntry : client.getMenuEntries())
		{
			String target = menuEntry.getTarget();
			target = target
					.replaceAll("Cow", "Moomoo UwU")
					.replaceAll("General Graardor", "General Garage Door")
					.replaceAll("Crafting", "Glassblowing")
					.replaceAll("Verzik Vitur", "The Fat Lady")
					.replaceAll("Pestilent Bloat", "Slob")
					.replaceAll("Sotetseg", "Daddy Big Fat Melees")
					.replaceAll("The Maiden of Sugadinti", "They're called spiders, not crabs")
			;
			menuEntry.setTarget(target);
        }
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (event.getGroup().equals("barrageHelper"))
		{
			executor.execute(this::reset);
		}
	}

	private void reset() {
		npcWhiteList = Text.fromCSV(config.getNpcWhitelist());
	}

	private String getHealthString(NPC npc)
	{
		Integer maxHealth = npcManager.getHealth(npc.getId());
		Integer health = npcHealthCache.get(npc);
		if (maxHealth == null && health == null)
			System.out.println("maxHealth is null.");
//		if (health == null || maxHealth == null) return "";
		String s = "";
		// TODO do not show up on npcs that don't have hp or levels.
		switch (config.healthMarkingStyle()) {
			case OFF:
				return "";
			case NUMERIC_SHORT:
				s = (health == null) ? "?" : "" + health;
				break;
			case NUMERIC:
				s = "(" + ((health == null) ? "?" : "" + health) + "/" + maxHealth + ")";
				break;
			case ASCII_ART:
				if (health == null) s = "?";
				else if (health == 0) return "";
				else if (health == maxHealth) s = "++++++";
				else
				{
					int healthBarLength = (int) (Math.ceil((float) health / maxHealth * 4) + 1);
					for (int i = 0; i < healthBarLength; i++)
					{
						s += "+";
					}
				}
				break;
		}
		return ColorUtil.prependColorTag(s, (health == null) ? Color.WHITE : Color.RED); // shouldn't happen.
	}

	private Integer getHealth(NPC npc)
	{
		int ratio = npc.getHealthRatio();
		Integer health = npcManager.getHealth(npc.getId());
		if (health == null)
			System.out.println("is null");
		return (ratio == -1) ? null : getHealth(ratio, npc.getHealthScale(), health);
	}

	// Taken from OpponentInfoOverlay.
	private int getHealth(int ratio, int scale, int lastMaxHealth)
	{
		// This is the reverse of the calculation of healthRatio done by the server
		// which is: healthRatio = 1 + (healthScale - 1) * health / maxHealth (if health > 0, 0 otherwise)
		// It's able to recover the exact health if maxHealth <= healthScale.
		int minHealth = 1;
		int maxHealth;
		if (scale > 1)
		{
			if (ratio > 1)
			{
				// This doesn't apply if healthRatio = 1, because of the special case in the server calculation that
				// health = 0 forces healthRatio = 0 instead of the expected healthRatio = 1
				minHealth = (lastMaxHealth * (ratio - 1) + scale - 2) / (scale - 1);
			}
			maxHealth = (lastMaxHealth * ratio - 1) / (scale - 1);
			if (maxHealth > lastMaxHealth)
			{
				maxHealth = lastMaxHealth;
			}
		}
		else
		{
			// If healthScale is 1, healthRatio will always be 1 unless health = 0
			// so we know nothing about the upper limit except that it can't be higher than maxHealth
			maxHealth = lastMaxHealth;
		}
		// Take the average of min and max possible healths
		return (minHealth + maxHealth + 1) / 2;
	}

	private static final Color arr[] = {
		Color.getHSBColor(126 / 360f, .97f, .67f),
		Color.getHSBColor(61 / 360f, .95f, .90f),
		Color.getHSBColor(35 / 360f, .95f, .90f),
		Color.getHSBColor(3 / 360f, .95f, .90f),
		Color.getHSBColor(177 / 360f, .95f, .90f),
		Color.getHSBColor(213 / 360f, .95f, .90f),
		Color.getHSBColor(303 / 360f, .95f, .90f),
		Color.getHSBColor(126 / 360f, .97f * .5f, .67f),
		Color.getHSBColor(61 / 360f, .95f * .5f, .90f),
		Color.getHSBColor(35 / 360f, .95f * .5f, .90f),
		Color.getHSBColor(3 / 360f, .95f * .5f, .90f),
		Color.getHSBColor(177 / 360f, .95f * .5f, .90f),
		Color.getHSBColor(213 / 360f, .95f * .5f, .90f),
		Color.getHSBColor(303 / 360f, .95f * .5f, .90f),
	};

	private Color getColorForTile(WorldPoint worldLocation)
	{
		int colorIndex = (worldLocation.getX() * 37 + worldLocation.getY()) % arr.length;
		return arr[colorIndex];
	}

	private int lastDefenceXp = -1;
	@Subscribe
	public void onStatChanged(StatChanged statChanged) {
		if (statChanged.getSkill() != Skill.DEFENCE) return;

		int xp = statChanged.getXp();
		int diff = xp - lastDefenceXp;
		int damageDone = diff / 12;

		Actor interacting = client.getLocalPlayer().getInteracting();
		String name = (interacting == null) ? "null" : interacting.getName();
		String id = "not an npc";
		if (interacting instanceof NPC) {
			id = Integer.toString(((NPC) interacting).getId());
		}
//		System.out.println("damage done: " + damageDone + " against " + name + " (" + id + ")");
		lastDefenceXp = xp;

		if (!(interacting instanceof NPC)) return;

		NPCComposition composition = client.getNpcDefinition(((NPC) interacting).getId());
		Integer maxHealth = npcManager.getHealth(((NPC) interacting).getId());
		if (maxHealth == null) {
			System.out.println("health was null! " + ((NPC) interacting).getId());
			return;
		}

		Integer predictedHealth = predictedNpcHealthCache.get(interacting);
		if (predictedHealth == null) {
			predictedHealth = maxHealth;
		}
		System.out.println("modifying health of " + name + " " + ((NPC) interacting).getId() + " from " + predictedHealth + " to " + (predictedHealth - damageDone));
//		if (predictedHealth - damageDone <= 0) {
//
//		}
		predictedNpcHealthCache.put((NPC) interacting, predictedHealth - damageDone);
	}

}
//	private Map<WorldPoint, String> lastModifiedOriginalMenuEntryTargetText = new HashMap<>();
//	private Map<WorldPoint, MenuEntry> lastModifiedMenuEntry = new HashMap<>();
//	private Map<WorldPoint, Integer> highestHealth = new HashMap<>();

//	static {
//		final PrintStream out = System.out;
//		final PrintStream err = System.err;
//		PrintStream myStream = new PrintStream(System.out) {
//    @Override
//    public void println(String x) {
//    	if (x != null && x.toLowerCase().contains("exception")) new Exception().printStackTrace((PrintStream) out);
//        super.println(System.currentTimeMillis() + ": " + x);
//    }
//};
//System.setOut(myStream);
//
//		PrintStream myStream2 = new PrintStream(System.err) {
//			@Override
//			public void println(String x) {
//				if (x != null && x.toLowerCase().contains("exception")) new Exception().printStackTrace(err);
//				super.println(System.currentTimeMillis() + ": " + x);
//			}
//		};
//		System.setErr(myStream2);
//	}
//

//	@Subscribe
//	public void onMenuEntryAdded(MenuEntryAdded event)
//	{
//		if (!NPC_MENU_ACTIONS.contains(MenuAction.of(event.getType()))) return;
//
//		NPC npc;
//		try
//		{
//			npc = client.getCachedNPCs()[event.getIdentifier()];
//		} catch (Exception e) {
//			e.printStackTrace(System.out);
//			return;
//		}
//
//		boolean npcIsOnWhitelist = npcWhiteList.stream().filter(npcName -> event.getTarget().toLowerCase().contains(npcName.toLowerCase())).findAny().isPresent();
//
//		MenuEntry[] menuEntries = client.getMenuEntries();
//		MenuEntry menuEntry = menuEntries[menuEntries.length - 1];
//
//		WorldPoint worldLocation = npc.getWorldLocation();
//
//		if (config.colorMenuEntriesByTile() && npcIsOnWhitelist)
//		{
//			final Color tileColor = getColorForTile(worldLocation);
//			menuEntry.setTarget(ColorUtil.prependColorTag(Text.removeTags(menuEntry.getTarget()), tileColor));
//		}
//
//		Integer health = npcHealthCache.get(npc);
//		if (health == null) {
//			health = getHealth(npc);
//			if (health == null) return;
//			// This only applies if you enter an area with an enemy that already has an hp bar over its head, and you mouseover it.
//			// TODO test if this is even possible.
//			System.out.println("npc added is " + npc);
//			npcHealthCache.put(npc, health);
//		}
//
//		if (config.showHealthOnAllEnemies() || npcIsOnWhitelist) {
//			if (config.hideNpcLevels()) menuEntry.setTarget(menuEntry.getTarget().replaceAll("  \\(level-\\d*\\)", ""));
//			if (health != null) menuEntry.setTarget(menuEntry.getTarget() + " " + ColorUtil.prependColorTag(" " + getHealthString(npc), Color.RED));
//		}
//
//		if (config.markHighestHealthTarget() && npcIsOnWhitelist)
//		{
//			// Unfortunately runelite does not provide a method that is called only after all menu entries have been added.
//			// This is why this section seems unnecessarily complicated, updating menu entries as they come in and then
//			// restoring them if that menu entry turned out not to be the highest health target.
//			if (health != null && health > highestHealth.getOrDefault(worldLocation, -1))
//			{
//				if (lastModifiedMenuEntry.get(worldLocation) != null)
//				{
//					lastModifiedMenuEntry.get(worldLocation).setTarget(lastModifiedOriginalMenuEntryTargetText.get(worldLocation));
//				}
//				highestHealth.put(worldLocation, health);
//				lastModifiedMenuEntry.put(worldLocation, menuEntry);
//				lastModifiedOriginalMenuEntryTargetText.put(worldLocation, menuEntry.getTarget());
//				menuEntry.setTarget("--" + menuEntry.getTarget() + "--");
//			}
//		}
//	}

//	public static void main(String[] args)
//	{
//		JFrame frame = new JFrame();
//		JPanel panel = new JPanel() {
//			@Override
//			protected void paintComponent(Graphics g)
//			{
//				super.paintComponent(g);
//
//				for (int i = 0; i < 50; i++)
//				{
//					for (int j = 0; j < 50; j++)
//					{
//						final Color tileColor = arr[(i * 37 + j) % arr.length];
//						g.setColor(tileColor);
//						g.fillRect(i * 50, j * 50, 50, 50);
//					}
//				}
//			}
//		};
//		frame.add(panel);
//		frame.setSize(500, 500);
//		frame.setVisible(true);
//	}

