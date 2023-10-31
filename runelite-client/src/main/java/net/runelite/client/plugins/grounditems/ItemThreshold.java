/*
 * Copyright (c) 2020, dekvall <https://github.com/dekvall>
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
package net.runelite.client.plugins.grounditems;

import com.google.common.base.Strings;
import lombok.RequiredArgsConstructor;
import lombok.Value;

@Value
@RequiredArgsConstructor
class ItemThreshold
{
	enum Inequality
	{
		LESS_THAN,
		MORE_THAN
	}

	String itemName;
	int quantity;
	Inequality inequality;
	Boolean noted;

	public ItemThreshold(String itemName, int quantity, Inequality inequality) {
		this(itemName, quantity, inequality, null);
	}

	static ItemThreshold fromConfigEntry(String entry)
	{
		if (Strings.isNullOrEmpty(entry))
		{
			return null;
		}

		Inequality operator = Inequality.MORE_THAN;
		int qty = 0;

		Boolean noted = null;
		int colonIndex = entry.indexOf(':');
		if (colonIndex != -1) {
			String notedString = entry.substring(0, colonIndex);
			if (notedString.equals("noted")) noted = true;
			else if (notedString.equals("unnoted")) noted = false;
			entry = entry.substring(colonIndex + 1);
		}

		for (int i = entry.length() - 1; i >= 0; i--)
		{
			char c = entry.charAt(i);
			if (c >= '0' && c <= '9' || Character.isWhitespace(c))
			{
				continue;
			}
			switch (c)
			{
				case '<':
					operator = Inequality.LESS_THAN;
					// fallthrough
				case '>':
					if (i + 1 < entry.length())
					{
						try
						{
							qty = Integer.parseInt(entry.substring(i + 1).trim());
						}
						catch (NumberFormatException e)
						{
							qty = 0;
							operator = Inequality.MORE_THAN;
						}
						entry = entry.substring(0, i);
					}
			}
			break;
		}

		return new ItemThreshold(entry.trim(), qty, operator, noted);
	}

	boolean quantityHolds(int itemCount)
	{
		if (inequality == Inequality.LESS_THAN)
		{
			return itemCount < quantity;
		}
		else
		{
			return itemCount > quantity;
		}
	}

	public static boolean matchesEntry(String entry, String itemName) {
		int colonIndex = entry.indexOf(':');
		if (colonIndex != -1) entry = entry.substring(colonIndex + 1);
		int index = entry.indexOf('<');
		if (index == -1)
		{
			index = entry.indexOf('>');
		}
		if (index != -1)
		{
			entry = entry.substring(0, index);
		}
		return itemName.equalsIgnoreCase(entry);
	}
}
