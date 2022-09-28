package net.runelite.client.plugins.mystuff;

import static java.lang.Math.min;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.api.widgets.JavaScriptCallback;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.RequestFocusType;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.externalplugins.ExternalPluginManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemVariationMapping;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.screenmarkers.ui.ScreenMarkerPluginPanel;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;
import net.runelite.http.api.item.ItemStats;

import javax.inject.Inject;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.runelite.api.widgets.WidgetInfo.*;

@PluginDescriptor(
        name = "mystuff",
        description = "mystuff",
        enabledByDefault = false
)
public class MyStuff extends Plugin
{
    public static final int ZULRAH_GREEN_ID = 2042;
    public static final int ZULRAH_RED_ID = 2043;
    public static final int ZULRAH_TANZ_ID = 2044;
    public static int lastcerbanimation;
    public static int lastcerbanimationGameTick;
    private static int ignoreNext;
    @Inject
    private Notifier notifier;

    @Inject
    private ClientThread clientThread2;
    public static ClientThread clientThread = null;

    @Inject
    private Client client;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private MyStuffOverlay myStuffOverlay;

    public static final Map<Skill, Integer> xp = new HashMap<>();
    public static final Map<Skill, Integer> levels = new HashMap<>();

    public MyStuff() {
        clientThread = clientThread2;
    }

    public static volatile int playerattackcounter = 0;
    public static volatile int hunllefattackcounter = 0;
    public static volatile int lastplayerattack = -1;

    public static int lastZulrahDown = -1;

    public String getMousePointerText() {
    	if (kqPhaseChangeTimer >= 0) {
			return "" + kqPhaseChangeTimer;
		} else if (kqShortcutStallTimer >= 0) {
			return "" + kqShortcutStallTimer;
		} else if (fairyRingStallTimer >= 0) {
    		return "" + fairyRingStallTimer;
		}
    	return "" + ticks;
	}

	@Subscribe
	public void onActorDeath(ActorDeath actorDeath)
	{
		if (actorDeath.getActor().equals(client.getLocalPlayer())) {
			ItemContainer itemContainer = client.getItemContainer(InventoryID.INVENTORY);
			int ppotdoses = 0;
			int restoredoses = 0;
			int brewdoses = 0;
			int scbdoses = 0;
			int divinescbdoses = 0;
			Map<String, Integer> items = new HashMap<>();
			for (Item item : itemContainer.getItems())
			{
				ItemStats itemStats = itemManager.getItemStats(item.getId(), false);
				if (itemStats != null && !itemStats.isEquipable()) {
					System.out.println("item " + itemManager.getItemComposition(item.getId()).getName());
					switch (item.getId()) {
						case ItemID.SARADOMIN_BREW4:
							brewdoses += 1;
						case ItemID.SARADOMIN_BREW3:
							brewdoses += 1;
						case ItemID.SARADOMIN_BREW2:
							brewdoses += 1;
						case ItemID.SARADOMIN_BREW1:
							brewdoses += 1;
							break;

						case ItemID.SUPER_RESTORE4:
							restoredoses++;
						case ItemID.SUPER_RESTORE3:
							restoredoses++;
						case ItemID.SUPER_RESTORE2:
							restoredoses++;
						case ItemID.SUPER_RESTORE1:
							restoredoses++;
							break;

						case ItemID.SUPER_COMBAT_POTION4:
							scbdoses++;
						case ItemID.SUPER_COMBAT_POTION3:
								scbdoses++;
						case ItemID.SUPER_COMBAT_POTION2:
								scbdoses++;
						case ItemID.SUPER_COMBAT_POTION1:
								scbdoses++;
							break;

						case ItemID.DIVINE_SUPER_COMBAT_POTION4:
							divinescbdoses++;
						case ItemID.DIVINE_SUPER_COMBAT_POTION3:
							divinescbdoses++;
						case ItemID.DIVINE_SUPER_COMBAT_POTION2:
							divinescbdoses++;
						case ItemID.DIVINE_SUPER_COMBAT_POTION1:
							divinescbdoses++;
							break;

						case ItemID.PRAYER_POTION4:
							ppotdoses++;
						case ItemID.PRAYER_POTION3:
								ppotdoses++;
						case ItemID.PRAYER_POTION2:
								ppotdoses++;
						case ItemID.PRAYER_POTION1:
								ppotdoses++;
							break;

						default:
							String name = itemManager.getItemComposition(item.getId()).getName();
							Integer value = items.get(name);
							if (value == null) value = 0;
							value++;
							items.put(name, value);
							break;
					}
				}
			}
			System.out.println("brews: " + brewdoses);
			System.out.println("restores: " + restoredoses);
			System.out.println("ppots: " + ppotdoses);
			System.out.println("scbs: " + scbdoses);
			System.out.println("divine scbs: " + divinescbdoses);
			System.out.println(items);
		}

		Widget widget = client.getWidget(735, 17);
		for (int i = 0; i < widget.getDynamicChildren().length; i += 1)
		{

			System.out.println(i + " " + widget.getDynamicChildren()[i].getText());
		}

		int i = 0;
		for (Widget dynamicChild : widget.getDynamicChildren())
		{
			dynamicChild.setOriginalWidth(10);
			dynamicChild.setOriginalHeight(10);
			dynamicChild.setOriginalX(i * 20);
			dynamicChild.revalidate();
			i++;
		}
	}

    @Subscribe
    public void onAnimationChanged(AnimationChanged animationChanged)
    {
        Actor actor = animationChanged.getActor();
        if (client.getLocalPlayer() == actor)
		{
        	if (client.getLocalPlayer().getAnimation() == 424)
			{
				ticks = 0;
			}
        	else if (client.getLocalPlayer().getAnimation() == 3265) {
				fairyRingStallTimer = 6;
			}
			else if (client.getLocalPlayer().getAnimation() == 2594) {
				kqShortcutStallTimer = 10;
			}
		}
        if (actor.getName() != null && actor.getName().toLowerCase().contains("kalphite queen")) {
        	if (actor.getAnimation() == 6242) {
        		kqPhaseChangeTimer = 22;
			}
		}
		int animation = actor.getAnimation();
        if ("zulrah".equalsIgnoreCase(actor.getName())) {
            if (actor.getAnimation() == 5072) {
                lastZulrahDown = client.getTickCount();
                System.out.println("setting lastZulrahDown to " + lastZulrahDown);
            }
        }
        else if ("corrupted hunllef".equalsIgnoreCase(actor.getName())) {

//            8419 (attack)
//                    8418 (stomp)
//                    8753 (swap)
//                    8754 (swap to mage)
//            8755 (swap to range)
            if (animation == 8419 || animation == 8418) { // attack or stomp
                hunllefattackcounter++;
                hunllefattackcounter %= 4;
            } else if (animation == 8753) { // ????
//                System.out.println("??????????????????? 8753");
            } else if (animation == 8754 || animation == 8755) { // swap to mage or range.
                hunllefattackcounter = 0;
            }
        } else if ("cerberus".equalsIgnoreCase(actor.getName())) {
            if (animation == 4490 || animation == 4491) {
                incrementLastCerbAnimation();
                lastcerbanimationGameTick = client.getTickCount();
//                System.out.println(lastcerbanimation + " " + client.getTickCount());
                MyStuffOverlay.printCerbProgress();
            }
        } else if (actor == client.getLocalPlayer()) {
//            System.out.println("is local player " + actor.getAnimation());
            if (animation == 426 || animation == 440 || animation == 1167) {
//                if (lastPlayerAnimation != animation) {
//                    playerattackcounter = 0;
//                }
                playerattackcounter++;
                lastPlayerAnimation = animation;
            }
        }
    }

