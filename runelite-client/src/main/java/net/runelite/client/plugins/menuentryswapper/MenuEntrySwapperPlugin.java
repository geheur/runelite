/*
 * Copyright (c) 2018, Adam <Adam@sigterm.info>
 * Copyright (c) 2018, Kamiel
 * Copyright (c) 2019, Rami <https://github.com/Rami-J>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.menuentryswapper;

import com.google.common.annotations.VisibleForTesting;
import static com.google.common.base.Predicates.alwaysTrue;
import static com.google.common.base.Predicates.equalTo;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.inject.Provides;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.inject.Inject;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ItemComposition;
import net.runelite.api.KeyCode;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.PostItemComposition;
import net.runelite.api.events.WidgetMenuOptionClicked;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemVariationMapping;
import net.runelite.client.input.KeyManager;
import net.runelite.client.input.MouseManager;
import net.runelite.client.menus.MenuManager;
import net.runelite.client.menus.WidgetMenuOption;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.Text;
import org.apache.commons.lang3.ArrayUtils;

import javax.inject.Inject;
import java.util.List;
import java.util.Set;

@PluginDescriptor(
		name = "Menu Entry Swapper",
		description = "Change the default option that is displayed when hovering over objects",
		tags = {"npcs", "inventory", "items", "objects"},
		enabledByDefault = false
)
public class MenuEntrySwapperPlugin extends Plugin
{
	private static final String CONFIGURE = "Configure";
	private static final String SAVE = "Save";
	private static final String RESET = "Reset";
	private static final String MENU_TARGET = "Shift-click";

	private static final String SHIFTCLICK_CONFIG_GROUP = "shiftclick";
	private static final String ITEM_KEY_PREFIX = "item_";

	public static final String BUY_SELL_OPTION_REGEX = "(Buy|Sell) (\\d+)";
	public static final String SET_BUY_SELL_QUANTITY_REGEX = "Set (buy|sell) quantity (\\d+)";
	public static final String RESET_BUY_SELL_QUANTITY_REGEX = "Reset (buy|sell) quantity";

	private static final WidgetMenuOption FIXED_INVENTORY_TAB_CONFIGURE = new WidgetMenuOption(CONFIGURE,
			MENU_TARGET, WidgetInfo.FIXED_VIEWPORT_INVENTORY_TAB);

	private static final WidgetMenuOption FIXED_INVENTORY_TAB_SAVE = new WidgetMenuOption(SAVE,
			MENU_TARGET, WidgetInfo.FIXED_VIEWPORT_INVENTORY_TAB);

	private static final WidgetMenuOption RESIZABLE_INVENTORY_TAB_CONFIGURE = new WidgetMenuOption(CONFIGURE,
			MENU_TARGET, WidgetInfo.RESIZABLE_VIEWPORT_INVENTORY_TAB);

	private static final WidgetMenuOption RESIZABLE_INVENTORY_TAB_SAVE = new WidgetMenuOption(SAVE,
			MENU_TARGET, WidgetInfo.RESIZABLE_VIEWPORT_INVENTORY_TAB);

	private static final WidgetMenuOption RESIZABLE_BOTTOM_LINE_INVENTORY_TAB_CONFIGURE = new WidgetMenuOption(CONFIGURE,
			MENU_TARGET, WidgetInfo.RESIZABLE_VIEWPORT_BOTTOM_LINE_INVENTORY_TAB);

	private static final WidgetMenuOption RESIZABLE_BOTTOM_LINE_INVENTORY_TAB_SAVE = new WidgetMenuOption(SAVE,
			MENU_TARGET, WidgetInfo.RESIZABLE_VIEWPORT_BOTTOM_LINE_INVENTORY_TAB);

	private static final Set<MenuAction> NPC_MENU_TYPES = ImmutableSet.of(
			MenuAction.NPC_FIRST_OPTION,
			MenuAction.NPC_SECOND_OPTION,
			MenuAction.NPC_THIRD_OPTION,
			MenuAction.NPC_FOURTH_OPTION,
			MenuAction.NPC_FIFTH_OPTION,
			MenuAction.EXAMINE_NPC);

	private static final Set<String> ESSENCE_MINE_NPCS = ImmutableSet.of(
			"aubury",
			"wizard sedridor",
			"wizard distentor",
			"wizard cromperty",
			"brimstail"
	);

	private final Map<String, Integer> buyQuantityMap = new HashMap<>();
	private final Map<String, Integer> sellQuantityMap = new HashMap<>();

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private MenuEntrySwapperConfig config;

//	@Inject
//	private ClickInputListener clickInputListener;

	@Inject
	private ConfigManager configManager;

	@Inject
	private KeyManager keyManager;

	@Inject
	private MouseManager mouseManager;

	@Inject
	private MenuManager menuManager;

	@Inject
	private ItemManager itemManager;

	@Getter
	private boolean configuringShiftClick = false;

	private final Multimap<String, Swap> swaps = LinkedHashMultimap.create();
	private final ArrayListMultimap<String, Integer> optionIndexes = ArrayListMultimap.create();

	@Provides
	MenuEntrySwapperConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(MenuEntrySwapperConfig.class);
	}

	@Override
	public void startUp()
	{
		if (config.shiftClickCustomization())
		{
			enableCustomization();
		}

		setupSwaps();
	}

	@Override
	public void shutDown()
	{
		disableCustomization();

		swaps.clear();
	}

	@VisibleForTesting
	void setupSwaps()
	{
		swap("talk-to", "mage of zamorak", "teleport", config::swapAbyssTeleport);
		swap("talk-to", "rionasta", "send-parcel", config::swapHardWoodGroveParcel);
		swap("talk-to", "captain khaled", "task", config::swapCaptainKhaled);
		swap("talk-to", "bank", config::swapBank);
		swap("talk-to", "contract", config::swapContract);
		swap("talk-to", "exchange", config::swapExchange);
		swap("talk-to", "help", config::swapHelp);
		swap("talk-to", "nets", config::swapNets);
		swap("talk-to", "repairs", config::swapDarkMage);
		// make sure assignment swap is higher priority than trade swap for slayer masters
		swap("talk-to", "assignment", config::swapAssignment);
		swap("talk-to", "trade", config::swapTrade);
		swap("talk-to", "trade-with", config::swapTrade);
		swap("talk-to", "shop", config::swapTrade);
		swap("talk-to", "robin", "claim-slime", config::claimSlime);
		swap("talk-to", "travel", config::swapTravel);
		swap("talk-to", "pay-fare", config::swapTravel);
		swap("talk-to", "charter", config::swapTravel);
		swap("talk-to", "take-boat", config::swapTravel);
		swap("talk-to", "fly", config::swapTravel);
		swap("talk-to", "jatizso", config::swapTravel);
		swap("talk-to", "neitiznot", config::swapTravel);
		swap("travel", "dive", config::swapTravel);
		swap("talk-to", "rellekka", config::swapTravel);
		swap("talk-to", "island of stone", config::swapTravel);
		swap("talk-to", "ungael", config::swapTravel);
		swap("talk-to", "pirate's cove", config::swapTravel);
		swap("talk-to", "waterbirth island", config::swapTravel);
		swap("talk-to", "island of stone", config::swapTravel);
		swap("talk-to", "miscellania", config::swapTravel);
		swap("talk-to", "follow", config::swapTravel);
		swap("talk-to", "transport", config::swapTravel);
		swap("talk-to", "pay", config::swapPay);
		swapContains("talk-to", alwaysTrue(), "pay (", config::swapPay);
		swap("talk-to", "decant", config::swapDecant);
		swap("talk-to", "quick-travel", config::swapQuick);
		swap("talk-to", "enchant", config::swapEnchant);
		swap("talk-to", "start-minigame", config::swapStartMinigame);
		swap("talk-to", ESSENCE_MINE_NPCS::contains, "teleport", config::swapEssenceMineTeleport);
		swap("talk-to", "collect", config::swapCollectMiscellania);

		swap("message", "delete", () -> {
//			System.out.println("shiftmodifer: " + shiftModifier());
			return shiftModifier();
		});

//		swap("deposit-all", "fill", () -> true); // doesn't work I don't think.
//		swap("fill", "empty", () -> true);
		swap("open", "rune pouch", "empty", () -> shiftModifier());

		swap("pass", "energy barrier", "pay-toll(2-ecto)", config::swapTravel);
		swap("open", "gate", "pay-toll(10gp)", config::swapTravel);

		swap("open", "hardwood grove doors", "quick-pay(100)", config::swapHardWoodGrove);

		swap("inspect", "trapdoor", "travel", config::swapTravel);
		swap("board", "travel cart", "pay-fare", config::swapTravel);

		swap("cage", "harpoon", config::swapHarpoon);
		swap("big net", "harpoon", config::swapHarpoon);
		swap("net", "harpoon", config::swapHarpoon);

		swap("enter", "portal", "home", () -> config.swapHomePortal() == HouseMode.HOME);
		swap("enter", "portal", "build mode", () -> config.swapHomePortal() == HouseMode.BUILD_MODE);
		swap("enter", "portal", "friend's house", () -> config.swapHomePortal() == HouseMode.FRIENDS_HOUSE);

		swap("view", "add-house", () -> config.swapHouseAdvertisement() == HouseAdvertisementMode.ADD_HOUSE);
		swap("view", "visit-last", () -> config.swapHouseAdvertisement() == HouseAdvertisementMode.VISIT_LAST);

		for (String option : new String[]{"zanaris", "configure", "tree"})
		{
			swapContains(option, alwaysTrue(), "zanaris", () -> client.isKeyPressed(KeyCode.KC_Z));
			swapContains(option, alwaysTrue(), "last-destination", () -> (config.swapFairyRing() == FairyRingMode.LAST_DESTINATION ^ shiftModifier()) && !client.isKeyPressed(KeyCode.KC_T) && !client.isKeyPressed(KeyCode.KC_Z));
			swapContains(option, alwaysTrue(), "configure", () -> (config.swapFairyRing() == FairyRingMode.CONFIGURE ^ shiftModifier()) && !client.isKeyPressed(KeyCode.KC_T) && !client.isKeyPressed(KeyCode.KC_Z));
		}

		// order matters here.
		swapContains("venerate", alwaysTrue(), "ancient", () -> client.isKeyPressed(KeyCode.KC_C));
		swapContains("venerate", alwaysTrue(), "arceuus", () -> client.isKeyPressed(KeyCode.KC_R));
		swapContains("venerate", alwaysTrue(), "standard", () -> true);
		swapContains("venerate", alwaysTrue(), "lunar", () -> true);

		swapContains("tree", alwaysTrue(), "zanaris", () -> config.swapFairyRing() == FairyRingMode.ZANARIS);

		swap("fill", "coal bag", "empty", () -> true);

		swap("check", "reset", config::swapBoxTrap);
		swap("dismantle", "reset", config::swapBoxTrap);
		swap("take", "lay", config::swapBoxTrap);

		swap("pick-up", "chase", config::swapChase);

		swap("interact", target -> target.endsWith("birdhouse"), "empty", config::swapBirdhouseEmpty);

		swap("enter", "the gauntlet", "enter-corrupted", config::swapGauntlet);

		swap("enter", "quick-enter", config::swapQuick);
		swap("ring", "quick-start", config::swapQuick);
		swap("pass", "quick-pass", config::swapQuick);
		swap("pass", "quick pass", config::swapQuick);
		swap("open", "quick-open", config::swapQuick);
		swap("climb-down", "quick-start", config::swapQuick);
		swap("climb-down", "pay", config::swapQuick);

		swap("admire", "teleport", config::swapAdmire);
		swap("admire", "spellbook", config::swapAdmire);
		swap("admire", "perks", config::swapAdmire);

		swap("teleport menu", "duel arena", () -> config.swapJewelleryBox() ^ shiftModifier());
		swap("teleport menu", "castle wars", () -> config.swapJewelleryBox() ^ shiftModifier());
		swap("teleport menu", "ferox enclave", () -> config.swapJewelleryBox() ^ shiftModifier());
		swap("teleport menu", "burthorpe", () -> config.swapJewelleryBox() ^ shiftModifier());
		swap("teleport menu", "barbarian outpost", () -> config.swapJewelleryBox() ^ shiftModifier());
		swap("teleport menu", "corporeal beast", () -> config.swapJewelleryBox() ^ shiftModifier());
		swap("teleport menu", "tears of guthix", () -> config.swapJewelleryBox() ^ shiftModifier());
		swap("teleport menu", "wintertodt camp", () -> config.swapJewelleryBox() ^ shiftModifier());
		swap("teleport menu", "warriors' guild", () -> config.swapJewelleryBox() ^ shiftModifier());
		swap("teleport menu", "champions' guild", () -> config.swapJewelleryBox() ^ shiftModifier());
		swap("teleport menu", "monastery", () -> config.swapJewelleryBox() ^ shiftModifier());
		swap("teleport menu", "ranging guild", () -> config.swapJewelleryBox() ^ shiftModifier());
		swap("teleport menu", "fishing guild", () -> config.swapJewelleryBox() ^ shiftModifier());
		swap("teleport menu", "mining guild", () -> config.swapJewelleryBox() ^ shiftModifier());
		swap("teleport menu", "crafting guild", () -> config.swapJewelleryBox() ^ shiftModifier());
		swap("teleport menu", "cooking guild", () -> config.swapJewelleryBox() ^ shiftModifier());
		swap("teleport menu", "woodcutting guild", () -> config.swapJewelleryBox() ^ shiftModifier());
		swap("teleport menu", "farming guild", () -> config.swapJewelleryBox() ^ shiftModifier());
		swap("teleport menu", "miscellania", () -> config.swapJewelleryBox() ^ shiftModifier());
		swap("teleport menu", "grand exchange", () -> config.swapJewelleryBox() ^ shiftModifier());
		swap("teleport menu", "falador park", () -> config.swapJewelleryBox() ^ shiftModifier());
		swap("teleport menu", "dondakan's rock", () -> config.swapJewelleryBox() ^ shiftModifier());
		swap("teleport menu", "edgeville", () -> config.swapJewelleryBox() ^ shiftModifier());
		swap("teleport menu", "karamja", () -> config.swapJewelleryBox() ^ shiftModifier());
		swap("teleport menu", "draynor village", () -> config.swapJewelleryBox() ^ shiftModifier());
		swap("teleport menu", "al kharid", () -> config.swapJewelleryBox() ^ shiftModifier());

		swap("shared", "private", config::swapPrivate);

		swap("pick", "pick-lots", config::swapPick);

		swap("view offer", "abort offer", () -> shiftModifier() && config.swapGEAbort());

		Arrays.asList(
				"honest jimmy", "bert the sandman", "advisor ghrim", "dark mage", "lanthus", "turael", "mazchna", "vannaka",
				"chaeldar", "nieve", "steve", "duradel", "krystilia", "konar", "murphy", "cyrisus", "smoggy", "ginea", "watson",
				"barbarian guard", "amy", "random"
		).forEach(npc -> swap("cast", "npc contact", npc, () -> shiftModifier() && config.swapNpcContact()));

		swap("value", target -> (target == null) ? false : buyQuantityMap.getOrDefault(target, -1) == 1, "buy 1", () -> true);
		swap("value", target -> (target == null) ? false : buyQuantityMap.getOrDefault(target, -1) == 5, "buy 5", () -> true);
		swap("value", target -> (target == null) ? false : buyQuantityMap.getOrDefault(target, -1) == 10, "buy 10", () -> true);
		swap("value", target -> (target == null) ? false : buyQuantityMap.getOrDefault(target, -1) == 50, "buy 50", () -> true);

		swap("value", target -> (target == null) ? false : sellQuantityMap.getOrDefault(target, -1) == 1, "sell 1", () -> true);
		swap("value", target -> (target == null) ? false : sellQuantityMap.getOrDefault(target, -1) == 5, "sell 5", () -> true);
		swap("value", target -> (target == null) ? false : sellQuantityMap.getOrDefault(target, -1) == 10, "sell 10", () -> true);
		swap("value", target -> (target == null) ? false : sellQuantityMap.getOrDefault(target, -1) == 50, "sell 50", () -> true);

		swap("wear", "rub", () -> {
			boolean result = shiftModifier() ^ config.swapTeleportItem();
//			System.out.println(shiftModifier() + " " + config.swapTeleportItem() + " " + result);
//			new Exception().printStackTrace(System.out);
			return result;
		});
		swap("wear", "teleport", () -> {
//			System.out.println("shiftmodifier is " + shiftModifier());
			return shiftModifier() ^ config.swapTeleportItem();
		});
		swap("wield", "teleport", () -> shiftModifier() ^ config.swapTeleportItem());

		swap("climb", "climb-up", () -> !shiftModifier());
		swap("climb", "climb-down", () -> shiftModifier());
//		swap("open", "pick-lock", () -> true);
		swap("open", "search for traps", () -> true);

		swap("bury", "use", config::swapBones);

//		swap("clean", "use", config::swapHerbs);
		swap("clean", target -> !(target.contains("buchu") || target.contains("golpar") || target.contains("noxifer")), "use", config::swapHerbs);

		swap("collect-note", "collect-item", () -> config.swapGEItemCollect() == GEItemCollectMode.ITEMS);
		swap("collect-notes", "collect-items", () -> config.swapGEItemCollect() == GEItemCollectMode.ITEMS);

		swap("collect-item", "collect-note", () -> config.swapGEItemCollect() == GEItemCollectMode.NOTES);
		swap("collect-items", "collect-notes", () -> config.swapGEItemCollect() == GEItemCollectMode.NOTES);

		swap("collect to inventory", "collect to bank", () -> config.swapGEItemCollect() == GEItemCollectMode.BANK);
		swap("collect", "bank", () -> config.swapGEItemCollect() == GEItemCollectMode.BANK);
		swap("collect-note", "bank", () -> config.swapGEItemCollect() == GEItemCollectMode.BANK);
		swap("collect-notes", "bank", () -> config.swapGEItemCollect() == GEItemCollectMode.BANK);
		swap("collect-item", "bank", () -> config.swapGEItemCollect() == GEItemCollectMode.BANK);
		swap("collect-items", "bank", () -> config.swapGEItemCollect() == GEItemCollectMode.BANK);

		swap("tan 1", "tan all", config::swapTan);

		swapTeleport("varrock teleport", "grand exchange");
		swapTeleport("camelot teleport", "seers'");
		swapTeleport("watchtower teleport", "yanille");
		swapTeleport("teleport to house", "outside");
	}

	private void swap(String option, String swappedOption, Supplier<Boolean> enabled)
	{
		swap(option, alwaysTrue(), swappedOption, enabled);
	}

	private void swap(String option, String target, String swappedOption, Supplier<Boolean> enabled)
	{
		swap(option, equalTo(target), swappedOption, enabled);
	}

	private void swap(String option, Predicate<String> targetPredicate, String swappedOption, Supplier<Boolean> enabled)
	{
		swaps.put(option, new Swap(alwaysTrue(), targetPredicate, swappedOption, enabled, true));
	}

	private void swapContains(String option, Predicate<String> targetPredicate, String swappedOption, Supplier<Boolean> enabled)
	{
		swaps.put(option, new Swap(alwaysTrue(), targetPredicate, swappedOption, enabled, false));
	}

	private void swapTeleport(String option, String swappedOption)
	{
		swap("cast", option, swappedOption, () -> shiftModifier() && config.swapTeleportSpell());
		swap(swappedOption, option, "cast", () -> shiftModifier() && config.swapTeleportSpell());
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (event.getGroup().equals(MenuEntrySwapperConfig.GROUP) && event.getKey().equals("shiftClickCustomization"))
		{
			if (config.shiftClickCustomization())
			{
				enableCustomization();
			}
			else
			{
				disableCustomization();
			}
		}
		else if (event.getGroup().equals(SHIFTCLICK_CONFIG_GROUP) && event.getKey().startsWith(ITEM_KEY_PREFIX))
		{
			clientThread.invoke(this::resetItemCompositionCache);
		}
	}

	private void resetItemCompositionCache()
	{
		itemManager.invalidateItemCompositionCache();
		client.getItemCompositionCache().reset();
	}

	private Integer getSwapConfig(int itemId)
	{
		itemId = ItemVariationMapping.map(itemId);
		String config = configManager.getConfiguration(SHIFTCLICK_CONFIG_GROUP, ITEM_KEY_PREFIX + itemId);
		if (config == null || config.isEmpty())
		{
			return null;
		}

		return Integer.parseInt(config);
	}

	private void setSwapConfig(int itemId, int index)
	{
		itemId = ItemVariationMapping.map(itemId);
		configManager.setConfiguration(SHIFTCLICK_CONFIG_GROUP, ITEM_KEY_PREFIX + itemId, index);
	}

	private void unsetSwapConfig(int itemId)
	{
		itemId = ItemVariationMapping.map(itemId);
		configManager.unsetConfiguration(SHIFTCLICK_CONFIG_GROUP, ITEM_KEY_PREFIX + itemId);
	}

	private void enableCustomization()
	{
//		mouseManager.registerMouseListener(clickInputListener);
		refreshShiftClickCustomizationMenus();
		// set shift click action index on the item compositions
		clientThread.invoke(this::resetItemCompositionCache);
	}

	private void disableCustomization()
	{
//		mouseManager.unregisterMouseListener(clickInputListener);
		removeShiftClickCustomizationMenus();
		configuringShiftClick = false;
		// flush item compositions to reset the shift click action index
		clientThread.invoke(this::resetItemCompositionCache);
	}

	@Subscribe
	public void onWidgetMenuOptionClicked(WidgetMenuOptionClicked event)
	{
		if (event.getWidget() == WidgetInfo.FIXED_VIEWPORT_INVENTORY_TAB
				|| event.getWidget() == WidgetInfo.RESIZABLE_VIEWPORT_INVENTORY_TAB
				|| event.getWidget() == WidgetInfo.RESIZABLE_VIEWPORT_BOTTOM_LINE_INVENTORY_TAB)
		{
			configuringShiftClick = event.getMenuOption().equals(CONFIGURE) && Text.removeTags(event.getMenuTarget()).equals(MENU_TARGET);
			refreshShiftClickCustomizationMenus();
		}
	}

	@Subscribe
	public void onMenuOpened(MenuOpened event)
	{
		if (!configuringShiftClick)
		{
			return;
		}

		MenuEntry firstEntry = event.getFirstEntry();
		if (firstEntry == null)
		{
			return;
		}

		int widgetId = firstEntry.getParam1();
		if (widgetId != WidgetInfo.INVENTORY.getId())
		{
			return;
		}

		int itemId = firstEntry.getIdentifier();
		if (itemId == -1)
		{
			return;
		}

		ItemComposition itemComposition = client.getItemDefinition(itemId);
		String itemName = itemComposition.getName();
		String option = "Use";
		int shiftClickActionIndex = itemComposition.getShiftClickActionIndex();
		String[] inventoryActions = itemComposition.getInventoryActions();

		if (shiftClickActionIndex >= 0 && shiftClickActionIndex < inventoryActions.length)
		{
			option = inventoryActions[shiftClickActionIndex];
		}

		MenuEntry[] entries = event.getMenuEntries();

		for (MenuEntry entry : entries)
		{
			if (itemName.equals(Text.removeTags(entry.getTarget())))
			{
				entry.setType(MenuAction.RUNELITE.getId());

				if (option.equals(entry.getOption()))
				{
					entry.setOption("* " + option);
				}
			}
		}

		final MenuEntry resetShiftClickEntry = new MenuEntry();
		resetShiftClickEntry.setOption(RESET);
		resetShiftClickEntry.setTarget(MENU_TARGET);
		resetShiftClickEntry.setIdentifier(itemId);
		resetShiftClickEntry.setParam1(widgetId);
		resetShiftClickEntry.setType(MenuAction.RUNELITE.getId());
		client.setMenuEntries(ArrayUtils.addAll(entries, resetShiftClickEntry));
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded menuEntryAdded)
	{
//		System.out.println(menuEntryAdded.getOption() + " " + menuEntryAdded.getTarget() + " " + menuEntryAdded.getType() + " " + menuEntryAdded.getActionParam0() + " " + menuEntryAdded.getActionParam1() + " " + menuEntryAdded.getIdentifier());
		// 1007 is in bank.
		if (shiftModifier()) swapBuySell(menuEntryAdded);

		// This swap needs to happen prior to drag start on click, which happens during
		// widget ticking and prior to our client tick event. This is because drag start
		// is what builds the context menu row which is what the eventual click will use

		ShiftDepositMode depositMode = ShiftDepositMode.OFF;
		ShiftWithdrawMode withdrawMode = ShiftWithdrawMode.OFF;

//		for (int i = 0; i < 112; i++)
//		{
//			if (client.isKeyPressed(i))
//				System.out.println("key pressed: " + i);
//		}

		if (client.isKeyPressed(KeyCode.KC_X))
		{
			depositMode = ShiftDepositMode.DEPOSIT_X;
			withdrawMode = ShiftWithdrawMode.WITHDRAW_X;
		}
		else if (client.isKeyPressed(KeyCode.KC_C))
		{
			depositMode = ShiftDepositMode.DEPOSIT_SET_X;
			withdrawMode = ShiftWithdrawMode.WITHDRAW_SET_X;
		}
		else if (client.isKeyPressed(KeyCode.KC_F))
		{
			int noModSetting = client.getVarbitValue(6590); // 0 is 1, 1 is 5, 2 is 10, 3 is x, 4 is All.

			if (noModSetting == 4) {
				depositMode = ShiftDepositMode.DEPOSIT_1;
				withdrawMode = ShiftWithdrawMode.WITHDRAW_1;
			} else {
				depositMode = ShiftDepositMode.DEPOSIT_ALL;
				withdrawMode = ShiftWithdrawMode.WITHDRAW_ALL;
			}
		}
		else if (client.isKeyPressed(KeyCode.KC_T))
		{
			depositMode = ShiftDepositMode.DEPOSIT_10;
			withdrawMode = ShiftWithdrawMode.WITHDRAW_10;
		}
		else if (shiftModifier())
		{
			// if client is set to 1 and plugin is set to 1, shift should be All.
			// if client is set to something and plugin is set to the same thing, shift should be 1.
			depositMode = config.bankDepositShiftClick();
			withdrawMode = config.bankWithdrawShiftClick();
		}

		if (menuEntryAdded.getTarget().contains("Bucket of sand"))
		{
			withdrawMode = ShiftWithdrawMode.WITHDRAW_X;
		} else if (menuEntryAdded.getTarget().contains("Coal bag")) {
			depositMode = ShiftDepositMode.EXTRA_OP;
		} else if (menuEntryAdded.getTarget().contains("pouch") && !menuEntryAdded.getTarget().toLowerCase().contains("rune")) {
			depositMode = ShiftDepositMode.EXTRA_OP;
		}

		// Swap to shift-click deposit behavior
		// Deposit- op 1 is the current withdraw amount 1/5/10/x for deposit box interface and chambers of xeric storage unit.
		// Deposit- op 2 is the current withdraw amount 1/5/10/x for bank interface

		final int widgetGroupId = WidgetInfo.TO_GROUP(menuEntryAdded.getActionParam1());

		if (depositMode != ShiftDepositMode.OFF
				&& menuEntryAdded.getType() == MenuAction.CC_OP.getId()
				&& (menuEntryAdded.getIdentifier() == 2 || menuEntryAdded.getIdentifier() == 1)
				&& (menuEntryAdded.getOption().startsWith("Deposit-") || menuEntryAdded.getOption().startsWith("Store") || menuEntryAdded.getOption().startsWith("Donate")))
		{
			final int opId = widgetGroupId == WidgetID.DEPOSIT_BOX_GROUP_ID ? depositMode.getIdentifierDepositBox()
					: widgetGroupId == WidgetID.CHAMBERS_OF_XERIC_STORAGE_UNIT_INVENTORY_GROUP_ID ? depositMode.getIdentifierChambersStorageUnit()
					: depositMode.getIdentifier();
			final int actionId = opId >= 6 ? MenuAction.CC_OP_LOW_PRIORITY.getId() : MenuAction.CC_OP.getId();
			bankModeSwap(actionId, opId);
		}

		// Swap to shift-click withdraw behavior
		// Deposit- op 1 is the current withdraw amount 1/5/10/x
		if (withdrawMode != ShiftWithdrawMode.OFF
				&& menuEntryAdded.getType() == MenuAction.CC_OP.getId() && menuEntryAdded.getIdentifier() == 1
				&& menuEntryAdded.getOption().startsWith("Withdraw"))
		{
			boolean isChambersStorageUnit = widgetGroupId == WidgetID.CHAMBERS_OF_XERIC_STORAGE_UNIT_PRIVATE_GROUP_ID || widgetGroupId == WidgetID.CHAMBERS_OF_XERIC_STORAGE_UNIT_SHARED_GROUP_ID;
			final int actionId = isChambersStorageUnit ? MenuAction.CC_OP.getId()
					: withdrawMode.getMenuAction().getId();
			final int opId = isChambersStorageUnit ? withdrawMode.getIdentifierChambersStorageUnit()
					: withdrawMode.getIdentifier();
//			final int opId = WidgetInfo.TO_GROUP(
//				menuEntryAdded.getActionParam1()) == WidgetID.SEED_VAULT_GROUP_ID
//				? withdrawMode.getIdentifierSeedVault()
//				: withdrawMode.getIdentifier();
//			int actionId = withdrawMode.getMenuAction().getId();
//			System.out.println("withdraw (1) option: " + menuEntryAdded.getOption() + " " + opId + " " + actionId);
//			if (WidgetInfo.TO_GROUP(menuEntryAdded.getActionParam1()) == WidgetID.SEED_VAULT_GROUP_ID && opId == 5)
//			{
//				actionId = MenuAction.CC_OP.getId();
//			}
			bankModeSwap(actionId, opId);
		}
	}

	private void swapBuySell(MenuEntryAdded menuEntryAdded)
	{
		String option = Text.removeTags(menuEntryAdded.getOption());

		if (option.matches(BUY_SELL_OPTION_REGEX))
		{
			Matcher matcher = Pattern.compile(BUY_SELL_OPTION_REGEX).matcher(option);
			matcher.matches();
			String operation = matcher.group(1);
			int quantity = Integer.valueOf(matcher.group(2));

			MenuEntry[] menuEntries = client.getMenuEntries();
			MenuEntry setQuantityEntry = new MenuEntry();
			setQuantityEntry.setOption("Set " + operation.toLowerCase() + " quantity " + quantity);
			setQuantityEntry.setTarget(menuEntryAdded.getTarget());
			setQuantityEntry.setType(MenuAction.RUNELITE.getId());
			menuEntries[menuEntries.length - 1] = setQuantityEntry;
			client.setMenuEntries(menuEntries);
		}
		else if ("Value".equals(option))
		{
			MenuEntry[] menuEntries = client.getMenuEntries();
			// Search for Buy or Sell menu entry to figure out if we are in a buy or sell menu.
			for (int i = 0; i < menuEntries.length; i++)
			{
				Matcher matcher = Pattern.compile(SET_BUY_SELL_QUANTITY_REGEX).matcher(Text.removeTags(menuEntries[i].getOption()));
				if (matcher.matches())
				{
					String operation = matcher.group(1);
					MenuEntry resetQuantityEntry = new MenuEntry();
					resetQuantityEntry.setOption("Reset " + operation + " quantity");
					resetQuantityEntry.setTarget(menuEntryAdded.getTarget());
					resetQuantityEntry.setType(MenuAction.RUNELITE.getId());
					menuEntries[menuEntries.length - 1] = resetQuantityEntry;
					client.setMenuEntries(menuEntries);
					break;
				}
			}
		}
	}

	private void bankModeSwap(int entryTypeId, int entryIdentifier)
	{
		MenuEntry[] menuEntries = client.getMenuEntries();

		for (int i = menuEntries.length - 1; i >= 0; --i)
		{
			MenuEntry entry = menuEntries[i];

			if (entry.getType() == entryTypeId && entry.getIdentifier() == entryIdentifier)
			{
				// Raise the priority of the op so it doesn't get sorted later
				entry.setType(MenuAction.CC_OP.getId());

				menuEntries[i] = menuEntries[menuEntries.length - 1];
				menuEntries[menuEntries.length - 1] = entry;

				client.setMenuEntries(menuEntries);
				break;
			}
		}
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
	    System.out.println(System.currentTimeMillis() + ", onMenuOptionClicked" + event.getMenuOption() + ", " + event.getMenuAction() + ", " + event.getMenuTarget() + " " + event.getId() + " " + event.getMenuAction());
//	    MyLog.menuOptionClicked(event.getMenuOption() + " " + event.getMenuTarget());
		if (event.getMenuAction() != MenuAction.RUNELITE)
		{
			return;
		}

		if (event.getWidgetId() == WidgetInfo.INVENTORY.getId())
		{
			int itemId = event.getId();

			if (itemId == -1)
			{
				return;
			}

			String option = event.getMenuOption();
			String target = event.getMenuTarget();
			ItemComposition itemComposition = client.getItemDefinition(itemId);

			if (option.equals(RESET) && target.equals(MENU_TARGET))
			{
				unsetSwapConfig(itemId);
				return;
			}

			if (!itemComposition.getName().equals(Text.removeTags(target)))
			{
				return;
			}

			if (option.equals("Use")) //because "Use" is not in inventoryActions
			{
				setSwapConfig(itemId, -1);
			}
			else
			{
				String[] inventoryActions = itemComposition.getInventoryActions();

				for (int index = 0; index < inventoryActions.length; index++)
				{
					if (option.equals(inventoryActions[index]))
					{
						setSwapConfig(itemId, index);
						break;
					}
				}
			}
		}
		else if (event.getMenuOption().matches(SET_BUY_SELL_QUANTITY_REGEX))
		{
			Matcher matcher = Pattern.compile(SET_BUY_SELL_QUANTITY_REGEX).matcher(event.getMenuOption());
			if (matcher.matches())
			{
				String operation = matcher.group(1);
				int quantity = Integer.valueOf(matcher.group(2));
				System.out.println("here");
				(("buy".equals(operation)) ? buyQuantityMap : sellQuantityMap).put(Text.removeTags(event.getMenuTarget()).toLowerCase(), quantity);
			}
		}
		else if (event.getMenuOption().matches(RESET_BUY_SELL_QUANTITY_REGEX))
		{
			Matcher matcher = Pattern.compile(RESET_BUY_SELL_QUANTITY_REGEX).matcher(event.getMenuOption());
			if (matcher.matches())
			{
				String operation = matcher.group(1);
				System.out.println("here");
				(("buy".equals(operation)) ? buyQuantityMap : sellQuantityMap).remove(Text.removeTags(event.getMenuTarget()).toLowerCase());
			}
		}
	}

	private void swapMenuEntry(int index, MenuEntry menuEntry)
	{
		final int eventId = menuEntry.getIdentifier();
		final MenuAction menuAction = MenuAction.of(menuEntry.getType());
		final String option = Text.removeTags(menuEntry.getOption()).toLowerCase();
		final String target = Text.removeTags(menuEntry.getTarget()).toLowerCase();
		final NPC hintArrowNpc = client.getHintArrowNpc();

		if (hintArrowNpc != null
				&& hintArrowNpc.getIndex() == eventId
				&& NPC_MENU_TYPES.contains(menuAction))
		{
			return;
		}

		if (shiftModifier() && (menuAction == MenuAction.ITEM_FIRST_OPTION
				|| menuAction == MenuAction.ITEM_SECOND_OPTION
				|| menuAction == MenuAction.ITEM_THIRD_OPTION
				|| menuAction == MenuAction.ITEM_FOURTH_OPTION
				|| menuAction == MenuAction.ITEM_FIFTH_OPTION
				|| menuAction == MenuAction.ITEM_USE))
		{
			// Special case use shift click due to items not actually containing a "Use" option, making
			// the client unable to perform the swap itself.
			if (config.shiftClickCustomization() && !option.equals("use"))
			{
				Integer customOption = getSwapConfig(eventId);

				if (customOption != null && customOption == -1)
				{
					swap("use", target, index, true);
				}
			}

			// don't perform swaps on items when shift is held; instead prefer the client menu swap, which
			// we may have overwrote
			return;
		}

		Collection<Swap> swaps = this.swaps.get(option);
		for (Swap swap : swaps)
		{
			if (swap.getTargetPredicate().test(target) && swap.getEnabled().get())
			{
				if (swap(swap.getSwappedOption(), target, index, swap.isStrict()))
				{
					break;
				}
			}
		}
	}

	@Subscribe
	public void onClientTick(ClientTick clientTick)
	{
		// The menu is not rebuilt when it is open, so don't swap or else it will
		// repeatedly swap entries
		if (client.getGameState() != GameState.LOGGED_IN || client.isMenuOpen())
		{
			return;
		}

		MenuEntry[] menuEntries = client.getMenuEntries();

		// Build option map for quick lookup in findIndex
		int idx = 0;
		optionIndexes.clear();
		for (MenuEntry entry : menuEntries)
		{
			System.out.println("menu entry is " + entry.getTarget());
			String option = Text.removeTags(entry.getOption()).toLowerCase();
			optionIndexes.put(option, idx++);
		}

		// Perform swaps
		idx = 0;
		for (MenuEntry entry : menuEntries)
		{
			swapMenuEntry(idx++, entry);
		}
	}

	@Subscribe
	public void onPostItemComposition(PostItemComposition event)
	{
		if (!config.shiftClickCustomization())
		{
			// since shift-click is done by the client we have to check if our shift click customization is on
			// prior to altering the item shift click action index.
			return;
		}

		ItemComposition itemComposition = event.getItemComposition();
		Integer option = getSwapConfig(itemComposition.getId());

		if (option != null)
		{
			itemComposition.setShiftClickActionIndex(option);
		}
	}

	private boolean swap(String option, String target, int index, boolean strict)
	{
		MenuEntry[] menuEntries = client.getMenuEntries();

		// find option to swap with
		int optionIdx = findIndex(menuEntries, index, option, target, strict);

		if (optionIdx >= 0)
		{
			swap(optionIndexes, menuEntries, optionIdx, index);
			return true;
		}

		return false;
	}

	private int findIndex(MenuEntry[] entries, int limit, String option, String target, boolean strict)
	{
		if (strict)
		{
			List<Integer> indexes = optionIndexes.get(option.toLowerCase());

			// We want the last index which matches the target, as that is what is top-most
			// on the menu
			for (int i = indexes.size() - 1; i >= 0; --i)
			{
				int idx = indexes.get(i);
				MenuEntry entry = entries[idx];
				String entryTarget = Text.removeTags(entry.getTarget()).toLowerCase();

				// Limit to the last index which is prior to the current entry
				if (idx < limit && entryTarget.equals(target))
				{
					return idx;
				}
			}
		}
		else
		{
			// Without strict matching we have to iterate all entries up to the current limit...
			for (int i = limit - 1; i >= 0; i--)
			{
				MenuEntry entry = entries[i];
				String entryOption = Text.removeTags(entry.getOption()).toLowerCase();
				String entryTarget = Text.removeTags(entry.getTarget()).toLowerCase();

				if (entryOption.contains(option.toLowerCase()) && entryTarget.equals(target))
				{
					return i;
				}
			}

		}

		return -1;
	}

	private void swap(ArrayListMultimap<String, Integer> optionIndexes, MenuEntry[] entries, int index1, int index2)
	{
		MenuEntry entry1 = entries[index1],
				entry2 = entries[index2];

		entries[index1] = entry2;
		entries[index2] = entry1;

		client.setMenuEntries(entries);

		// Update optionIndexes
		String option1 = Text.removeTags(entry1.getOption()).toLowerCase(),
				option2 = Text.removeTags(entry2.getOption()).toLowerCase();

		List<Integer> list1 = optionIndexes.get(option1),
				list2 = optionIndexes.get(option2);

		// call remove(Object) instead of remove(int)
		list1.remove((Integer) index1);
		list2.remove((Integer) index2);

		sortedInsert(list1, index2);
		sortedInsert(list2, index1);
	}

	private static <T extends Comparable<? super T>> void sortedInsert(List<T> list, T value)
	{
		int idx = Collections.binarySearch(list, value);
		list.add(idx < 0 ? -idx - 1 : idx, value);
	}

	private void removeShiftClickCustomizationMenus()
	{
		menuManager.removeManagedCustomMenu(FIXED_INVENTORY_TAB_CONFIGURE);
		menuManager.removeManagedCustomMenu(FIXED_INVENTORY_TAB_SAVE);
		menuManager.removeManagedCustomMenu(RESIZABLE_BOTTOM_LINE_INVENTORY_TAB_CONFIGURE);
		menuManager.removeManagedCustomMenu(RESIZABLE_BOTTOM_LINE_INVENTORY_TAB_SAVE);
		menuManager.removeManagedCustomMenu(RESIZABLE_INVENTORY_TAB_CONFIGURE);
		menuManager.removeManagedCustomMenu(RESIZABLE_INVENTORY_TAB_SAVE);
	}

	private void refreshShiftClickCustomizationMenus()
	{
		removeShiftClickCustomizationMenus();
		if (configuringShiftClick)
		{
			menuManager.addManagedCustomMenu(FIXED_INVENTORY_TAB_SAVE);
			menuManager.addManagedCustomMenu(RESIZABLE_BOTTOM_LINE_INVENTORY_TAB_SAVE);
			menuManager.addManagedCustomMenu(RESIZABLE_INVENTORY_TAB_SAVE);
		}
		else
		{
			menuManager.addManagedCustomMenu(FIXED_INVENTORY_TAB_CONFIGURE);
			menuManager.addManagedCustomMenu(RESIZABLE_BOTTOM_LINE_INVENTORY_TAB_CONFIGURE);
			menuManager.addManagedCustomMenu(RESIZABLE_INVENTORY_TAB_CONFIGURE);
		}
	}

	private boolean shiftModifier()
	{
		return client.isKeyPressed(KeyCode.KC_SHIFT);
	}
}
