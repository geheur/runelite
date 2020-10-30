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

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import javax.inject.Inject;
import net.runelite.api.ItemID;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.WidgetItemOverlay;
import net.runelite.client.ui.overlay.components.TextComponent;

public class WeaponChargesOverlay extends WidgetItemOverlay
{
	@Inject
	private WeaponChargesPlugin plugin;
		@Inject
	private WeaponChargesConfig config;

	private final TextComponent textComponent = new TextComponent();

	public WeaponChargesOverlay()
	{
//		setPosition(OverlayPosition.DYNAMIC);
//		setLayer(OverlayLayer.ABOVE_WIDGETS);
//		this.plugin = plugin;
//		this.config = config;
	}

	@Override
	public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem itemWidget)
	{
		if (!config.showChargesOverlay())
		{
			return;
		}

		graphics.setFont(FontManager.getRunescapeSmallFont());

		if (plugin.isIbansStaffEquipped()) {
			ChargedWeapon chargedWeapon = plugin.getChargedWeaponFromId(ItemID.IBANS_STAFF);
			if (chargedWeapon != null)
			{
				final Integer charges = plugin.getCharges(chargedWeapon);
				if (charges != null)
				{
					final String chargesString = String.valueOf(charges);
					int chargesStringWidth = graphics.getFontMetrics().stringWidth(chargesString);

					final Rectangle bounds = itemWidget.getCanvasBounds();
					textComponent.setPosition(new Point(bounds.x + 16 - chargesStringWidth / 2, bounds.y + 24));
					textComponent.setText(chargesString);
					textComponent.setColor(plugin.getChargesColor(charges));
					textComponent.render(graphics);
				}
			}
		}
	}
}