    public static int countdowntimer = 0;
    public static int cerbfighttimer = 0;
    public static String countingdownto = "";
    private void incrementLastCerbAnimation() {
    	overlayManager.add(new Overlay()
		{
			@Override
			public Dimension render(Graphics2D graphics)
			{
				Widget equippedFragmentsWidget = client.getWidget(735, 35);
				for (int i = 0; i < equippedFragmentsWidget.getDynamicChildren().length; i += 6)
				{
					Widget fragmentBackground = equippedFragmentsWidget.getDynamicChildren()[i + 1];
					graphics.drawString("bla", fragmentBackground.getCanvasLocation().getX(), fragmentBackground.getCanvasLocation().getY());
				}
				return null;
			}
		});
        lastcerbanimation++;
        if (lastcerbanimation == 1) {
//            System.out.println("resetting cerbfighttimer");
            cerbfighttimer = client.getTickCount();
            countdowntimer = client.getTickCount() + 67;
            countingdownto = "triple (2)";
        } else if (lastcerbanimation == 13) {
            countdowntimer = client.getTickCount() + 24;
            countingdownto = "start";
        } else if (lastcerbanimation == 18) {
            countdowntimer = client.getTickCount() + 36;
            countingdownto = "lava (1)";
        } else if (lastcerbanimation == 24) {
            countdowntimer = client.getTickCount() + 5;
            countingdownto = "triple (3)";
        } else if (lastcerbanimation == 25) {
            countdowntimer = client.getTickCount() + 30;
            countingdownto = "lava";
        }
    }

    private boolean hitsplatTakenThisTick = false;

    @Subscribe
    public void onHitsplatApplied(HitsplatApplied hitsplatApplied)
    {
//        System.out.println("hitsplatapplied: " + Thread.currentThread().getName());
        if (client.getLocalPlayer().equals(hitsplatApplied.getActor()) && hitsplatApplied.getHitsplat().getHitsplatType() != HitsplatID.POISON && hitsplatApplied.getHitsplat().getAmount() != 10) {
//            System.out.println("is " + hitsplatTakenThisTick);
            if (!hitsplatTakenThisTick) {
//                System.out.println(client.getTickCount() + " hydraticks++");
                myStuffOverlay.hydraticks++;
                if (myStuffOverlay.hydraticks == 2) {
                    myStuffOverlay.firstPrayerWasMage = client.isPrayerActive(Prayer.PROTECT_FROM_MAGIC);
                    System.out.println("set firstPrayerWasMage to " + myStuffOverlay.firstPrayerWasMage + " " + hydraFightDuration);
                    System.out.println("setting hydraFightDuration from " + hydraFightDuration);
                    hydraFightDuration = 8;
                }
            }
//            System.out.println("setting to true");
            hitsplatTakenThisTick = true;
        }
        Actor actor = hitsplatApplied.getActor();
//        if (actor == client.getLocalPlayer()) ticks = 0;
//		if (actor.getName().toLowerCase().contains("cerberus")) {
//			LootManager.cerbhp -= hitsplatApplied.getHitsplat().getAmount();
//		}
        if (actor == client.getLocalPlayer()) {
            if (MyStuff.ignoreNext > 0)
            {
                MyStuff.ignoreNext--;
            } else
            {
//                MyStuff.lastcerbanimation++;
                MyStuffOverlay.printCerbProgress();
            }
        }
        if (actor instanceof NPC && hitsplatApplied.getHitsplat().isMine())
        {
            if (expectdamage > 500) expectdamage = 0; // we don't want our entire hitpoints xp to be taken as an xp drop, lol.
            if (expectdamage == 0)
            {
                myStuffOverlay.eatDelay = 0;
            }
            if (
                    expectdamage == 0 && hitsplatApplied.getHitsplat().getAmount() == 0 ||
                            expectdamage != 0 && Math.abs(hitsplatApplied.getHitsplat().getAmount() - expectdamage) <= 1
            )
            {
                myStuffOverlay.eatDelay += 4;
                if (myStuffOverlay.eatDelay < 4) {
                    myStuffOverlay.eatDelay = 4;
                }
                expectdamage = 0;
            }
        }
    }

    int expectdamage = 0;

    public static int ticks = 0;

    @Subscribe
    public void onStatChanged(StatChanged statChanged)
    {
        Skill skill = statChanged.getSkill();

        int newXp = statChanged.getXp();
        int lastXp = xp.getOrDefault(skill, 0);
        if (newXp != lastXp)
        {
//            MyLog.xpgain(skill, newXp - lastXp, newXp);
            if (skill == Skill.HITPOINTS) {
                if (expectdamage != 0) {
                }
                myStuffOverlay.eatDelay = 0;
                expectdamage = (int) Math.round((newXp - lastXp) / 1.333);
            } else if (skill == Skill.SLAYER) {
//                MusicPlugin.counter = 0;
                lastcerbanimation = 0;
                cerbfighttimer = client.getTickCount();
//                System.out.println("slayer xp: " + client.getTickCount());
                countdowntimer = client.getTickCount();
//                LootManager.cerbhp = 600;
            } else if (skill == Skill.MAGIC) {
            }
            if (skill != Skill.MAGIC && newXp - lastXp != 58)
			{
				ticks = 0;
			}
        }
        xp.put(skill, newXp);

        int newLevel = statChanged.getBoostedLevel();
        int lastLevel = levels.getOrDefault(skill, 0);
        if (newLevel != lastLevel)
        {
//            MyLog.skillchange(skill, newLevel - lastLevel, newLevel);
        }
        levels.put(skill, newLevel);

        if (skill == Skill.SLAYER) {
//            System.out.println("resetting hydra ticks");
            myStuffOverlay.hydraticks = 0;
            myStuffOverlay.hydraPhase = 0;
            myStuffOverlay.lastPrayerWasMage = null;
        }
    }

    private MenuEntry[] lastMenuEntries = null;

    private static final Pattern fightDurationPattern = Pattern.compile("Fight duration: ([\\d:]+). Personal best: ([\\d:]+)");
    private static final Pattern fightDurationPattern_PERSONAL_BEST_VARIANT = Pattern.compile("Fight duration: ([\\d:]+). Personal best: ([\\d:]+)");

    @Inject
	EventBus eventBus;

    @Subscribe
    public void onChatMessage(ChatMessage event)
    {
    		if ("Your attempt to steal goes unnoticed.".equals(event.getMessage())) {
    			hydraFightDuration = -1;
				Skill skill = Skill.valueOf("THIEVING");
				int xp = 1;

				int totalXp = client.getSkillExperience(skill) + xp;
				int level = min(Experience.getLevelForXp(totalXp), 99);

				client.getBoostedSkillLevels()[skill.ordinal()] = level;
				client.getRealSkillLevels()[skill.ordinal()] = level;
				client.getSkillExperiences()[skill.ordinal()] = totalXp;

				client.queueChangedSkill(skill);

				StatChanged statChanged = new StatChanged(
					skill,
					totalXp,
					level,
					level
				);
				eventBus.post(statChanged);
			}

			if (event.getMessage().contains("You fail to pick")) {
				hydraFightDuration = 7 + client.getTickCount();
				System.out.println("hydra fight duration " + hydraFightDuration);
				ticks = 0;
			}

			if (event.getMessage().contains("Your dodgy necklace protects you.")) {
				System.out.println("dodgy necklace detected.");
				hydraFightDuration = -1;
				lastHydraId = 1000;
				Skill skill = Skill.valueOf("THIEVING");
				int xp = 1;

				int totalXp = client.getSkillExperience(skill) + xp;
				int level = min(Experience.getLevelForXp(totalXp), 99);

				client.getBoostedSkillLevels()[skill.ordinal()] = level;
				client.getRealSkillLevels()[skill.ordinal()] = level;
				client.getSkillExperiences()[skill.ordinal()] = totalXp;

				client.queueChangedSkill(skill);

				StatChanged statChanged = new StatChanged(
					skill,
					totalXp,
					level,
					level
				);
				eventBus.post(statChanged);
			}
//        if (event.getType() != ChatMessageType.GAMEMESSAGE)
//        {
//            return;
//        }

        String chatMsg = Text.removeTags(event.getMessage()); // remove color and linebreaks
//        System.out.println("chatMsg: " + chatMsg);
//        System.out.println(event.getType());

        if (chatMsg.contains("The Alchemical Hydra becomes enraged!"))
        {

        }
        else if (chatMsg.contains("The chemicals neutralise the Alchemical Hydra's defences!"))
        {
//            System.out.println("geyser chat message");
            myStuffOverlay.hydrageysertick = client.getTickCount();
        }
        else if (chatMsg.contains("The electricity temporarily paralyzes you!"))
        {
            myStuffOverlay.hydraticks--;
        }

        Matcher m = fightDurationPattern.matcher(chatMsg);
        if (m.matches()) {
            String time = m.group(1);
        }

//		if chatMsg
    }

