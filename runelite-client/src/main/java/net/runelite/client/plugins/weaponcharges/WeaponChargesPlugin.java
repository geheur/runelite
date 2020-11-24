/*
 * Copyright (c) 2018, JerwuQu <marcus@ramse.se>
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

package net.runelite.client.plugins.weaponcharges;

import com.google.inject.Provides;
import java.awt.Color;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.inject.Inject;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.AnimationID;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.ItemID;
import net.runelite.api.Player;
import net.runelite.api.VarPlayer;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.SoundEffectPlayed;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.kit.KitType;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.util.Text;

@PluginDescriptor(
	name = "Weapon Charges",
	description = "Displays ibans blast and swamp trident charges on the inventory icon or as an infobox.",
	tags = {"iban", "trident", "charge"}
)
@Slf4j
public class WeaponChargesPlugin extends Plugin
{
	private static final int WEAPON_SLOT = EquipmentInventorySlot.WEAPON.getSlotIdx();
	private static final Pattern ibansCheckString = Pattern.compile("You have ([0-9]+) charges left on the staff.");
	private static final String ibansRechargeString = "You hold the staff above the well...";
	private static final Pattern tridentCheckString = Pattern.compile("Your weapon has ([0-9,]+) charges.");
//	private static final String tridentRechargeString = "You hold the staff above the well..."; // There is no chat message, but there is a dialog.
	private static final int MAGIC_IBANS_BLAST = 708;
	private static final int SWAMP_TRIDENT_CAST = 1167;

	private ChargedWeapon rechargedWeapon;

	@Inject
	private Client client;

	@Inject
	private WeaponChargesOverlay overlay;

	@Inject
	private WeaponChargesItemOverlay itemOverlay;

	@Inject
	private ItemManager itemManager;

	@Inject
	private WeaponChargesConfig config;

	@Inject
	private InfoBoxManager infoBoxManager;

	@Inject
	private ClientThread clientThread;

	@Inject
	private OverlayManager overlayManager;

	@Provides
	WeaponChargesConfig getConfig(ConfigManager configManager)
	{
		return configManager.getConfig(WeaponChargesConfig.class);
	}

	@Override
	protected void startUp() throws Exception
	{
		System.out.println("startup");
		processChargesCounter();
		overlayManager.add(overlay);
		overlayManager.add(itemOverlay);
	}

	@Override
	protected void shutDown() throws Exception
	{
		System.out.println("shutdown");
//		removeChargesCounter();
		overlayManager.remove(overlay);
		overlayManager.remove(itemOverlay);
	}

	Integer getCharges(ChargedWeapon weapon)
	{
		if ((weapon == ChargedWeapon.IBANS_STAFF ||
			weapon == ChargedWeapon.IBANS_STAFF_U) &&
			config.ibansCharges() >= 0)
		{
			return config.ibansCharges();
		} else if (weapon == ChargedWeapon.TRIDENT_OF_THE_SWAMP) {
			return config.swampTridentCharges();
		}
		else
		{
			return null;
		}
	}

	@Getter
	private boolean ibansStaffEquipped = false;

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event.getItemContainer() != client.getItemContainer(InventoryID.EQUIPMENT))
		{
			return;
		}

		if (
			event.getItemContainer().contains(ItemID.IBANS_STAFF) ||
			event.getItemContainer().contains(ItemID.IBANS_STAFF_U)
		) {
			ibansStaffEquipped = true;
		} else {
			ibansStaffEquipped = false;
		}
	}

	Color getChargesColor(Integer charges)
	{
		if (charges == null)
		{
			return Color.MAGENTA;
		}
		else if (charges > 100)
		{
			return Color.WHITE;
		}
		else
		{
			return Color.RED;
		}
	}

//	private void addChargesCounter(ChargedWeapon weapon)
//	{
//		if (counter != null)
//		{
//			if (counter.getchargedweapon() == weapon)
//			{
//				return;
//			}
//
//			removechargescounter();
//		}
//
//		counter = new weaponchargescounter(itemmanager.getimage(weapon.getitemid()), this, weapon);
//		infoBoxManager.addInfoBox(counter);
//		log.debug("Added Weapon Charges Counter");
//	}
//
//	private void removeChargesCounter()
//	{
//		if (counter != null)
//		{
//			infoBoxManager.removeInfoBox(counter);
//			counter = null;
//			log.debug("Removed Weapon Charges Counter");
//		}
//	}
//
	private void processChargesCounter(final ItemContainer equipmentContainer)
	{
		final ChargedWeapon equippedWeapon = getEquippedWeapon(equipmentContainer);
//		if (config.showChargesInfoBox() && equippedWeapon != null)
//		{
//			clientThread.invokeLater(() -> addChargesCounter(equippedWeapon));
//		}
//		else
//		{
//			removeChargesCounter();
//		}
	}

	private void processChargesCounter()
	{
		processChargesCounter(client.getItemContainer(InventoryID.EQUIPMENT));
	}

	ChargedWeapon getChargedWeaponFromId(int itemId)
	{
		for (ChargedWeapon weapon : ChargedWeapon.values())
		{
			if (itemId == weapon.getItemId())
			{
				return weapon;
			}
		}

		return null;
	}

	private ChargedWeapon getEquippedWeapon(final ItemContainer equipmentContainer)
	{
		if (equipmentContainer != null)
		{
			Item[] equippedItems = equipmentContainer.getItems();
			if (equippedItems.length >= WEAPON_SLOT)
			{
				return getChargedWeaponFromId(equippedItems[WEAPON_SLOT].getId());
			}
		}

		return null;
	}

	@Subscribe
	private void onConfigChanged(ConfigChanged event)
	{
		processChargesCounter();
	}

	private SimpleWeapon lastWeaponChecked = null;

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
	    if (event.getMenuOption().equalsIgnoreCase("check")) {
			for (SimpleWeapon simpleWeapon : simpleWeapons) {
				if (event.getMenuTarget().toLowerCase().contains(simpleWeapon.name.toLowerCase())) {
					System.out.println("setting last weapon checked to " + simpleWeapon.name);
					lastWeaponChecked = simpleWeapon;
					break;
				}
			}
		}
		final String menuTarget = event.getMenuTarget();
		for (ChargedWeapon weapon : ChargedWeapon.values())
		{
			if (menuTarget.contains(">" + weapon.getItemName() + "<"))
			{
				rechargedWeapon = weapon;
				return;
			}
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event.getType() != ChatMessageType.GAMEMESSAGE && event.getType() != ChatMessageType.SPAM)
		{
			return;
		}

		String message = Text.removeTags(event.getMessage());

		if (lastWeaponChecked != null) {
			String numbers = message.replaceAll("[^0-9]", "");
			System.out.println("number string: " + numbers);
			if (numbers.length() > 0) {
				setWeaponCharges(lastWeaponChecked.itemId, Integer.valueOf(numbers));
				lastWeaponChecked = null;
			}
		}

//		checkMessageTrident(message);
		chatMessageBlowpipe(message);
	}

	private void chatMessageBlowpipe(String chatMsg)
	{
		//		if (chatMsg.contains(OUT_OF_DARTS))
//		{
//			if (config.showEmptyBlowpipeNotification())
//			{
//				notifier.notify("You have run out of darts!");
//			}
//
//			return;
//		}
//		if (chatMsg.contains(OUT_OF_SCALES))
//		{
//			if (config.showEmptyBlowpipeNotification())
//			{
//				notifier.notify("You have run out of scales!");
//			}
//
//			return;
//		}

		//Extract dart quantity and type as well as number of scales
		Matcher matcher = DART_AND_SCALE_PATTERN.matcher(chatMsg);

		if (matcher.find())
		{
			dartId = getDartIdByName(matcher.group(1));
			dartsLeft = Integer.valueOf(matcher.group(2).replace(",", ""));
			scalesLeft = Integer.valueOf(matcher.group(3).replace(",", ""));

			config.dartsLeft(dartsLeft);
			System.out.println("setting scales to " + scalesLeft);
			config.scalesLeft(scalesLeft);
			config.dartType(dartId);
		}
	}

	private void checkMessageTrident(String message)
	{
		// Recharge
		if (message.equals(ibansRechargeString))
		{
			config.ibansCharges(rechargedWeapon.getRechargeAmount());
		}

		// Check charges
		Matcher match = ibansCheckString.matcher(message);
		if (match.find())
		{
			config.ibansCharges(Integer.parseInt(match.group(1)));
		}

		// Check charges
		match = tridentCheckString.matcher(message);
		if (match.find())
		{
			int charges = Integer.parseInt(match.group(1).replaceAll(",", ""));
			System.out.println("trident string found: " + charges);
			config.swampTridentCharges(charges);
		}
	}

	@Subscribe
	public void onAnimationChanged(AnimationChanged event)
	{
		if (event.getActor() == client.getLocalPlayer()) {

//			System.out.println(client.getTickCount() + " animation changed: " + event.getActor().getAnimation());
		}

		final Actor actor = event.getActor();
		if (actor != client.getLocalPlayer()) return;

		for (SimpleWeapon simpleWeapon : simpleWeapons) {
			if (simpleWeapon.animationIds.contains(actor.getAnimation()) && simpleWeapon.itemId == client.getLocalPlayer().getPlayerComposition().getEquipmentId(KitType.WEAPON)) {
//			    System.out.println("subtracting charge from weapon " + simpleWeapon.itemId);
				addWeaponCharges(simpleWeapon.itemId, -1);
			}
		}
//		if (actor.getAnimation() == MAGIC_IBANS_BLAST)
//		{
//			config.ibansCharges(config.ibansCharges() - 1);
//		} else if (actor.getAnimation() == SWAMP_TRIDENT_CAST)
//		{
//			System.out.println("swamp trident cast, reducing charges: " + config.swampTridentCharges());
//			config.swampTridentCharges(config.swampTridentCharges() - 1);
//			System.out.println("charges are now: " + config.swampTridentCharges());
//		}
	}

	public static int dartsLeft = -1;
	public static int scalesLeft = -1;
	public static int dartId;

	private int ticks = 0;
	private int ticksInAnimation;
	private int attackStyleVarbit = -1;

	private static final Random RANDOM = new Random();
	private static final Pattern DART_AND_SCALE_PATTERN = Pattern.compile("Darts: (\\S*)(?: dart)? x (\\d*[,]?\\d*). Scales: (\\d*[,]?\\d*) \\(\\d+[.]?\\d%\\).");

	public Integer getWeaponCharges(int itemId) {
		Map<Integer, Integer> chargeMap;
	    try {
			chargeMap = Arrays.stream(config.chargesMap().split(",")).collect(Collectors.toMap(s -> Integer.valueOf(s.split(":")[0]), s -> Integer.valueOf(s.split(":")[1])));
		} catch (NumberFormatException | ClassCastException e) {
	    	System.out.println(e.getMessage());
	    	config.chargesMap("");
            return null;
		}

		if (chargeMap == null) chargeMap = new HashMap<>();
		return chargeMap.get(itemId);
	}

	public void setWeaponCharges(int itemId, int charges) {
		Map<Integer, Integer> chargeMap;
		try {
			chargeMap = Arrays.stream(config.chargesMap().split(",")).collect(Collectors.toMap(s -> Integer.valueOf(s.split(":")[0]), s -> Integer.valueOf(s.split(":")[1])));
		} catch (NumberFormatException | ClassCastException e) {
			System.out.println(e.getMessage());
			config.chargesMap("");
			chargeMap = new HashMap<>();
		}

		if (chargeMap == null) chargeMap = new HashMap<>();
		chargeMap.put(itemId, charges);
		config.chargesMap(chargeMap.entrySet().stream().map(entry -> entry.getKey() + ":" + entry.getValue()).collect(Collectors.joining(",")));
	}

	public void addWeaponCharges(int itemId, int charges) {
		Map<Integer, Integer> chargeMap;
		try {
			chargeMap = Arrays.stream(config.chargesMap().split(",")).collect(Collectors.toMap(s -> Integer.valueOf(s.split(":")[0]), s -> Integer.valueOf(s.split(":")[1])));
		} catch (NumberFormatException | ClassCastException e) {
			System.out.println(e.getMessage());
			config.chargesMap("");
			chargeMap = new HashMap<>();
		}

		charges = chargeMap.getOrDefault(itemId, 0) + charges;
		chargeMap.put(itemId, charges);
		System.out.println("storing " + chargeMap.entrySet().stream().map(entry -> entry.getKey() + ":" + entry.getValue()).collect(Collectors.joining(",")));
		config.chargesMap(chargeMap.entrySet().stream().map(entry -> entry.getKey() + ":" + entry.getValue()).collect(Collectors.joining(",")));
	}

	@RequiredArgsConstructor
	public static class SimpleWeapon {
		public final int itemId;
		public final List<Integer> animationIds;
		public final String name;
	}

	public static final List<SimpleWeapon> simpleWeapons = new ArrayList<>();
	static {
		simpleWeapons.add(new SimpleWeapon(ItemID.TRIDENT_OF_THE_SWAMP, Arrays.asList(SWAMP_TRIDENT_CAST), "trident of the swamp"));
		simpleWeapons.add(new SimpleWeapon(ItemID.TRIDENT_OF_THE_SEAS, Arrays.asList(SWAMP_TRIDENT_CAST), "trident of the seas"));
		/* "Your weapon has d,ddd charges." */
		simpleWeapons.add(new SimpleWeapon(ItemID.ABYSSAL_TENTACLE, Arrays.asList(1658), "abyssal tentacle"));
		simpleWeapons.add(new SimpleWeapon(ItemID.CRYSTAL_HALBERD, Arrays.asList(428, 440, 1203), "crystal halberd"));
		/* "Your abyssal tentacle can perform d,ddd more attacks." */
