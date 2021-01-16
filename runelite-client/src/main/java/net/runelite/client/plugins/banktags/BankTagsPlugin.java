/*
 * Copyright (c) 2018, Adam <Adam@sigterm.info>
 * Copyright (c) 2018, Ron Young <https://github.com/raiyni>
 * Copyright (c) 2018, Tomas Slusny <slusnucky@gmail.com>
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
package net.runelite.client.plugins.banktags;

import com.google.common.collect.Lists;
import com.google.common.primitives.Shorts;
import com.google.inject.Provides;

import java.awt.*;
import java.awt.Point;
import java.awt.event.MouseWheelEvent;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.inject.Inject;

import net.runelite.api.*;
import net.runelite.api.events.DraggingWidgetChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GrandExchangeSearched;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.ScriptCallbackEvent;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.JavaScriptCallback;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemVariationMapping;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.game.chatbox.ChatboxPanelManager;
import net.runelite.client.input.MouseManager;
import net.runelite.client.input.MouseWheelListener;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.banktags.tabs.TabInterface;
import static net.runelite.client.plugins.banktags.tabs.TabInterface.FILTERED_CHARS;
import net.runelite.client.plugins.banktags.tabs.TabSprites;
import net.runelite.client.plugins.banktags.tabs.TagTab;
import net.runelite.client.plugins.cluescrolls.ClueScrollPlugin;
import net.runelite.client.util.Text;

@PluginDescriptor(
	name = "Bank Tags",
	description = "Enable tagging of bank items and searching of bank tags",
	tags = {"searching", "tagging"}
)
@PluginDependency(ClueScrollPlugin.class)
public class BankTagsPlugin extends Plugin implements MouseWheelListener
{
	public static final String CONFIG_GROUP = "banktags";
	public static final String TAG_SEARCH = "tag:";
	private static final String EDIT_TAGS_MENU_OPTION = "Edit-tags";
	public static final String ICON_SEARCH = "icon_";
	public static final String TAG_TABS_CONFIG = "tagtabs";
	public static final String VAR_TAG_SUFFIX = "*";
	private static final int ITEMS_PER_ROW = 8;
	private static final int ITEM_VERTICAL_SPACING = 36;
	private static final int ITEM_HORIZONTAL_SPACING = 48;
	private static final int ITEM_ROW_START = 51;

	private static final int MAX_RESULT_COUNT = 250;

	private static final String SEARCH_BANK_INPUT_TEXT =
		"Show items whose names or tags contain the following text:<br>" +
			"(To show only tagged items, start your search with 'tag:')";
	private static final String SEARCH_BANK_INPUT_TEXT_FOUND =
		"Show items whose names or tags contain the following text: (%d found)<br>" +
			"(To show only tagged items, start your search with 'tag:')";

	@Inject
	private ItemManager itemManager;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ChatboxPanelManager chatboxPanelManager;

	@Inject
	private MouseManager mouseManager;

	@Inject
	private BankTagsConfig config;

	@Inject
	private TagManager tagManager;

	@Inject
	private TabInterface tabInterface;

	@Inject
	private SpriteManager spriteManager;

	@Inject
	private ConfigManager configManager;

	@Provides
	BankTagsConfig getConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BankTagsConfig.class);
	}

	@Override
	public void resetConfiguration()
	{
		List<String> extraKeys = Lists.newArrayList(
			CONFIG_GROUP + "." + TagManager.ITEM_KEY_PREFIX,
			CONFIG_GROUP + "." + ICON_SEARCH,
			CONFIG_GROUP + "." + TAG_TABS_CONFIG
		);

		for (String prefix : extraKeys)
		{
			List<String> keys = configManager.getConfigurationKeys(prefix);
			for (String key : keys)
			{
				String[] str = key.split("\\.", 2);
				if (str.length == 2)
				{
					configManager.unsetConfiguration(str[0], str[1]);
				}
			}
		}

		clientThread.invokeLater(() ->
		{
			tabInterface.destroy();
			tabInterface.init();
		});
	}


	@Override
	public void startUp()
	{
		cleanConfig();
		mouseManager.registerMouseWheelListener(this);
		clientThread.invokeLater(tabInterface::init);
		spriteManager.addSpriteOverrides(TabSprites.values());
	}

	@Deprecated
	private void cleanConfig()
	{
		removeInvalidTags("tagtabs");

		List<String> tags = configManager.getConfigurationKeys(CONFIG_GROUP + ".item_");
		tags.forEach(s ->
		{
			String[] split = s.split("\\.", 2);
			removeInvalidTags(split[1]);
		});

		List<String> icons = configManager.getConfigurationKeys(CONFIG_GROUP + ".icon_");
		icons.forEach(s ->
		{
			String[] split = s.split("\\.", 2);
			String replaced = split[1].replaceAll("[<>/]", "");
			if (!split[1].equals(replaced))
			{
				String value = configManager.getConfiguration(CONFIG_GROUP, split[1]);
				configManager.unsetConfiguration(CONFIG_GROUP, split[1]);
				if (replaced.length() > "icon_".length())
				{
					configManager.setConfiguration(CONFIG_GROUP, replaced, value);
				}
			}
		});
	}

	@Deprecated
	private void removeInvalidTags(final String key)
	{
		final String value = configManager.getConfiguration(CONFIG_GROUP, key);
		if (value == null)
		{
			return;
		}

		String replaced = value.replaceAll("[<>:/]", "");
		if (!value.equals(replaced))
		{
			replaced = Text.toCSV(Text.fromCSV(replaced));
			if (replaced.isEmpty())
			{
				configManager.unsetConfiguration(CONFIG_GROUP, key);
			}
			else
			{
				configManager.setConfiguration(CONFIG_GROUP, key, replaced);
			}
		}
	}

	@Override
	public void shutDown()
	{
		mouseManager.unregisterMouseWheelListener(this);
		clientThread.invokeLater(tabInterface::destroy);
		spriteManager.removeSpriteOverrides(TabSprites.values());
	}

	@Subscribe
	public void onGrandExchangeSearched(GrandExchangeSearched event)
	{
		final String input = client.getVar(VarClientStr.INPUT_TEXT);
		if (!input.startsWith(TAG_SEARCH))
		{
			return;
		}

		event.consume();

		final String tag = input.substring(TAG_SEARCH.length()).trim();
		final Set<Integer> ids = tagManager.getItemsForTag(tag)
			.stream()
			.mapToInt(Math::abs)
			.mapToObj(ItemVariationMapping::getVariations)
			.flatMap(Collection::stream)
			.distinct()
			.filter(i -> itemManager.getItemComposition(i).isTradeable())
			.limit(MAX_RESULT_COUNT)
			.collect(Collectors.toCollection(TreeSet::new));

		client.setGeSearchResultIndex(0);
		client.setGeSearchResultCount(ids.size());
		client.setGeSearchResultIds(Shorts.toArray(ids));
	}

	@Subscribe
	public void onScriptCallbackEvent(ScriptCallbackEvent event)
	{
		String eventName = event.getEventName();

		int[] intStack = client.getIntStack();
		String[] stringStack = client.getStringStack();
		int intStackSize = client.getIntStackSize();
		int stringStackSize = client.getStringStackSize();

		tabInterface.handleScriptEvent(event);

		switch (eventName)
		{
			case "setSearchBankInputText":
				stringStack[stringStackSize - 1] = SEARCH_BANK_INPUT_TEXT;
				break;
			case "setSearchBankInputTextFound":
			{
				int matches = intStack[intStackSize - 1];
				stringStack[stringStackSize - 1] = String.format(SEARCH_BANK_INPUT_TEXT_FOUND, matches);
				break;
			}
			case "bankSearchFilter":
				final int itemId = intStack[intStackSize - 1];
				final String searchfilter = stringStack[stringStackSize - 1];

				// This event only fires when the bank is in search mode. It will fire even if there is no search
				// input. We prevent having a tag tab open while also performing a normal search, so if a tag tab
				// is active here it must mean we have placed the bank into search mode. See onScriptPostFired().
				TagTab activeTab = tabInterface.getActiveTab();
				String search = activeTab != null ? TAG_SEARCH + activeTab.getTag() : searchfilter;

				if (search.isEmpty())
				{
					return;
				}

				boolean tagSearch = search.startsWith(TAG_SEARCH);
				if (tagSearch)
				{
					search = search.substring(TAG_SEARCH.length()).trim();
				}

				if (tagManager.findTag(itemId, search))
				{
					// return true
					intStack[intStackSize - 2] = 1;
				}
				else if (tagSearch)
				{
					// if the item isn't tagged we return false to prevent the item matching if the item name happens
					// to contain the tag name.
					intStack[intStackSize - 2] = 0;
				}
				break;
			case "getSearchingTagTab":
				intStack[intStackSize - 1] = tabInterface.isActive() ? 1 : 0;
				break;
		}
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		MenuEntry[] entries = client.getMenuEntries();

		if (event.getActionParam1() == WidgetInfo.BANK_ITEM_CONTAINER.getId()
			&& event.getOption().equals("Examine"))
		{
			Widget container = client.getWidget(WidgetInfo.BANK_ITEM_CONTAINER);
			Widget item = container.getChild(event.getActionParam0());
			int itemID = item.getItemId();
			String text = EDIT_TAGS_MENU_OPTION;
			int tagCount = tagManager.getTags(itemID, false).size() + tagManager.getTags(itemID, true).size();

			if (tagCount > 0)
			{
				text += " (" + tagCount + ")";
			}

			MenuEntry editTags = new MenuEntry();
			editTags.setParam0(event.getActionParam0());
			editTags.setParam1(event.getActionParam1());
			editTags.setTarget(event.getTarget());
			editTags.setOption(text);
			editTags.setType(MenuAction.RUNELITE.getId());
			editTags.setIdentifier(event.getIdentifier());
			entries = Arrays.copyOf(entries, entries.length + 1);
			entries[entries.length - 1] = editTags;
			client.setMenuEntries(entries);
		}

		tabInterface.handleAdd(event);
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (event.getWidgetId() == WidgetInfo.BANK_ITEM_CONTAINER.getId()
			&& event.getMenuAction() == MenuAction.RUNELITE
			&& event.getMenuOption().startsWith(EDIT_TAGS_MENU_OPTION))
		{
			event.consume();
			int inventoryIndex = event.getActionParam();
			ItemContainer bankContainer = client.getItemContainer(InventoryID.BANK);
			if (bankContainer == null)
			{
				return;
			}
			Item[] items = bankContainer.getItems();
			if (inventoryIndex < 0 || inventoryIndex >= items.length)
			{
				return;
			}
			Item item = bankContainer.getItems()[inventoryIndex];
			if (item == null)
			{
				return;
			}

			int itemId = item.getId();
			ItemComposition itemComposition = itemManager.getItemComposition(itemId);
			String name = itemComposition.getName();

			// Get both tags and vartags and append * to end of vartags name
			Collection<String> tags = tagManager.getTags(itemId, false);
			tagManager.getTags(itemId, true).stream()
				.map(i -> i + "*")
				.forEach(tags::add);

			String initialValue = Text.toCSV(tags);

			chatboxPanelManager.openTextInput(name + " tags:<br>(append " + VAR_TAG_SUFFIX + " for variation tag)")
				.addCharValidator(FILTERED_CHARS)
				.value(initialValue)
				.onDone((Consumer<String>) (newValue) ->
					clientThread.invoke(() ->
					{
						// Split inputted tags to vartags (ending with *) and regular tags
						final Collection<String> newTags = new ArrayList<>(Text.fromCSV(newValue.toLowerCase()));
						final Collection<String> newVarTags = new ArrayList<>(newTags).stream().filter(s -> s.endsWith(VAR_TAG_SUFFIX)).map(s ->
						{
							newTags.remove(s);
							return s.substring(0, s.length() - VAR_TAG_SUFFIX.length());
						}).collect(Collectors.toList());

						// And save them
						tagManager.setTagString(itemId, Text.toCSV(newTags), false);
						tagManager.setTagString(itemId, Text.toCSV(newVarTags), true);

						// Check both previous and current tags in case the tag got removed in new tags or in case
						// the tag got added in new tags
						tabInterface.updateTabIfActive(Text.fromCSV(initialValue.toLowerCase().replaceAll(Pattern.quote(VAR_TAG_SUFFIX), "")));
						tabInterface.updateTabIfActive(Text.fromCSV(newValue.toLowerCase().replaceAll(Pattern.quote(VAR_TAG_SUFFIX), "")));
					}))
				.build();
		}
		else
		{
			tabInterface.handleClick(event);
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged configChanged)
	{
		if (configChanged.getGroup().equals(CONFIG_GROUP) && configChanged.getKey().equals("useTabs"))
		{
			if (config.tabs())
			{
				clientThread.invokeLater(tabInterface::init);
			}
			else
			{
				clientThread.invokeLater(tabInterface::destroy);
			}
		}
	}

	@Subscribe
	public void onScriptPreFired(ScriptPreFired event)
	{
		int scriptId = event.getScriptId();
		if (scriptId == ScriptID.BANKMAIN_FINISHBUILDING)
		{
			// Since we apply tag tab search filters even when the bank is not in search mode,
			// bankkmain_build will reset the bank title to "The Bank of Gielinor". So apply our
			// own title.
			TagTab activeTab = tabInterface.getActiveTab();
			if (tabInterface.isTagTabActive())
			{
				// Tag tab tab has its own title since it isn't a real tag
				Widget bankTitle = client.getWidget(WidgetInfo.BANK_TITLE_BAR);
				bankTitle.setText("Tag tab tab");
			}
			else if (activeTab != null)
			{
				Widget bankTitle = client.getWidget(WidgetInfo.BANK_TITLE_BAR);
				bankTitle.setText("Tag tab <col=ff0000>" + activeTab.getTag() + "</col>");
			}
		}
		else if (scriptId == ScriptID.BANKMAIN_SEARCH_TOGGLE)
		{
			tabInterface.handleSearch();
		}
	}

	@Subscribe
	public void onScriptPostFired(ScriptPostFired event)
	{
		if (event.getScriptId() == ScriptID.BANKMAIN_BUILD)
		{
			TagTab activeTab = tabInterface.getActiveTab();
			if (activeTab != null) {
				applyCustomBankTagItemPositions(activeTab);
			}
		}
		if (event.getScriptId() == ScriptID.BANKMAIN_SEARCHING)
		{
			// The return value of bankmain_searching is on the stack. If we have a tag tab active
			// make it return true to put the bank in a searching state.
			if (tabInterface.getActiveTab() != null || tabInterface.isTagTabActive())
			{
				client.getIntStack()[client.getIntStackSize() - 1] = 1; // true
			}
			return;
		}

		if (event.getScriptId() != ScriptID.BANKMAIN_BUILD || !config.removeSeparators())
		{
			return;
		}

		if (!tabInterface.isActive())
		{
			return;
		}

		Widget itemContainer = client.getWidget(WidgetInfo.BANK_ITEM_CONTAINER);
		if (itemContainer == null)
		{
			return;
		}

		int items = 0;

		Widget[] containerChildren = itemContainer.getDynamicChildren();

		// sort the child array as the items are not in the displayed order
		Arrays.sort(containerChildren, Comparator.comparing(Widget::getOriginalY)
			.thenComparing(Widget::getOriginalX));

		for (Widget child : containerChildren)
		{
			if (child.getItemId() != -1 && !child.isHidden())
			{
				// calculate correct item position as if this was a normal tab
				int adjYOffset = (items / ITEMS_PER_ROW) * ITEM_VERTICAL_SPACING;
				int adjXOffset = (items % ITEMS_PER_ROW) * ITEM_HORIZONTAL_SPACING + ITEM_ROW_START;

				if (child.getOriginalY() != adjYOffset)
				{
					child.setOriginalY(adjYOffset);
					child.revalidate();
				}

				if (child.getOriginalX() != adjXOffset)
				{
					child.setOriginalX(adjXOffset);
					child.revalidate();
				}

				items++;
			}

			// separator line or tab text
			if (child.getSpriteId() == SpriteID.RESIZEABLE_MODE_SIDE_PANEL_BACKGROUND
				|| child.getText().contains("Tab"))
			{
				child.setHidden(true);
			}
		}

		final Widget bankItemContainer = client.getWidget(WidgetInfo.BANK_ITEM_CONTAINER);
		int itemContainerHeight = bankItemContainer.getHeight();
		// add a second row of height here to allow users to scroll down when the last row is partially visible
		int adjustedScrollHeight = (items / ITEMS_PER_ROW) * ITEM_VERTICAL_SPACING + ITEM_VERTICAL_SPACING;
		itemContainer.setScrollHeight(Math.max(adjustedScrollHeight, itemContainerHeight));

		final int itemContainerScroll = bankItemContainer.getScrollY();
		clientThread.invokeLater(() ->
			client.runScript(ScriptID.UPDATE_SCROLLBAR,
				WidgetInfo.BANK_SCROLLBAR.getId(),
				WidgetInfo.BANK_ITEM_CONTAINER.getId(),
				itemContainerScroll));

	}

	public static Widget lastDraggedOnWidget = null;

	private void applyCustomBankTagItemPositions(TagTab activeTab) {
		Widget[] bankItems = client.getWidget(WidgetInfo.BANK_ITEM_CONTAINER).getDynamicChildren();
		Map<Integer, Widget> itemIdToWidget = new HashMap<>();
//		System.out.println("modifying. number of children: " + bankItems.length);

		String bankTagName = activeTab.getTag();
		Map<Integer, Integer> itemPositionIndexes = getBankOrder(bankTagName);

		for (Widget bankItem : bankItems) {
			if (bankItem.isHidden()) continue;
			int itemId = bankItem.getItemId();
			if (itemId < 0) continue; // item id -1 is used for something but idk what. It's not an item so I don't want it.
			itemId = getNonPlaceholderId(itemId);

			itemIdToWidget.put(itemId, bankItem);

			bankItem.setOnDragCompleteListener((JavaScriptCallback) (ev) -> {
				customBankTagOrderInsert(bankTagName, ev.getSource().getItemId(), (lastDraggedOnWidget == null) ? -2 : lastDraggedOnWidget.getItemId());
			});

			// TODO remove items that were deleted from the bank tag? Wouldn't go in here because items not in bank but in the bank tab will show up here, but it's something to think about for the future.
            // This is also a problem if you remove an item, drag another item to its position, and re-add the item. It will show up on top of the other item.
			if (!itemPositionIndexes.containsKey(itemId)) {
				assignPosition(itemPositionIndexes, itemId);
			}
		}
		System.out.println("tag items: " + itemPositionIndexes.toString());

		setItemPositions(itemPositionIndexes);
		configManager.setConfiguration(CONFIG_GROUP, "custom_banktagorder_" + bankTagName, bankTagOrderMapToString(itemPositionIndexes));
		System.out.println("saved tag " + bankTagName);
	}

	private Map<Integer, Integer> getBankOrder(String bankTagName) {
		String configuration = configManager.getConfiguration(CONFIG_GROUP, "custom_banktagorder_" + bankTagName);
		return (configuration == null) ? new HashMap<>() : bankTagOrderStringToMap(configuration);
	}

	private String bankTagOrderMapToString(Map<Integer, Integer> itemPositionMap) {
	    StringBuilder sb = new StringBuilder();
		for (Map.Entry<Integer, Integer> integerIntegerEntry : itemPositionMap.entrySet()) {
			sb.append(integerIntegerEntry.getKey() + ":" + integerIntegerEntry.getValue() + ",");
		}
		if (sb.length() > 0) {
			sb.delete(sb.length() - 1, sb.length());
		}
		return sb.toString();
	}

	private Map<Integer, Integer> bankTagOrderStringToMap(String s) {
	    Map<Integer, Integer> map = new HashMap<>();
		for (String s1 : s.split(",")) {
			String[] split = s1.split(":");
			map.put(Integer.valueOf(split[0]), Integer.valueOf(split[1]));
		}
		return map;
	}

	private void setItemPositions(Map<Integer, Integer> itemPositionIndexes) {
		Widget container = client.getWidget(WidgetInfo.BANK_ITEM_CONTAINER);
		for (Widget child : container.getDynamicChildren()) {
			int itemId = getNonPlaceholderId(child.getItemId());
			Integer integer = itemPositionIndexes.get(itemId);
			if (integer == null) {
				// This hides things like the tab dividers and those weird spacer things that occupy the empty space. These can interfere with dragging.
				child.setHidden(true);
				child.revalidate();
				continue;
			}
			// TODO (unrelated to the surrounding code) show on bank item when it's in your inventory or equipped. This is good for items that I have multiple of - it'll be easier to tell if I have the item withdrawn already. For items I already have 1 of, placeholders fulfill this purpose.
			child.setOriginalX((integer % 8) * 48 + 51);
			child.setOriginalY((integer / 8) * 36);
			child.revalidate();
		}
		container.setScrollHeight(2000);
		final int itemContainerScroll = container.getScrollY();
		clientThread.invokeLater(() ->
				client.runScript(ScriptID.UPDATE_SCROLLBAR,
						WidgetInfo.BANK_SCROLLBAR.getId(),
						WidgetInfo.BANK_ITEM_CONTAINER.getId(),
						itemContainerScroll));
	}

	private int getNonPlaceholderId(int id) {
		ItemComposition itemComposition = itemManager.getItemComposition(id);
		return (itemComposition.getPlaceholderTemplateId() == 14401) ? itemComposition.getPlaceholderId() : id;
	}

	private void customBankTagOrderInsert(String bankTagName, int draggedItemId, int draggedOnItemId) {
	    draggedItemId = getNonPlaceholderId(draggedItemId);
		draggedOnItemId = getNonPlaceholderId(draggedOnItemId);
		net.runelite.api.Point mouseCanvasPosition = client.getMouseCanvasPosition();
		Widget container = client.getWidget(WidgetInfo.BANK_ITEM_CONTAINER);
		net.runelite.api.Point point = new net.runelite.api.Point(mouseCanvasPosition.getX() - container.getCanvasLocation().getX(), mouseCanvasPosition.getY() - container.getCanvasLocation().getY());
		System.out.println("mouse canvas position: " + mouseCanvasPosition + " bank widget canvas position: " + container.getCanvasLocation());
		System.out.println("bank container relative position: " + point);
//		System.out.println("mouse drag complete " + draggedItemId + " " + draggedOnItemId + " " + ((lastDraggedOnWidget == null) ? "null" : lastDraggedOnWidget.getItemId()) + " " + ((draggedWidget == null) ? "null" : draggedWidget.getItemId()));

		Map<Integer, Integer> itemIdToIndexes = getBankOrder(bankTagName);

		int row = (point.getY() + container.getScrollY()) / 36;
		int col = (point.getX() - 51) / 48;
		System.out.println("row col " + row + " " + col + " " + row * 8 + col);
		Integer draggedOnItemIndex = (lastDraggedOnWidget != null) ? itemIdToIndexes.get(draggedOnItemId) : row * 8 + col;
		System.out.println("dragged on item index is " + draggedOnItemIndex);
		System.out.println("dragged on item id: " + draggedOnItemId + " " + draggedItemId);
		Integer draggedItemIndex = itemIdToIndexes.get(draggedItemId);
		if (draggedOnItemIndex == null) {
			System.out.println("DRAGGED ON ITEM WAS NULL " + lastDraggedOnWidget.getName());
		}
		if (draggedItemIndex == null) {
			System.out.println("DRAGGED ITEM WAS NULL");
		}

//		boolean isInsert = client.getVar(Varbits.BANK_REARRANGE_MODE) == 1;
//		if (isInsert) {
//			Integer index = itemIdToIndexes.get(draggedOnItemId);
//		} else {
			itemIdToIndexes.put(draggedItemId, draggedOnItemIndex);
			itemIdToIndexes.put(draggedOnItemId, draggedItemIndex);
			configManager.setConfiguration(CONFIG_GROUP, "custom_banktagorder_" + bankTagName, bankTagOrderMapToString(itemIdToIndexes));
			System.out.println("saved tag " + bankTagName);
//		}

		System.out.println("tag items: " + itemIdToIndexes.toString());

		setItemPositions(itemIdToIndexes);
	}

	private static void assignPosition(Map<Integer, Integer> itemPositionIndexes, int itemId) {
		int smallestIndex = 0;
		ArrayList<Integer> integers = new ArrayList<>(itemPositionIndexes.values());
		integers.sort(Integer::compare);
		for (Integer integer : integers) {
			if (smallestIndex == integer) {
				smallestIndex++;
			} else if (integer > smallestIndex) {
				break;
			}
		}
		System.out.println("assigning position for item " + itemId + " to " + smallestIndex);
		itemPositionIndexes.put(itemId, smallestIndex);
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		tabInterface.update();
	}

	@Subscribe
	public void onDraggingWidgetChanged(DraggingWidgetChanged event)
	{
		final boolean shiftPressed = client.isKeyPressed(KeyCode.KC_SHIFT);
		tabInterface.handleDrag(event.isDraggingWidget(), shiftPressed);
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		if (event.getGroupId() == WidgetID.BANK_GROUP_ID)
		{
			tabInterface.init();
		}
	}

	@Override
	public MouseWheelEvent mouseWheelMoved(MouseWheelEvent event)
	{
		tabInterface.handleWheel(event);
		return event;
	}
}