    public GameObject fairyRing = null;
	public int fairyRingStallTimer = -1;

	public NPC kq = null;
	public int kqPhaseChangeTimer = -1;
	public String kqOverheadText = null;

	public WorldPoint kqShortcutTextLocation = null;
	public int kqShortcutStallTimer = -1;
	public String kqShortcutText = null;

	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
//		if (event.getNpc().getId() == 29495 || event.getNpc().getId() == 40779) {
//			System.out.println("despawn ring");
//			fairyRing = null;
//		}
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		if (event.getGameObject().getId() == 29495 || event.getGameObject().getId() == 40779) {
			System.out.println("spawn ring");
			fairyRing = event.getGameObject();
		}
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event)
	{
		if (event.getGameObject().getId() == 29495 || event.getGameObject().getId() == 40779) {
			System.out.println("despawn ring");
			fairyRing = null;
		}
	}

	private boolean constructionWidgetVisible = false;

    @Subscribe
    public void onWidgetLoaded(WidgetLoaded widgetLoaded) {
        if (widgetLoaded.getGroupId() == ADVENTURE_LOG.getGroupId()) {

            clientThread.invokeLater(() -> {
                Widget widget = client.getWidget(ADVENTURE_LOG.getGroupId(), 3);
//                System.out.println("widget loaded " + widget.getDynamicChildren());
//                System.out.println(widget.getDynamicChildren().length);
                int childToSwapWith = 0;
                for (Widget child : widget.getDynamicChildren()) {
//                    System.out.println("\t " + child.getText());
                    if (
                            child.getText().contains("The Diary of Tarn Razorlor")
                                    || child.getText().contains("Book o' Piracy")
                                    || child.getText().contains("Collection Log")
                    ) {
                        Widget toSwapWith = widget.getDynamicChildren()[childToSwapWith];
                        childToSwapWith++;

                        int originalY = toSwapWith.getOriginalY();
                        toSwapWith.setOriginalY(child.getOriginalY());
                        child.setOriginalY(originalY);
                        toSwapWith.revalidate();
                        child.revalidate();

                        child.setTextColor(0xff0000);
//                        System.out.println("setting color for child");
                        child.setOnMouseLeaveListener((JavaScriptCallback) e -> {
                            child.setTextColor(0xff0000);
                        });
                        child.revalidate();
                    }
                }
            });
        }
    }

    int bla = 1;
    @Subscribe
    public void onClientTick(ClientTick event)
    {
		for (NPC npc : client.getNpcs())
		{
//			if (npc.isDead()) {
//				npc.setAnimation(-1);
//				npc.setOverheadText("Guess I'll die.");
//			}
//
			if ("Quack!".equals(npc.getOverheadText())) {
				npc.setOverheadText("Quack quack, motherfucker.");
//				npc.setOverheadText("Yo bitch where's my money?");
			}
		}

		MenuEntry[] menuEntries = client.getMenuEntries();
        if (!Arrays.equals(menuEntries, lastMenuEntries))
        {
//            MyLog.mouseover(menuEntries);
        }
        lastMenuEntries = menuEntries;

        Widget constructionWidget = client.getWidget(458, 1);
        boolean b = constructionWidget != null && !constructionWidget.isHidden();
        if (b != constructionWidgetVisible) {
//                Event.UniqueIdEvent.Type id = b ? Event.UniqueIdEvent.Type.CONSTRUCTION_BUILD_MENU_VISIBLE : Event.UniqueIdEvent.Type.CONSTRUCTION_BUILD_MENU_HIDDEN;
//                System.out.println("logging " + id.name());
//                new Event.UniqueIdEvent(id).log();
            constructionWidgetVisible = b;
        }

        List<WidgetInfo> dialogs = new ArrayList<>();
        dialogs.add(DIALOG_OPTION);
        boolean optionsOpen = client.getWidget(DIALOG_OPTION) != null;
        boolean playerSpeaking = client.getWidget(DIALOG_PLAYER) != null;

        Widget npcNameWidget = client.getWidget(DIALOG_NPC_NAME);
        String npcName = (npcNameWidget == null) ? null : npcNameWidget.getText();
        Widget npcTextWidget = client.getWidget(DIALOG_NPC_TEXT);
        String npcText = (npcTextWidget == null) ? null : npcTextWidget.getText();
        Widget optionWidget = client.getWidget(DIALOG_OPTION.getGroupId(), 1);
        List<String> options = new ArrayList<>();
        if (optionWidget != null) {
            Widget[] dynamicChildren = optionWidget.getDynamicChildren();
            for (Widget dynamicChild : dynamicChildren) {
                options.add(dynamicChild.getText());
            }
            npcText = options.remove(0);
        }
//            if (npcText != null && !npcText.equals(lastNpcText))
//            {
//                if (npcText != null && npcText.toLowerCase().contains("your new task"))
//                {
//                    System.out.println("slayer task: " + System.currentTimeMillis() + " " + npcName + " " + npcText);
//                }
//                if (npcText != null && npcText.toLowerCase().contains("furnished"))
//                {
//                    System.out.println("construction contract: " + System.currentTimeMillis() + " " + npcName + " " + npcText);
//                }
//            }
//            lastNpcText = npcText;
//            Event.ChatDialogEvent cde = new Event.ChatDialogEvent(npcName, npcText, options);
//			System.out.println("npc name is " + npcName);
//            Event.UniqueIdEvent uniqueIdEvent = null;
        if (npcText != null)
        {
//				System.out.println("\"" + npcName + "\"" + " " + "\"" + npcText + "\"" + " " + "\"" + options.size() + " " + options);
            if (options.size() > 2 && options.get(0).contains("Fetch from bank: 24 x Teak plank"))
            {
//                    uniqueIdEvent = new Event.UniqueIdEvent(Event.UniqueIdEvent.Type.BUTLER_FETCH_FROM_BANK);
            }
//				else if (options.size() > 2 && options.get(0).contains("Please wait...")) { System.out.println("2"); }
            else if (npcName != null && npcName.contains("Demon butler") && npcText.contains("Master, I have returned with what you"))
            {
//                    uniqueIdEvent = new Event.UniqueIdEvent(Event.UniqueIdEvent.Type.BUTLER_RETURN);
            }
            else if (npcName != null && npcName.contains("Demon butler") && npcText.contains("Master, I have returned with what thou"))
            {
//                    uniqueIdEvent = new Event.UniqueIdEvent(Event.UniqueIdEvent.Type.BUTLER_RETURN_INVENTORY_FULL);
            }
            else if (options.size() > 2 && options.get(0).contains("Yes"))
            {
//                    uniqueIdEvent = new Event.UniqueIdEvent(Event.UniqueIdEvent.Type.CONSTRUCTION_REMOVE_DIALOG);
            }
//				else if (options.size() > 2 && options.get(0).contains("Please wait...")) { System.out.println("6"); }
        }
//            if (uniqueIdEvent == null) {
//                uniqueIdEvent = new Event.UniqueIdEvent(Event.UniqueIdEvent.Type.MAKE_ALL_INTERFACE_OPTION_CHOSEN);
//            }
//            if (!cde.similar(lastChatDialogEvent) && npcText != null && npcText.length() > 0) {
//                try {
//                    System.out.println("logging unique id " + uniqueIdEvent.id + npcName + " " + npcText + " " + options);
//                    uniqueIdEvent.log();
//					cde.log();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//            lastChatDialogEvent = cde;
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event)
    {
//        System.out.println("clicked: " + event.getMenuTarget() + " " + event.getMenuOption());
        if (Text.removeTags(event.getMenuTarget()).contains("Divine") && Text.removeTags(event.getMenuOption()).equalsIgnoreCase("drink")) {
            myStuffOverlay.hydraticks--;
        }
        if (Text.removeTags(event.getMenuTarget()).equalsIgnoreCase("alchemical door") && Text.removeTags(event.getMenuOption()).equalsIgnoreCase("quick-open")) {
            System.out.println("quick pass alchemical door");
            myStuffOverlay.hydraticks = 0;
            hydraFightDuration = 0;
            playerattackcounter = 0;
            hunllefattackcounter = 0;
        }
        if (Text.removeTags(event.getMenuTarget()).equalsIgnoreCase("barrier") && Text.removeTags(event.getMenuOption()).equalsIgnoreCase("quick-pass")) {
            System.out.println("quick pass barrier");
            playerattackcounter = 0;
            hunllefattackcounter = 0;
        }

        if (event.getId() == 11390) {
            SystemTray tray = SystemTray.getSystemTray();

            //If the icon is a file
            Image image = Toolkit.getDefaultToolkit().createImage("icon.png");
            //Alternative (if the icon is on the classpath):
            //Image image = Toolkit.getDefaultToolkit().createImage(getClass().getResource("icon.png"));

            TrayIcon trayIcon = new TrayIcon(image, "Tray Demo");
            //Let the system resize the image if needed
            trayIcon.setImageAutoSize(true);
            //Set tooltip text for the tray icon
            trayIcon.setToolTip("System tray icon demo");
            try {
                tray.add(trayIcon);
            } catch (AWTException e) {
                e.printStackTrace();
            }

            trayIcon.displayMessage("auto", "notification demo", TrayIcon.MessageType.INFO);
        }
//		System.out.println("clicked: " + event.getMenuTarget() + " " + event.getId() + " " + event.getActionParam());
        if (event.getMenuTarget().toLowerCase().contains("boat")) {

            System.out.println(client.getTickCount() + " zulrah: boat clicked");
            Plugin zulrahPlugin = getPlugin("ZulrahHelperPlugin");
            try
            {
                zulrahPlugin.getClass().getDeclaredMethod("reset").invoke(zulrahPlugin);
            }
            catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e)
            {
                e.printStackTrace();
            }
            setZulrahRotation(rotation_start);

            lastZulrahSeen = -1;
            lastZulrahLocation = null;
            lastZulrahAnimation = -1;
        }
        if (event.getMenuOption().contains("View House Options"))
            System.out.println("time to click view house options: " + (System.currentTimeMillis() - lastHouseLoad));
