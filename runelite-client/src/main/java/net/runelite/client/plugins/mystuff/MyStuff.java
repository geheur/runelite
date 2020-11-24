package net.runelite.client.plugins.mystuff;

import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;

import javax.inject.Inject;
import java.awt.*;
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

    @Subscribe
    public void onHitsplatApplied(HitsplatApplied hitsplatApplied)
    {
        Actor actor = hitsplatApplied.getActor();
        if (actor == client.getLocalPlayer()) ticks = 0;
//		if (actor.getName().toLowerCase().contains("cerberus")) {
//			LootManager.cerbhp -= hitsplatApplied.getHitsplat().getAmount();
//		}
//        if (actor == client.getLocalPlayer()) {
//            if (LootManager.ignoreNext > 0)
//            {
//                LootManager.ignoreNext--;
//            } else
//            {
//                LootManager.lastcerbanimation++;
//                MyStuffOverlay.printCerbProgress();
//            }
//        }
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
//                LootManager.lastcerbanimation = 0;
//                LootManager.cerbhp = 600;
            } else if (skill == Skill.MAGIC) {
            }
//            ticks = 0;
        }
        xp.put(skill, newXp);

        int newLevel = statChanged.getBoostedLevel();
        int lastLevel = levels.getOrDefault(skill, 0);
        if (newLevel != lastLevel)
        {
//            MyLog.skillchange(skill, newLevel - lastLevel, newLevel);
        }
        levels.put(skill, newLevel);
    }

    private MenuEntry[] lastMenuEntries = null;

    private static final Pattern fightDurationPattern = Pattern.compile("Fight duration: ([\\d:]+). Personal best: ([\\d:]+)");
    private static final Pattern fightDurationPattern_PERSONAL_BEST_VARIANT = Pattern.compile("Fight duration: ([\\d:]+). Personal best: ([\\d:]+)");

    @Subscribe
    public void onChatMessage(ChatMessage event)
    {
        if (event.getType() != ChatMessageType.GAMEMESSAGE)
        {
            return;
        }

        String chatMsg = Text.removeTags(event.getMessage()); // remove color and linebreaks

        Matcher m = fightDurationPattern.matcher(chatMsg);
        if (m.matches()) {
            String time = m.group(1);
        }

//		if chatMsg
    }

    private boolean constructionWidgetVisible = false;

    @Subscribe
    public void onClientTick(ClientTick event)
    {
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
//            Plugin zulrahPlugin = getPlugin("ZulrahHelperPlugin");
//            try
//            {
//                zulrahPlugin.getClass().getDeclaredMethod("reset").invoke(zulrahPlugin);
//            }
//            catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e)
//            {
//                e.printStackTrace();
//            }
//            setZulrahRotation(rotation_start);
//
//            lastZulrahSeen = -1;
//            lastZulrahLocation = null;
//            lastZulrahAnimation = -1;
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
    private static List<Integer> rawItemIds = Arrays.asList(new Integer[]{383, 7944, 13439, 3142, 1775});
    private static List<Integer> cookedItemIds = Arrays.asList(new Integer[]{3144, 385, 13441, 7946, 10980});
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
    @Subscribe
    public void onAnimationChanged(AnimationChanged event)
    {
        if (client.getGameState() != GameState.LOGGED_IN)
        {
            return;
        }

        Player localPlayer = client.getLocalPlayer();
        if (localPlayer != event.getActor())
        {
            return;
        }

        int graphic = localPlayer.getGraphic();
        int animation = localPlayer.getAnimation();

//		if (animation == 1167 || animation == 7552) {
//
//			System.out.println("animation: " + animation);
//		}
//
        if (animation == 381 || animation == 393) {
//            MyLog.logUnique(Event.UniqueIdEvent.Type.ATTACK_ANIMATION);
        }
    }

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
//    static {
//        rotation_start.add(ZulrahState.START_1);
//
//        rotation_magma_pre.add(ZulrahState.MAGMA_2);
//        rotation_magma_pre.add(ZulrahState.MAGMA_3);
//
//        rotation_magma_east.add(ZulrahState.MAGMA_EAST_4);
//        rotation_magma_east.add(ZulrahState.MAGMA_EAST_5);
//        rotation_magma_east.add(ZulrahState.MAGMA_EAST_6);
//        rotation_magma_east.add(ZulrahState.MAGMA_EAST_7);
//        rotation_magma_east.add(ZulrahState.MAGMA_EAST_8);
//        rotation_magma_east.add(ZulrahState.MAGMA_EAST_9);
//        rotation_magma_east.add(ZulrahState.MAGMA_EAST_10);
//
//        rotation_magma_north.add(ZulrahState.MAGMA_NORTH_4);
//        rotation_magma_north.add(ZulrahState.MAGMA_NORTH_5);
//        rotation_magma_north.add(ZulrahState.MAGMA_NORTH_6);
//        rotation_magma_north.add(ZulrahState.MAGMA_NORTH_7);
//        rotation_magma_north.add(ZulrahState.MAGMA_NORTH_8);
//        rotation_magma_north.add(ZulrahState.MAGMA_NORTH_9);
//        rotation_magma_north.add(ZulrahState.MAGMA_NORTH_10);
//
//        rotation_tanz.add(ZulrahState.TANZ_2);
//        rotation_tanz.add(ZulrahState.TANZ_3);
//        rotation_tanz.add(ZulrahState.TANZ_4);
//        rotation_tanz.add(ZulrahState.TANZ_5);
//        rotation_tanz.add(ZulrahState.TANZ_6);
//        rotation_tanz.add(ZulrahState.TANZ_7);
//        rotation_tanz.add(ZulrahState.TANZ_8);
//        rotation_tanz.add(ZulrahState.TANZ_9);
//        rotation_tanz.add(ZulrahState.TANZ_10);
//        rotation_tanz.add(ZulrahState.TANZ_11);
//        rotation_tanz.add(ZulrahState.TANZ_12);
//
//        rotation_green.add(ZulrahState.GREEN_2);
//        rotation_green.add(ZulrahState.GREEN_3);
//        rotation_green.add(ZulrahState.GREEN_4);
//        rotation_green.add(ZulrahState.GREEN_5);
//        rotation_green.add(ZulrahState.GREEN_6);
//        rotation_green.add(ZulrahState.GREEN_7);
//        rotation_green.add(ZulrahState.GREEN_8);
//        rotation_green.add(ZulrahState.GREEN_9);
//        rotation_green.add(ZulrahState.GREEN_10);
//        rotation_green.add(ZulrahState.GREEN_11);
//    }

    private List<ZulrahState> zulrahRotation = null;
    private int zulrahRotationIndex = -1;
    private void setZulrahRotation(List<ZulrahState> rotation) {
        zulrahRotation = rotation;
        zulrahRotationIndex = 0;
    }

    private ZulrahState getZulrahState() {
        return (zulrahRotation == null) ? null : zulrahRotation.get(zulrahRotationIndex);
    }

//    private void selectOption(int choice) {
//        choice--;
//        Plugin zulrahPlugin = getPlugin("ZulrahHelperPlugin");
//        Method selectOption = null;
//        try
//        {
//            selectOption = zulrahPlugin.getClass().getDeclaredMethod("selectOption", int.class);
//        }
//        catch (NoSuchMethodException e)
//        {
//            e.printStackTrace();
//        }
//        selectOption.setAccessible(true);
//        try
//        {
//            selectOption.invoke(zulrahPlugin, choice);
//        }
//        catch (IllegalAccessException e)
//        {
//            e.printStackTrace();
//        }
//        catch (InvocationTargetException e)
//        {
//            e.printStackTrace();
//        }
//    }

//    private void advanceState(int color, WorldPoint worldPoint) {
//        ZulrahState zulrahState = getZulrahState();
//        if (zulrahState == ZulrahState.START_1) {
//            if (color == ZULRAH_GREEN_ID) {
//                setZulrahRotation(rotation_green);
//                selectOption(2);
//            } else if (color == ZULRAH_RED_ID) {
//                setZulrahRotation(rotation_magma_pre);
//                selectOption(1);
//            } else if (color == ZULRAH_TANZ_ID) {
//                setZulrahRotation(rotation_tanz);
//                selectOption(3);
//            }
//        } else if (zulrahState == ZulrahState.MAGMA_3) {
//            System.out.println("zulrah location: " + worldPoint.getRegionID() + " " + worldPoint.getRegionX() + " " + worldPoint.getRegionY());
//            if (worldPoint.getRegionID() == 9007 && worldPoint.getRegionX() == 28 && worldPoint.getRegionY() == 56) {
//                setZulrahRotation(rotation_magma_north);
//                selectOption(1);
//            } else {
//                setZulrahRotation(rotation_magma_east);
//                selectOption(2);
//            }
//        } else {
//            zulrahRotationIndex++;
//            if (zulrahRotationIndex >= zulrahRotation.size()) {
//                setZulrahRotation(rotation_start);
//            }
//        }

//        System.out.println("zulrah state update: " + zulrahState + " -> " + getZulrahState());
//
//    }

    private boolean makeAllInterfaceOpen = false;
    private boolean bankopen = false;
    private boolean lastTickPiety = false;
    private String lastNpcText = "";
    @Subscribe
    public void onGameTick(GameTick event)
    {
//		List<NPC> npcs = client.getNpcs();
//		NPC npc = npcs.get(0);
//
//		System.out.println("class: " + npc.getClass().getSimpleName());
//		System.out.println("resource: " + npc.getClass().getResource(npc.getClass().getSimpleName()));
//		for (Method method : npc.getClass().getMethods())
//		{
//
//			System.out.println("method: " + method.getReturnType().getSimpleName() + " " + Arrays.asList(method.getParameterTypes()) + " " + method.getName());
//			if (method.getReturnType().equals(int[].class)) {
//				try
//				{
//					int[] invoke = (int[]) method.invoke(npc);
//					ArrayList<Integer> objects = new ArrayList<>();
//					for (int i : invoke)
//					{
//						objects.add(i);
//					}
//					System.out.println(objects);
//					Arrays.asList(invoke);
//				}
//				catch (IllegalAccessException e)
//				{
//					e.printStackTrace();
//				}
//				catch (InvocationTargetException e)
//				{
//					e.printStackTrace();
//				}
//			}
//		}
//		for (Field field : npc.getClass().getFields())
//		{
//
//			System.out.println("field: " + field.getType().getSimpleName() + " " + field.getName());
//		}

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
//                advanceState(id, location);
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

}
