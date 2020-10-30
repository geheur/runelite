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

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

import java.util.Map;

@ConfigGroup("weaponCharges")
public interface WeaponChargesConfig extends Config
{
	@ConfigItem(
		keyName = "showChargesOverlay",
		name = "Show charges overlay",
		description = "Displays amount of charges on top of the item in your inventory"
	)
	default boolean showChargesOverlay()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showChargesInfoBox",
		name = "Show charges InfoBox",
		description = "Displays amount of charges in an InfoBox when equipped"
	)
	default boolean showChargesInfoBox()
	{
		return true;
	}

	@ConfigItem(
		keyName = "ibansCharges",
		name = "",
		description = "",
		hidden = true
	)
	default int ibansCharges()
	{
		return -1;
	}

	@ConfigItem(
		keyName = "ibansCharges",
		name = "",
		description = ""
	)
	void ibansCharges(int ibansCharges);

	@ConfigItem(
			keyName = "chargesMap",
			name = "",
			description = "",
			hidden = true
	)
	default String chargesMap()
	{
		return "";
	}

	@ConfigItem(
			keyName = "chargesMap",
			name = "",
			description = ""
	)
	void chargesMap(String map);

	@ConfigItem(
		keyName = "swampTridentCharges",
		name = "",
		description = "",
		hidden = true
	)
	default int swampTridentCharges()
	{
		return -1;
	}

	@ConfigItem(
		keyName = "swampTridentCharges",
		name = "",
		description = ""
	)
	void swampTridentCharges(int swampTridentCharges);

	@ConfigItem(
		keyName = "dartType",
		name = "",
		description = "",
		hidden = true
	)
	default int dartType()
	{
		return -1;
	}

	@ConfigItem(
		keyName = "dartType",
		name = "",
		description = ""
	)
	void dartType(int dartType);

	@ConfigItem(
		keyName = "dartsLeft",
		name = "",
		description = "",
		hidden = true
	)
	default int dartsLeft()
	{
		return -1;
	}

	@ConfigItem(
		keyName = "dartsLeft",
		name = "",
		description = ""
	)
	void dartsLeft(int dartsLeft);

	@ConfigItem(
		keyName = "scalesLeft",
		name = "",
		description = "",
		hidden = true
	)
	default int scalesLeft()
	{
		return -1;
	}

	@ConfigItem(
		keyName = "scalesLeft",
		name = "",
		description = ""
	)
	void scalesLeft(int scalesLeft);
}