//        try {
//			System.out.println("menu option clicked " + event.getMenuOption() + "\"" + event.getMenuTarget() + "\"" + event.toString());
//            new Event.MenuOptionClickedEvent(event.getMenuOption() + " " + event.getMenuTarget()).log();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    private volatile int notifyin = -1;

    private static boolean shopOpen = false;
    private static List<Integer> lastShopState = Collections.emptyList();
    private static List<Integer> lastInventoryState = Collections.emptyList();
    //    private static Event.ChatDialogEvent lastChatDialogEvent = null;
    private static List<Integer> rawItemIds = Arrays.asList(new Integer[]{383, 7944, 13439, 3142, 1775, 11934});
    private static List<Integer> cookedItemIds = Arrays.asList(new Integer[]{3144, 385, 13441, 7946, 10980, 11936});
    private static Map<Integer, Integer> eatDelays = new HashMap<>();

    //    static {
//        eatDelays.put(3144, 2); // karambwans
//        eatDelays.put(13441, 3);
//        eatDelays.put(391, 3);
//        eatDelays.put(385, 3);
//        eatDelays.put(7946, 3);
//        eatDelays.put(379, 3);
//        eatDelays.put(373, 3);
//    }
    private enum ZulrahState
    {
        START_1(2042),
        MAGMA_2(2043), MAGMA_3(2044),
        MAGMA_NORTH_4(2042), MAGMA_NORTH_5(2043), MAGMA_NORTH_6(2044), MAGMA_NORTH_7(2042), MAGMA_NORTH_8(2044), MAGMA_NORTH_9(2042), MAGMA_NORTH_10(2043),
        MAGMA_EAST_4(2042), MAGMA_EAST_5(2044), MAGMA_EAST_6(2043), MAGMA_EAST_7(2042), MAGMA_EAST_8(2044), MAGMA_EAST_9(2042), MAGMA_EAST_10(2043),
        TANZ_2(2044), TANZ_3(2042), TANZ_4(2044), TANZ_5(2043), TANZ_6(2042), TANZ_7(2042), TANZ_8(2044), TANZ_9(2042), TANZ_10(2044), TANZ_11(2042), TANZ_12(2044),
        GREEN_2(2042), GREEN_3(2043), GREEN_4(2044), GREEN_5(2042), GREEN_6(2044), GREEN_7(2042), GREEN_8(2042), GREEN_9(2044), GREEN_10(2042), GREEN_11(2044);

        final int id;

        ZulrahState(int id) {
            this.id = id;
        }

        public boolean isTanzanite() {
            return id == 2044;
        }
    }

    private static final List<ZulrahState> rotation_start = new ArrayList<>();
    private static final List<ZulrahState> rotation_magma_pre = new ArrayList<>();
    private static final List<ZulrahState> rotation_magma_east = new ArrayList<>();
    private static final List<ZulrahState> rotation_magma_north = new ArrayList<>();
    private static final List<ZulrahState> rotation_tanz = new ArrayList<>();
    private static final List<ZulrahState> rotation_green = new ArrayList<>();
    static {
        rotation_start.add(ZulrahState.START_1);

        rotation_magma_pre.add(ZulrahState.MAGMA_2);
        rotation_magma_pre.add(ZulrahState.MAGMA_3);

        rotation_magma_east.add(ZulrahState.MAGMA_EAST_4);
        rotation_magma_east.add(ZulrahState.MAGMA_EAST_5);
        rotation_magma_east.add(ZulrahState.MAGMA_EAST_6);
        rotation_magma_east.add(ZulrahState.MAGMA_EAST_7);
        rotation_magma_east.add(ZulrahState.MAGMA_EAST_8);
        rotation_magma_east.add(ZulrahState.MAGMA_EAST_9);
        rotation_magma_east.add(ZulrahState.MAGMA_EAST_10);

        rotation_magma_north.add(ZulrahState.MAGMA_NORTH_4);
        rotation_magma_north.add(ZulrahState.MAGMA_NORTH_5);
        rotation_magma_north.add(ZulrahState.MAGMA_NORTH_6);
        rotation_magma_north.add(ZulrahState.MAGMA_NORTH_7);
        rotation_magma_north.add(ZulrahState.MAGMA_NORTH_8);
        rotation_magma_north.add(ZulrahState.MAGMA_NORTH_9);
        rotation_magma_north.add(ZulrahState.MAGMA_NORTH_10);

        rotation_tanz.add(ZulrahState.TANZ_2);
        rotation_tanz.add(ZulrahState.TANZ_3);
        rotation_tanz.add(ZulrahState.TANZ_4);
        rotation_tanz.add(ZulrahState.TANZ_5);
        rotation_tanz.add(ZulrahState.TANZ_6);
        rotation_tanz.add(ZulrahState.TANZ_7);
        rotation_tanz.add(ZulrahState.TANZ_8);
        rotation_tanz.add(ZulrahState.TANZ_9);
        rotation_tanz.add(ZulrahState.TANZ_10);
        rotation_tanz.add(ZulrahState.TANZ_11);
        rotation_tanz.add(ZulrahState.TANZ_12);

        rotation_green.add(ZulrahState.GREEN_2);
        rotation_green.add(ZulrahState.GREEN_3);
        rotation_green.add(ZulrahState.GREEN_4);
        rotation_green.add(ZulrahState.GREEN_5);
        rotation_green.add(ZulrahState.GREEN_6);
        rotation_green.add(ZulrahState.GREEN_7);
        rotation_green.add(ZulrahState.GREEN_8);
        rotation_green.add(ZulrahState.GREEN_9);
        rotation_green.add(ZulrahState.GREEN_10);
        rotation_green.add(ZulrahState.GREEN_11);
    }

    private List<ZulrahState> zulrahRotation = null;
    private int zulrahRotationIndex = -1;
    private void setZulrahRotation(List<ZulrahState> rotation) {
        zulrahRotation = rotation;
        zulrahRotationIndex = 0;
    }

    private ZulrahState getZulrahState() {
        return (zulrahRotation == null) ? null : zulrahRotation.get(zulrahRotationIndex);
    }

    private void selectOption(int choice) {
        choice--;
        Plugin zulrahPlugin = getPlugin("ZulrahHelperPlugin");
        Method selectOption = null;
        try
        {
            selectOption = zulrahPlugin.getClass().getDeclaredMethod("selectOption", int.class);
        }
        catch (NoSuchMethodException e)
        {
            e.printStackTrace();
        }
        selectOption.setAccessible(true);
        try
        {
            selectOption.invoke(zulrahPlugin, choice);
        }
        catch (IllegalAccessException e)
        {
            e.printStackTrace();
        }
        catch (InvocationTargetException e)
        {
            e.printStackTrace();
        }
    }

    private void advanceState(int color, WorldPoint worldPoint) {
        System.out.println("advance state " + color);
        ZulrahState zulrahState = getZulrahState();
        if (zulrahState == ZulrahState.START_1) {
            if (color == ZULRAH_GREEN_ID) {
                setZulrahRotation(rotation_green);
                selectOption(2);
            } else if (color == ZULRAH_RED_ID) {
                setZulrahRotation(rotation_magma_pre);
                selectOption(1);
            } else if (color == ZULRAH_TANZ_ID) {
                setZulrahRotation(rotation_tanz);
                selectOption(3);
            }
        } else if (zulrahState == ZulrahState.MAGMA_3) {
            System.out.println("zulrah location: " + worldPoint.getRegionID() + " " + worldPoint.getRegionX() + " " + worldPoint.getRegionY());
            if (worldPoint.getRegionID() == 9007 && worldPoint.getRegionX() == 28 && worldPoint.getRegionY() == 56) {
                setZulrahRotation(rotation_magma_north);
                selectOption(1);
            } else {
                setZulrahRotation(rotation_magma_east);
                selectOption(2);
            }
        } else {
            zulrahRotationIndex++;
            if (zulrahRotationIndex >= zulrahRotation.size()) {
                setZulrahRotation(rotation_start);
            }
        }

        System.out.println("zulrah state update: " + zulrahState + " -> " + getZulrahState());

    }

    private MessageNode chatMessage(String message) {
        return client.addChatMessage(ChatMessageType.GAMEMESSAGE, "bla", message, "bla");
    }

    private boolean makeAllInterfaceOpen = false;
    private boolean bankopen = false;
    private boolean lastTickPiety = false;
    private String lastNpcText = "";
    private int lastHydraId = -1;
    public static int hydraFightDuration = 0;
    @Subscribe
    public void onGameTick(GameTick event)
    {
    	fairyRingStallTimer--;
    	kqShortcutStallTimer--;
		kqPhaseChangeTimer--;

		if (hydraFightDuration == client.getTickCount()) {
			client.getLocalPlayer().setGraphic(726);
			client.getLocalPlayer().setSpotAnimFrame(0);
		}
    	if (lastHydraId == 1000)
		{
			lastHydraId = -1;
//			System.out.println("game tick." + " " + client.getNpcs().size());
			for (NPC npc : client.getNpcs())
			{
//				System.out.println("npc " + npc.getName() + " " + npc.getOverheadText());
							npc.setOverheadText("");
			}
		}
		Optional<NPC> hydra = client.getNpcs().stream().filter(npc ->
        {
            String name = npc.getName();
            if (name == null) return false;
            return name.toLowerCase().contains("alchemical");
        }).findAny();
        boolean hydraPresent = hydra.isPresent();
        int hydraId = hydraPresent ? hydra.get().getId() : -1;
        if (hydraPresent) {
            hydraFightDuration++;
            System.out.println(hydraFightDuration);
            if (lastHydraId == -1) {
                //start timer;
                hydraFightDuration = 0;
                System.out.println("bla");
                System.out.println("geyser tick on start: " + ((client.getTickCount() - myStuffOverlay.hydrageysertick) % 8));
            }

            if (hydraId != lastHydraId) {
                // first attack on tick 2.
                // 8615 - green.
                // 8616 - green -> blue. 70
                // 8619 - blue. 73. xpdrop on 74.
                // first attack on 79.
                // lightning on 97. attack on 103.
                // 8617 - blue -> red. 147.
                // first attack on 155.
                // third attack 167.
                // 8619 - red.
                // 146, 150.
                // clicked on 188.

                // 8618 - red -> gray. 199.
                // 8621 - gray. 202.
                // First attack 206.
                if (hydraId == 8616) {
                    System.out.println(hydraFightDuration + " blue phase transition");
                    myStuffOverlay.hydraPhase = 1;
                    myStuffOverlay.hydraPhaseBlue = hydraFightDuration;
                }
                else if (hydraId == 8617) {
                    System.out.println(hydraFightDuration + " red phase transition");
                    myStuffOverlay.hydraPhase = 2;
                    myStuffOverlay.hydraPhaseRed = hydraFightDuration;
                }
                else if (hydraId == 8618) {
                    System.out.println(hydraFightDuration + " gray phase transition");
                    myStuffOverlay.hydraPhase = 3;
                    myStuffOverlay.hydraPhaseGray = hydraFightDuration;
                }
            }
        }
//        System.out.println(MyStuffOverlay.getHydraAttackIndex(hydraFightDuration, myStuffOverlay.hydraPhase, myStuffOverlay.hydraPhaseBlue, myStuffOverlay.hydraPhaseRed, myStuffOverlay.hydraPhaseGray, true).prayerIsOdd);
        this.lastHydraId = hydraId;
//        System.out.println("gametick: " + Thread.currentThread().getName());
//        System.out.println(client.getTickCount() + " gametick");
//        System.out.println("setting to false");
        hitsplatTakenThisTick = false;
        if (client.isFriended("Zyli", false) && !client.isFriended("Applejack", false)) {
            chatMessage("Name available");
        }

        LocalPoint localLocation = client.getLocalPlayer().getLocalLocation();
        if (localLocation.getSceneX() == 52 && localLocation.getSceneY() == 58) {

//            System.out.println("hydra thing location whatever");
        };
        if (localLocation.getSceneX() == 51 && localLocation.getSceneY() == 58) {

//            System.out.println("hydra thing location whatever pre");
        };

        zulrahStuff();

        ticks++;
        clientThread = clientThread2;
//		for (int i = 0; i < 100; i++)
//		{
//			if (client.isKeyPressed(i)) {
//				System.out.println(i);
//			}
//		}

//        MyLog.logGameTick(client.getTickCount());
//        MyLog.logClientPosition();
//		LootManager.lastcerbanimation++;
        if (lastSpecTarget != null && lastSpecTarget.getOverheadText() != null && lastSpecTarget.getOverheadText() != "") {
//			lastSpecTarget.setOverheadText("some text");
        }

        if (client.isPrayerActive(Prayer.PIETY)) {
            if (!lastTickPiety) {
//                MyLog.logUnique(Event.UniqueIdEvent.Type.PIETY_ON);
                lastTickPiety = true;
            }
        } else {
            if (lastTickPiety) {
//                MyLog.logUnique(Event.UniqueIdEvent.Type.PIETY_OFF);
                lastTickPiety = false;
            }
        }

//        log.info("wayfairshelf {}", client.isFriended("wayfairshelf", false));
//        log.info("trapsisntgay {}", client.isFriended("trapsisntgay", false));
//        log.info("no gun ctrl {}", client.isFriended("no gun ctrl", false));
//        log.info("atfsucksdick {}", client.isFriended("atfsucksdick", false));
//        log.info("Zyli {}", client.isFriended("Zyli", false));
//        log.info("Applejack {}", client.isFriended("Applejack", false));
//        log.info("notfriended {}", client.isFriended("notfriended", false));
//
        myStuffOverlay.eatDelay--;
        myStuffOverlay.lastGameTickMillis = System.currentTimeMillis();

        if (notifyin-- == 0) {
            notifier.notify("cooking done");
        }

//        MyLog.playerLocation(client.getLocalPlayer().getWorldLocation());

        Widget bankWidget = client.getWidget(WidgetInfo.BANK_CONTAINER);
        if (bankWidget != null && !bankWidget.isHidden()) {
            if (!bankopen) {
                bankopen = true;
//                MyLog.logUnique(Event.UniqueIdEvent.Type.BANK_OPEN);
            }
        } else {
            bankopen = false;
        }

        Widget potionWidget = client.getWidget(270, 14);
        if (potionWidget != null && !potionWidget.isHidden()) {
            if (!makeAllInterfaceOpen) {
                makeAllInterfaceOpen = true;
//                if (client.isKeyPressed(KeyCode.KC_SPACE))
//                {
//                    MyLog.logUnique(Event.UniqueIdEvent.Type.MAKE_ALL_INTERFACE_OPTION_CHOSEN);
//                }
            }
        } else  {
            makeAllInterfaceOpen = false;
        }

        Widget shopwidget = client.getWidget(300, 16);
        if (shopwidget != null && !shopwidget.isSelfHidden()) {
            List<Integer> idsAndQuantities = new ArrayList<>();
            if (shopwidget.getChildren() != null) {
                for (Widget child : shopwidget.getChildren()) {
                    idsAndQuantities.add(child.getItemId());
                    idsAndQuantities.add(child.getItemQuantity());
                }
            }
            if (!(idsAndQuantities.equals(lastShopState)) || !shopOpen) {
//                myprint("shop: " + idsAndQuantities + " " + shopOpen);
            }
            lastShopState = idsAndQuantities;
            shopOpen = true;
        } else {
//            if (shopOpen) myprint("shop closed");
            shopOpen = false;
        }


        Widget inventorywidget = client.getWidget(WidgetInfo.INVENTORY);
        if (inventorywidget == null || inventorywidget.isSelfHidden()) {
            IllegalStateException ise = new IllegalStateException("inventory is not available");
            ise.printStackTrace();
            throw ise;
        }

        List<Integer> idsAndQuantities = new ArrayList<>();
        for (WidgetItem child : inventorywidget.getWidgetItems()) {
            idsAndQuantities.add(child.getId());
            idsAndQuantities.add(child.getQuantity());
        }
        if (!(idsAndQuantities.equals(lastInventoryState))) {
            List<Integer> itemsRemoved = getRemovedItems(idsAndQuantities);
            List<Integer> itemsAdded = getAddedItems(idsAndQuantities);
//			System.out.println("inventory change detected " + client.getTickCount());

            for (Integer itemId : itemsRemoved)
            {
                myStuffOverlay.eatDelay += eatDelays.computeIfAbsent(itemId, i -> 0);
                if (eatDelays.computeIfAbsent(itemId, i -> 0) > 0) {
                }
            }

            int rawCount = 0;
            boolean cooked = false;
            for (int i = 0; i < idsAndQuantities.size(); i+=2)
            {
                int id = idsAndQuantities.get(i);
                if (rawItemIds.contains(id))
                {
                    rawCount++;
                }

                if (cookedItemIds.contains(id))
                {
                    cooked = true;
                }
            }
            if (rawCount == 1 && cooked) {
                notifyin = 2;
            }

//			List<Integer> itemsAdded = getAddedItems(idsAndQuantities);
//
            lastInventoryState = new ArrayList<>(idsAndQuantities);
//            try {
//                new Event.InventoryEvent(idsAndQuantities).log();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
        }
    }

    private int lastZulrahSeen = -1;
    private WorldPoint lastZulrahLocation = null;
    private int lastZulrahAnimation = -1;
    private int lastPlayerAnimation = -1;
    private int lastNewZulrah = -1;
    private int playerAttackCooldown = 0;
    private Map<ZulrahState, Integer> ticksPerState = new HashMap<>();
    {
        ticksPerState.put(ZulrahState.GREEN_2, 28);
        ticksPerState.put(ZulrahState.GREEN_3, 38);
        ticksPerState.put(ZulrahState.GREEN_4, 18);
        ticksPerState.put(ZulrahState.GREEN_5, 18);
        ticksPerState.put(ZulrahState.GREEN_6, 18);
        ticksPerState.put(ZulrahState.GREEN_7, 23);
        ticksPerState.put(ZulrahState.GREEN_8, 18);
        ticksPerState.put(ZulrahState.GREEN_9, 34);
        ticksPerState.put(ZulrahState.GREEN_10, 33);

        ticksPerState.put(ZulrahState.MAGMA_2, 18);
        ticksPerState.put(ZulrahState.MAGMA_3, 15);

        ticksPerState.put(ZulrahState.MAGMA_EAST_4, 26);
        ticksPerState.put(ZulrahState.MAGMA_EAST_5, 37);
        ticksPerState.put(ZulrahState.MAGMA_EAST_6, 19);
        ticksPerState.put(ZulrahState.MAGMA_EAST_7, 18);
        ticksPerState.put(ZulrahState.MAGMA_EAST_8, 34);

        ticksPerState.put(ZulrahState.MAGMA_NORTH_4, 37);
        ticksPerState.put(ZulrahState.MAGMA_NORTH_5, 19);
        ticksPerState.put(ZulrahState.MAGMA_NORTH_6, 18);
        ticksPerState.put(ZulrahState.MAGMA_NORTH_7, 26);
        ticksPerState.put(ZulrahState.MAGMA_NORTH_8, 34);

        ticksPerState.put(ZulrahState.TANZ_2, 34);
        ticksPerState.put(ZulrahState.TANZ_3, 22);
        ticksPerState.put(ZulrahState.TANZ_4, 28);
        ticksPerState.put(ZulrahState.TANZ_5, 25);
        ticksPerState.put(ZulrahState.TANZ_6, 15);
        ticksPerState.put(ZulrahState.TANZ_7, 32);
        ticksPerState.put(ZulrahState.TANZ_8, 31);
    }
    private void zulrahStuff()
    {
        if (client.getLocalPlayer().getAnimation() == 1167 && lastPlayerAnimation != 1167) {

//            System.out.println("player: " + client.getTickCount() + " swamp trident");
            playerAttackCooldown = client.getTickCount();
        } else if (client.getLocalPlayer().getAnimation() == 7552 && lastPlayerAnimation != 7552) {

//            System.out.println("player: " + client.getTickCount() + " rcb");
            playerAttackCooldown = client.getTickCount();
        }
        lastPlayerAnimation = client.getLocalPlayer().getAnimation();
        int timeIntoThisPhase = client.getTickCount() - lastNewZulrah;
        int regionId = WorldPoint.fromLocalInstance(client, client.getLocalPlayer().getLocalLocation()).getRegionID();
        if (ticksPerState.containsKey(getZulrahState()) && (regionId == 9007 || regionId == 9008)) {
            Integer ticks = ticksPerState.get(getZulrahState());
            int ticksPerAttack = (getZulrahState().isTanzanite()) ? 5 : 4;
            int ticksUntilNextPlayerAttack = Math.max(0, ticksPerAttack - (client.getTickCount() - playerAttackCooldown));
            int d = Math.max(0, 5 - timeIntoThisPhase);
            int ticksRemaining = ticks - timeIntoThisPhase - d - ticksUntilNextPlayerAttack;
            int attacksRemaining = (ticksRemaining + ticksPerAttack - 1) / ticksPerAttack;
//			System.out.println("ticks in this phase: " + ticks + " ticks into phase: " + timeIntoThisPhase + " player attack cd: " + ticksUntilNextPlayerAttack + " " + attacksRemaining);
//			if (ticksUntilNextPlayerAttack == 0) attacksRemaining--;
            myStuffOverlay.playerOverheadText = Integer.toString(attacksRemaining);
        } else {
            myStuffOverlay.playerOverheadText = null;
        }
        List<NPC> npcs = client.getNpcs();
//		boolean zulrahseen = false;
        for (NPC npc : npcs)
        {
            int id = npc.getId();
            if (id != ZULRAH_GREEN_ID && id != ZULRAH_RED_ID && id != ZULRAH_TANZ_ID) continue;

            WorldPoint location = WorldPoint.fromLocalInstance(client, npc.getLocalLocation());
            if (lastZulrahSeen != -1 && (lastZulrahSeen != id || !Objects.equals(lastZulrahLocation, location))) {
                lastNewZulrah = client.getTickCount();
                advanceState(id, location);
            }

            lastZulrahSeen = id;
            lastZulrahLocation = location;

            int animation = npc.getAnimation();
            if (animation != lastZulrahAnimation) {
                System.out.println("zulrah: " + client.getTickCount() + " new zulrah animation: " + animation);
                if (animation == 5072 && getZulrahState() != ZulrahState.START_1) {
                    ticksPerState.compute(getZulrahState(), (s, t) -> {
                        int newTickCount = timeIntoThisPhase;
                        if (t != null && newTickCount != t) {
                            System.out.println("conflict " + s + " from " + t + " to " + newTickCount);
                        }
//						if (t == null || newTickCount < t) {
                        return newTickCount;
//						}
//						return t;
                    });
                    System.out.println(ticksPerState);
                }
            }
            lastZulrahAnimation = animation;

            break;
        }
//		if (!zulrahseen) {
//			if (lastZulrahSeen != -1) System.out.println("zulrah: " + client.getTickCount() + " zulrah not present -1");
//			lastZulrahSeen = -1;
//			lastZulrahLocation = null;
//			lastZulrahAnimation = -1;
//		}

    }

    private List<Integer> getRemovedItems(List<Integer> idsAndQuantities)
    {
        List<Integer> itemsRemoved = new ArrayList<>(lastInventoryState);
        for (int i = 0; i < idsAndQuantities.size(); i+=2)
        {
            int id = idsAndQuantities.get(i);
            itemsRemoved.remove((Integer) id);
        }
        return itemsRemoved;
    }

    private List<Integer> getAddedItems(List<Integer> idsAndQuantities)
    {
        List<Integer> itemsAdded = new ArrayList<>(idsAndQuantities);
        for (int i = 0; i < lastInventoryState.size(); i+=2)
        {
            int id = lastInventoryState.get(i);
            itemsAdded.remove((Integer) id);
        }
        return itemsAdded;
    }

    //	private List<Integer> getRemovedItems(List<Integer> idsAndQuantities)
