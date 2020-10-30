package net.runelite.client.plugins.weaponcharges;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.ItemID;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.WidgetItemOverlay;
import net.runelite.client.ui.overlay.components.TextComponent;

public class WeaponChargesItemOverlay extends WidgetItemOverlay
{
	private final WeaponChargesPlugin plugin;
	private final WeaponChargesConfig config;

	@Inject
	WeaponChargesItemOverlay(WeaponChargesPlugin plugin, WeaponChargesConfig config)
	{
		this.plugin = plugin;
		this.config = config;
		showOnInventory();
		showOnEquipment();
		showOnBank();
		showOnInterfaces(WidgetID.CHAMBERS_OF_XERIC_STORAGE_UNIT_INVENTORY_GROUP_ID);
		showOnInterfaces(WidgetID.CHAMBERS_OF_XERIC_STORAGE_UNIT_PRIVATE_GROUP_ID);
	}

	private static final Map<Integer, Color> dartColors = new HashMap<Integer, Color>() {
		{
			put(ItemID.BRONZE_DART, new Color(0x6e5727)); // 6e5527
			put(ItemID.IRON_DART, new Color(0x52504c));
			put(ItemID.STEEL_DART, new Color(0x7a7873));
			put(ItemID.MITHRIL_DART, new Color(0x414f78));
			put(ItemID.ADAMANT_DART, new Color(0x417852));
			put(ItemID.RUNE_DART, new Color(0x67e0f5));
			put(ItemID.DRAGON_DART, new Color(0x3e7877));
		}
	};

	@Override
	public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem itemWidget)
	{
		graphics.setFont(FontManager.getRunescapeSmallFont());

		Rectangle bounds = itemWidget.getCanvasBounds();
		TextComponent topText = new TextComponent();
		topText.setPosition(new java.awt.Point(bounds.x - 1, bounds.y + 15));
		topText.setText("");
		TextComponent bottomText = new TextComponent();
		bottomText.setPosition(new java.awt.Point(bounds.x - 1, bounds.y + 30));
		bottomText.setText("");

		boolean render = true;
		for (WeaponChargesPlugin.SimpleWeapon simpleWeapon : plugin.simpleWeapons) {
			if (itemId == simpleWeapon.itemId) {
				Integer weaponCharges = plugin.getWeaponCharges(simpleWeapon.itemId);
				if (weaponCharges != null) {
					topText.setText(String.valueOf(weaponCharges));
					render = true;
				}
			}
		}

		if (itemId == ItemID.TOXIC_BLOWPIPE) {
			int dartsLeft = config.dartsLeft();
			String dartsString = dartsLeft > 9999 ? new DecimalFormat("#0").format(dartsLeft / 1000.0) + "k" : dartsLeft < 1000 ? String.valueOf(dartsLeft) :
				new DecimalFormat("#0.0").format(dartsLeft / 1000.0) + "k";
			bottomText.setText(dartsString);
			bottomText.setColor(dartColors.get(config.dartType()));
			int stringLength = graphics.getFontMetrics().stringWidth(dartsString);
			bottomText.setPosition(new java.awt.Point(bounds.x - 1 + 30 - stringLength, bounds.y + 30));

			float scalesLeftPercent = (float) config.scalesLeft() / 16383 * 100;
			topText.setText(String.format("%d%%", (int) scalesLeftPercent));
			topText.setColor(getColorForScalesLeft());
		}

		if (render) {
			topText.render(graphics);
			bottomText.render(graphics);
		}
	}

	private Color getColorForScalesLeft()
	{
		float percent = (float) config.scalesLeft() / 16383;

		return Color.getHSBColor((float) (percent * 0.334), 1F, 1F);
	}
}