//		simpleWeapons.add(new SimpleWeapon(ItemID.IBANS_STAFF, MAGIC_IBANS_BLAST));
//		simpleWeapons.add(new SimpleWeapon(ItemID.IBANS_STAFF_U, MAGIC_IBANS_BLAST));
	}

	private static final int TICKS_RAPID_PVM = 2;
	private static final int TICKS_RAPID_PVP = 3;
	private static final int TICKS_NORMAL_PVM = 3;
	private static final int TICKS_NORMAL_PVP = 4;
	private static final int MAX_SCALES = 16383;

	private static int blowpipeHits = 0;
	private static int blowpipeHitsBySound = 0;

	@Subscribe
	public void onSoundEffectPlayed(SoundEffectPlayed soundEffectPlayed)
	{
//		new Exception("sound effect").printStackTrace(System.out);
		if (soundEffectPlayed.getSoundId() == 2696) {
			blowpipeHitsBySound++;
			System.out.println(client.getTickCount() + " blowpipe hits (by sound): " + blowpipeHits + " " + blowpipeHitsBySound);
		}
	}

	private int lastAnimationStart = 0;

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		Player player = client.getLocalPlayer();

		if (player.getAnimation() != AnimationID.BLOWPIPE_ATTACK_ANIMATION)
		{
			return;
		}

		if (ticks == 0) {
			lastAnimationStart = client.getTickCount();
		} else {
			if (client.getTickCount() - lastAnimationStart > ticksInAnimation) {
				ticks = 0;
				lastAnimationStart = client.getTickCount();
			}
		}

		ticks++;
		System.out.println(client.getTickCount() + " blowpipe: " + ticks + " " + ticksInAnimation);

		if (ticks == ticksInAnimation)
		{
			System.out.println(client.getTickCount() + " blowpipe hits (animation update): " + ++blowpipeHits + " " + blowpipeHitsBySound);
			AttractorDefinition attractorDefinition = getAttractorForPlayer();
			if (attractorDefinition == null)
			{
				return;
			}

			if (shouldConsumeDart(attractorDefinition))
			{
				dartsLeft--;
				config.dartsLeft(dartsLeft);
//				System.out.println("lost dart: " + dartsLeft);
			}

			if (shouldConsumeScales())
			{
				scalesLeft--;
				System.out.println("setting scales to " + scalesLeft);
				config.scalesLeft(scalesLeft);
//				System.out.println("lost scales: " + scalesLeft);
			}

			ticks = 0;
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		if ((attackStyleVarbit == -1 || attackStyleVarbit != client.getVar(VarPlayer.ATTACK_STYLE)) && client.getLocalPlayer() != null)
		{
			attackStyleVarbit = client.getVar(VarPlayer.ATTACK_STYLE);

			if (attackStyleVarbit == 0 || attackStyleVarbit == 3)
			{
				ticksInAnimation = client.getLocalPlayer().getInteracting() instanceof Player ? TICKS_NORMAL_PVP : TICKS_NORMAL_PVM;
			}
			else if (attackStyleVarbit == 1)
			{
				ticksInAnimation = client.getLocalPlayer().getInteracting() instanceof Player ? TICKS_RAPID_PVP : TICKS_RAPID_PVM;
			}
		}
	}

	private AttractorDefinition getAttractorForPlayer()
	{
		int attractorEquippedId = client.getLocalPlayer().getPlayerComposition().getEquipmentId(KitType.CAPE);

		return AttractorDefinition.getAttractorById(attractorEquippedId);
	}

	private boolean shouldConsumeDart(AttractorDefinition attractorDefinition)
	{
		double dartRoll = RANDOM.nextDouble();
		if (dartRoll <= attractorDefinition.getSavedChance())
		{
			return false;
		}
		else
		{
			return dartRoll > 1 - (attractorDefinition.getBreakOnImpactChance() + attractorDefinition.getDropToFloorChance());
		}
	}

	private boolean shouldConsumeScales()
	{
		return RANDOM.nextDouble() <= 0.66;
	}

	enum DartType {
		BRONZE,
		IRON,
		STEEL,
		MITHRIL,
		ADAMANT,
		RUNE,
		DRAGON;
	}

	private int getDartIdByName(String dartName)
	{
		switch (dartName.toLowerCase())
		{
			case "bronze":
				return ItemID.BRONZE_DART;
			case "iron":
				return ItemID.IRON_DART;
			case "steel":
				return ItemID.STEEL_DART;
			case "mithril":
				return ItemID.MITHRIL_DART;
			case "adamant":
				return ItemID.ADAMANT_DART;
			case "rune":
				return ItemID.RUNE_DART;
			case "dragon":
				return ItemID.DRAGON_DART;
			default:
				return -1;
		}
	}
}