//	{
//		List<Integer> itemsRemoved = new ArrayList<>(lastInventoryState);
//		for (int i = 0; i < idsAndQuantities.size(); i+=2)
//		{
//			int id = idsAndQuantities.get(i);
//			itemsRemoved.remove((Integer) id);
//		}
//		return itemsRemoved;
//	}
//
    @Override
    protected void startUp() throws Exception
    {
        overlayManager.add(myStuffOverlay);
    }

    @Override
    protected void shutDown() throws Exception
    {
        overlayManager.remove(myStuffOverlay);
    }

    void bla()
    {

//        MyLog.playerLocation(client.getLocalPlayer().getWorldLocation());
    }

    private Actor lastSpecTarget;
    @Subscribe
    public void onInteractingChanged(InteractingChanged interactingChanged)
    {
        Actor source = interactingChanged.getSource();
        Actor target = interactingChanged.getTarget();
//		if (source != client.getLocalPlayer() || target == null)
//		{
//			return;
//		}

//		log.debug("Updating last spec target to {} (was {})", target.getName(), lastSpecTarget);
        lastSpecTarget = target;
    }

    @Inject
    public PluginManager pluginManager;

    private Plugin getPlugin(String pluginName) {
        for (Plugin plugin : pluginManager.getPlugins()) {
            if (pluginName.equals(plugin.getClass().getSimpleName())) {
                return plugin;
            }
        }
        return null;
    }

    private int lastValue = -1;
    private long lastHouseLoad = -1;
    @Subscribe
    public void onVarbitChanged(VarbitChanged e)
    {
//        int value = client.getVar(Varbits.HOUSE_LOADING_SCREEN);
//        if (lastValue != value && value == 0) {
//            lastHouseLoad = System.currentTimeMillis();
//            try
//            {
//                System.out.println("logging house load complete");
//                new Event.UniqueIdEvent(Event.UniqueIdEvent.Type.HOUSE_LOAD_COMPLETE).log();
//            }
//            catch (IOException ex)
//            {
//                ex.printStackTrace();
//            }
//        }
//        lastValue = value;
    }

    @Inject
    private ItemManager itemManager;

    @Inject
    private ConfigManager configManager;

    @Inject
    private ExternalPluginManager externalPluginManager;

    //    int bla = 0;
    @Subscribe
    public void onCommandExecuted(CommandExecuted commandExecuted) {
    	if ("testinvent".equals(commandExecuted.getCommand())) {
			ItemContainer itemContainer = client.getItemContainer(InventoryID.INVENTORY);
			int ppotdoses = 0;
			int restoredoses = 0;
			int brewdoses = 0;
			int scbdoses = 0;
			int divinescbdoses = 0;
			Map<String, Integer> items = new HashMap<>();
			for (Item item : itemContainer.getItems())
			{
				ItemStats itemStats = itemManager.getItemStats(item.getId(), false);
				if (itemStats != null && !itemStats.isEquipable()) {
					System.out.println("item " + itemManager.getItemComposition(item.getId()).getName());
					switch (item.getId()) {
						case ItemID.SARADOMIN_BREW4:
							brewdoses += 1;
						case ItemID.SARADOMIN_BREW3:
							brewdoses += 1;
						case ItemID.SARADOMIN_BREW2:
							brewdoses += 1;
						case ItemID.SARADOMIN_BREW1:
							brewdoses += 1;
							break;

						case ItemID.SUPER_RESTORE4:
							restoredoses++;
						case ItemID.SUPER_RESTORE3:
							restoredoses++;
						case ItemID.SUPER_RESTORE2:
							restoredoses++;
						case ItemID.SUPER_RESTORE1:
							restoredoses++;
							break;

						case ItemID.SUPER_COMBAT_POTION4:
							scbdoses++;
						case ItemID.SUPER_COMBAT_POTION3:
							scbdoses++;
						case ItemID.SUPER_COMBAT_POTION2:
							scbdoses++;
						case ItemID.SUPER_COMBAT_POTION1:
							scbdoses++;
							break;

						case ItemID.DIVINE_SUPER_COMBAT_POTION4:
							divinescbdoses++;
						case ItemID.DIVINE_SUPER_COMBAT_POTION3:
							divinescbdoses++;
						case ItemID.DIVINE_SUPER_COMBAT_POTION2:
							divinescbdoses++;
						case ItemID.DIVINE_SUPER_COMBAT_POTION1:
							divinescbdoses++;
							break;

						case ItemID.PRAYER_POTION4:
							ppotdoses++;
						case ItemID.PRAYER_POTION3:
							ppotdoses++;
						case ItemID.PRAYER_POTION2:
							ppotdoses++;
						case ItemID.PRAYER_POTION1:
							ppotdoses++;
							break;

						default:
							String name = itemManager.getItemComposition(item.getId()).getName();
							Integer value = items.get(name);
							if (value == null) value = 0;
							value++;
							items.put(name, value);
							break;
					}
				}
			}
			System.out.println("brews: " + brewdoses);
			System.out.println("restores: " + restoredoses);
			System.out.println("ppots: " + ppotdoses);
			System.out.println("scbs: " + scbdoses);
			System.out.println("divine scbs: " + divinescbdoses);
			System.out.println(items);
		}
		if ("mything".equals(commandExecuted.getCommand())) {
            System.out.println((1 * 60 + 56 + .4 + 1 * 60 + 53 + .4 + 2 * 60 + 7 + .8 + 2 * 60 + 3 + 1 * 60 + 58 + .8 + 2 * 60 + 2 * 60 + 21 + 1 * 60 + 45 + 1 * 60 + 54 + .6 + 1 * 60 + 49 + .8 + 2 * 60 + 12 + .6 + 1 * 60 + 46 + .8 + 2 * 60 + 8 + .4 + 2 * 60 + 1 + .8 + 2 * 60 + 20 + .4 + 1 * 60 + 55 + .8 + 1 * 60 + 42 + 2 * 60 + 14 + .4 + 1 * 60 + 51 + 1 * 60 + 51 + 2 * 60 + 31 + .8 + 2 * 60 + 58 + .2 + 2 * 60 + 17 + .4 + 1 * 60 + 52 + .8 + 1 * 60 + 49 + .8 + 1 * 60 + 52 + .8 + 1 * 60 + 56 + .4 + 2 * 60 + 7 + .8 + 2 * 60 + 12 + .6 + 1 * 60 + 49 + .2 + 2 * 60 + 14 + .4 + 2 * 60 + 10 + .2 + 2 * 60 + 46 + .2 + 2 * 60 + 3 + 1 * 60 + 48 + .6) / 35);
            ItemComposition itemComposition = itemManager.getItemComposition(10587);
            System.out.println(itemComposition.getShiftClickActionIndex());
            ItemComposition itemComposition2 = itemManager.getItemComposition(9244);
            System.out.println(itemComposition2.getShiftClickActionIndex());
        }
        if ("ticks".equals(commandExecuted.getCommand())) {
            ticks = 0;
        }
        if ("plugins".equals(commandExecuted.getCommand())) {

            Path path = Paths.get(URI.create("file:///C:/Users/samue/jars/"));
            for (File file : path.toFile().listFiles())
            {
                System.out.println("installing \"" + file.getName() + "\" \"" + file.getAbsolutePath() + "\"");
                externalPluginManager.install(file.getName());
            }
        }
        if ("aot".equals(commandExecuted.getCommand())) {
            boolean b = Boolean.parseBoolean(configManager.getConfiguration("runelite", "gameAlwaysOnTop"));
            configManager.setConfiguration("runelite", "gameAlwaysOnTop", !b);
            chatMessage(b ? "not aot" : "aot");
        }
        if ("force".equals(commandExecuted.getCommand())) {
            RequestFocusType b = RequestFocusType.valueOf(configManager.getConfiguration("runelite", "notificationRequestFocus"));
            if (b == RequestFocusType.FORCE) {
                configManager.setConfiguration("runelite", "notificationRequestFocus", RequestFocusType.OFF);
                chatMessage("not forcing");
            } else {
                configManager.setConfiguration("runelite", "notificationRequestFocus", RequestFocusType.FORCE);
                chatMessage("forcing");
            }
        }
        if ("ph".equals(commandExecuted.getCommand())) {
            String[] arguments = commandExecuted.getArguments();
            ItemComposition itemDefinition = client.getItemDefinition(Integer.valueOf(arguments[0]));
            client.addChatMessage(ChatMessageType.PUBLICCHAT, "bla",
                    itemDefinition.getName() + " " + arguments[0] + ": " + itemDefinition.getId() + " (" + ((itemDefinition.getPlaceholderTemplateId() == 14401) ? "placeholder" : "regular item") + ") "
                            + "becomes " + itemDefinition.getPlaceholderId() + " which becomes " + client.getItemDefinition(itemDefinition.getPlaceholderId()).getPlaceholderId() + " (" + ((client.getItemDefinition(itemDefinition.getPlaceholderId()).getPlaceholderTemplateId() == 14401) ? "placeholder" : "regular item") + ") " + itemManager.canonicalize(Integer.valueOf(arguments[0])) + " " + itemManager.canonicalize(itemDefinition.getPlaceholderId()),
                    "bla");
        }
        if ("itemname".equals(commandExecuted.getCommand())) {
            String[] arguments = commandExecuted.getArguments();
            Integer id = Integer.valueOf(arguments[0]);
            ItemComposition itemDefinition = client.getItemDefinition(id);
            client.addChatMessage(ChatMessageType.PUBLICCHAT, "bla",
                    arguments[0] + ": " + itemDefinition.getName(),
                    "bla");
        }
        if ("itemvar".equals(commandExecuted.getCommand())) {
            String[] arguments = commandExecuted.getArguments();
            Integer id = Integer.valueOf(arguments[0]);
            ItemComposition itemDefinition = client.getItemDefinition(id);
            Collection<Integer> variations = ItemVariationMapping.getVariations(ItemVariationMapping.map(id));
            if (variations.size() == 1) {
                client.addChatMessage(ChatMessageType.PUBLICCHAT, "bla",
                        arguments[0] + " has no variants.",
                        "bla");
            } else
            {
                client.addChatMessage(ChatMessageType.PUBLICCHAT, "bla",
                        arguments[0] + " base id: " + ItemVariationMapping.map(id) + " all variations: " + variations,
                        "bla");
            }
        }
        if ("testslots".equals(commandExecuted.getCommand())) {
            Map<EquipmentInventorySlot, Integer> numOfEachSlot = new HashMap<>();
            for (int i = 0; i < 26000; i++) {
                ItemStats itemStats = itemManager.getItemStats(i, false);
                ItemComposition itemComposition = itemManager.getItemComposition(i);
                if (itemStats != null && itemStats.isEquipable() && itemComposition.getPlaceholderTemplateId() != 14401) {
                    int slotId = itemStats.getEquipment().getSlot();

                    int slotIndex = slotId;
                    if (slotId >= 7) slotIndex--;
                    if (slotId >= 9) slotIndex--;
                    if (slotId >= 12) slotIndex--;
                    EquipmentInventorySlot slot = EquipmentInventorySlot.values()[slotIndex];

                    System.out.println(i + " " + itemComposition.getName() + ": " + slot);
                    numOfEachSlot.put(slot, numOfEachSlot.getOrDefault(slot, 0) + 1);
                }
            }
            System.out.println(numOfEachSlot);
        }
        if ("testvarbit".equals(commandExecuted.getCommand())) {
            bla = (bla + 1) % 5;
//            System.out.println("setting to " + bla);
            client.setVarbitValue(client.getVarps(), 6590, bla);
        }
        if (commandExecuted.getCommand().startsWith("!")) {
            String name = client.getLocalPlayer().getName();
            client.addChatMessage(ChatMessageType.PUBLICCHAT, name, commandExecuted.getCommand() + " " + String.join(" ", commandExecuted.getArguments()), name);
        }
    }

    private int lastTickForHydraSound = 0;
    @Subscribe
    public void onSoundEffectPlayed(SoundEffectPlayed soundEffectPlayed)
    {
//        System.out.println("sound id: " + soundEffectPlayed.getSoundId());
        if (soundEffectPlayed.getSoundId() == 162) {
            incrementLastCerbAnimation();
            lastcerbanimationGameTick = client.getTickCount();
//            System.out.println(lastcerbanimation + " " + client.getTickCount());
            MyStuffOverlay.printCerbProgress();
        }
        if (soundEffectPlayed.getSoundId() == 2780) {
            incrementLastCerbAnimation();
            lastcerbanimationGameTick = client.getTickCount();
//            System.out.println(lastcerbanimation + " " + client.getTickCount());
            MyStuffOverlay.printCerbProgress();
        }
        if (soundEffectPlayed.getSoundId() <= 2780 && soundEffectPlayed.getSoundId() >= 2766 && lastTickForHydraSound != client.getTickCount()) {
//            System.out.println(client.getTickCount() + " hydraticks++");
//            myStuffOverlay.hydraticks++;
//            if (myStuffOverlay.hydraticks % 3 == 1) {
//                myStuffOverlay.lastPrayerWasMage = client.isPrayerActive(Prayer.PROTECT_FROM_MAGIC);
//            }
            lastTickForHydraSound = client.getTickCount();
        }
    }

	private final Set<LocalPoint> xarpusExhumedsLocation = new HashSet<>();

    @Getter
    private final Map<GroundObject, Integer> exhumeds = new HashMap<>();
    @Data
    public static final class ExhumedWithCount {
    	GroundObject object;
    	int count;
	}

	private int exhumedCount = 0;

	@Subscribe
	public void onGroundObjectSpawned(GroundObjectSpawned event)
	{
//		if (xarpusActive)
//		{
			GroundObject o = event.getGroundObject();
			if (o.getId() == GROUNDOBJECT_ID_EXHUMED)
			{
				System.out.println("spawned!");
				if (xarpusExhumedsLocation.contains(o.getLocalLocation()))
				{
					return;
				}
				xarpusExhumedsLocation.add(o.getLocalLocation());

				exhumedCount++;

				exhumeds.put(o, exhumedCount);
			}
//		}
	}

	private static final int GROUNDOBJECT_ID_EXHUMED = 32743;
	@Subscribe
	public void onGroundObjectDespawned(GroundObjectDespawned event)
	{
//		if (xarpusActive)
//		{
			GroundObject o = event.getGroundObject();
			if (o.getId() == GROUNDOBJECT_ID_EXHUMED)
			{
				System.out.println("despawned!");
				xarpusExhumedsLocation.remove(o.getLocalLocation());
				exhumeds.remove(o);
			}
//		}
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned npcSpawned)
	{
		if (npcSpawned.getNpc().getId() == 963 || npcSpawned.getNpc().getId() == 965) {
			kq = npcSpawned.getNpc();
		}

		NPC npc = npcSpawned.getNpc();
		switch (npc.getId())
		{
			case NpcID.XARPUS:
			case NpcID.XARPUS_8339:
			case NpcID.XARPUS_8340:
			case NpcID.XARPUS_8341:
			case 10766:
			case 10767:
			case 10768:
			case 10769:
			case 10770:
			case 10771:
			case 10772:
			case 10773:
				exhumeds.clear();
				xarpusExhumedsLocation.clear();
				break;
		}
	}

}
