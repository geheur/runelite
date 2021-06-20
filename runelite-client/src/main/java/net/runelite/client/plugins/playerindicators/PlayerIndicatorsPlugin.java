/*
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
package net.runelite.client.plugins.playerindicators;

import com.google.inject.Provides;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.Value;
import net.runelite.api.Client;
import net.runelite.api.FriendsChatRank;
import static net.runelite.api.FriendsChatRank.UNRANKED;
import static net.runelite.api.MenuAction.ITEM_USE_ON_PLAYER;
import static net.runelite.api.MenuAction.MENU_ACTION_DEPRIORITIZE_OFFSET;
import static net.runelite.api.MenuAction.PLAYER_EIGTH_OPTION;
import static net.runelite.api.MenuAction.PLAYER_FIFTH_OPTION;
import static net.runelite.api.MenuAction.PLAYER_FIRST_OPTION;
import static net.runelite.api.MenuAction.PLAYER_FOURTH_OPTION;
import static net.runelite.api.MenuAction.PLAYER_SECOND_OPTION;
import static net.runelite.api.MenuAction.PLAYER_SEVENTH_OPTION;
import static net.runelite.api.MenuAction.PLAYER_SIXTH_OPTION;
import static net.runelite.api.MenuAction.PLAYER_THIRD_OPTION;
import static net.runelite.api.MenuAction.RUNELITE_PLAYER;
import static net.runelite.api.MenuAction.SPELL_CAST_ON_PLAYER;
import static net.runelite.api.MenuAction.WALK;
import net.runelite.api.MenuEntry;
import net.runelite.api.Player;
import net.runelite.api.VarClientStr;
import net.runelite.api.Varbits;
import net.runelite.api.clan.ClanTitle;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ChatIconManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ColorUtil;

@PluginDescriptor(
	name = "Player Indicators",
	description = "Highlight players on-screen and/or on the minimap",
	tags = {"highlight", "minimap", "overlay", "players"}
)
public class PlayerIndicatorsPlugin extends Plugin
{
	@Inject
	private OverlayManager overlayManager;

	@Inject
	private PlayerIndicatorsConfig config;

	@Inject
	private PlayerIndicatorsOverlay playerIndicatorsOverlay;

	@Inject
	private PlayerIndicatorsTileOverlay playerIndicatorsTileOverlay;

	@Inject
	private PlayerIndicatorsMinimapOverlay playerIndicatorsMinimapOverlay;

	@Inject
	private PlayerIndicatorsService playerIndicatorsService;

	@Inject
	private Client client;

	@Inject
	private ChatIconManager chatIconManager;

	private List<String> tobTeamMembers = Collections.emptyList();

	@Provides
	PlayerIndicatorsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(PlayerIndicatorsConfig.class);
	}

	@Override
	protected void startUp() throws Exception
	{
		overlayManager.add(playerIndicatorsOverlay);
		overlayManager.add(playerIndicatorsTileOverlay);
		overlayManager.add(playerIndicatorsMinimapOverlay);
	}

	@Override
	protected void shutDown() throws Exception
	{
		overlayManager.remove(playerIndicatorsOverlay);
		overlayManager.remove(playerIndicatorsTileOverlay);
		overlayManager.remove(playerIndicatorsMinimapOverlay);
	}

	@Subscribe
	public void onGameTick(GameTick gameTick) {
		tobTeamMembers = getTobTeamMembers();
		playerIndicatorsService.setTobTeamMembers(tobTeamMembers);
	}

	private List<String> getTobTeamMembers()
	{
		int tobVar = client.getVar(Varbits.THEATRE_OF_BLOOD);
		if (tobVar == 1) { // in party, outside of tob.
			Widget widget = client.getWidget(WidgetInfo.TOB_PARTY_MEMBERS);
			if (widget != null)
			{
				return Arrays.stream(widget.getText().split("<br>"))
					.filter(name -> !name.equals("-"))
					.collect(Collectors.toList());
			}
		} else if (tobVar > 1) { // inside tob.
			// These vars contain garbage or misleading values when not inside a ToB instance, so only read them when
			// you're in tob.
			List<String> team = new ArrayList<>();
			team.add(client.getVar(VarClientStr.TOB_TEAM_MEMBER_NAME_1));
			team.add(client.getVar(VarClientStr.TOB_TEAM_MEMBER_NAME_2));
			team.add(client.getVar(VarClientStr.TOB_TEAM_MEMBER_NAME_3));
			team.add(client.getVar(VarClientStr.TOB_TEAM_MEMBER_NAME_4));
			team.add(client.getVar(VarClientStr.TOB_TEAM_MEMBER_NAME_5));
			team = team.stream().filter(name -> !name.isEmpty()).collect(Collectors.toList());
			return team;
		}
		return Collections.emptyList();
	}

	@Subscribe
	public void onClientTick(ClientTick clientTick)
	{
		if (client.isMenuOpen())
		{
			return;
		}

		MenuEntry[] menuEntries = client.getMenuEntries();
		boolean modified = false;

		for (MenuEntry entry : menuEntries)
		{
			int type = entry.getType();

			if (type >= MENU_ACTION_DEPRIORITIZE_OFFSET)
			{
				type -= MENU_ACTION_DEPRIORITIZE_OFFSET;
			}

			if (type == WALK.getId()
				|| type == SPELL_CAST_ON_PLAYER.getId()
				|| type == ITEM_USE_ON_PLAYER.getId()
				|| type == PLAYER_FIRST_OPTION.getId()
				|| type == PLAYER_SECOND_OPTION.getId()
				|| type == PLAYER_THIRD_OPTION.getId()
				|| type == PLAYER_FOURTH_OPTION.getId()
				|| type == PLAYER_FIFTH_OPTION.getId()
				|| type == PLAYER_SIXTH_OPTION.getId()
				|| type == PLAYER_SEVENTH_OPTION.getId()
				|| type == PLAYER_EIGTH_OPTION.getId()
				|| type == RUNELITE_PLAYER.getId())
			{
				Player[] players = client.getCachedPlayers();
				Player player = null;

				int identifier = entry.getIdentifier();

				// 'Walk here' identifiers are offset by 1 because the default
				// identifier for this option is 0, which is also a player index.
				if (type == WALK.getId())
				{
					identifier--;
				}

				if (identifier >= 0 && identifier < players.length)
				{
					player = players[identifier];
				}

				if (player == null)
				{
					continue;
				}

				Decorations decorations = getDecorations(player);

				if (decorations == null)
				{
					continue;
				}

				String oldTarget = entry.getTarget();
				String newTarget = decorateTarget(oldTarget, decorations);

				entry.setTarget(newTarget);
				modified = true;
			}
		}

		if (modified)
		{
			client.setMenuEntries(menuEntries);
		}
	}

	private Decorations getDecorations(Player player)
	{
		int image = -1;
		Color color = null;

		if (player.isFriend() && config.highlightFriends())
		{
			color = config.getFriendColor();
		}
		else if (player.isFriendsChatMember() && config.highlightFriendsChat())
		{
			color = config.getFriendsChatMemberColor();

			if (config.showFriendsChatRanks())
			{
				FriendsChatRank rank = playerIndicatorsService.getFriendsChatRank(player);
				if (rank != UNRANKED)
				{
					image = chatIconManager.getIconNumber(rank);
				}
			}
		}
		else if (
			config.highlightTeamMembers()
			&& (
				tobTeamMembers.contains(player.getName())
				|| (player.getTeam() > 0 && client.getLocalPlayer().getTeam() == player.getTeam())
			))
		{
			color = config.getTeamMemberColor();
		}
		else if (player.isClanMember() && config.highlightClanMembers())
		{
			color = config.getClanMemberColor();

			if (config.showClanChatRanks())
			{
				ClanTitle clanTitle = playerIndicatorsService.getClanTitle(player);
				if (clanTitle != null)
				{
					image = chatIconManager.getIconNumber(clanTitle);
				}
			}
		}
		else if (!player.isFriendsChatMember() && !player.isClanMember() && config.highlightOthers())
		{
			color = config.getOthersColor();
		}

		if (image == -1 && color == null)
		{
			return null;
		}

		return new Decorations(image, color);
	}

	private String decorateTarget(String oldTarget, Decorations decorations)
	{
		String newTarget = oldTarget;

		if (decorations.getColor() != null && config.colorPlayerMenu())
		{
			// strip out existing <col...
			int idx = oldTarget.indexOf('>');
			if (idx != -1)
			{
				newTarget = oldTarget.substring(idx + 1);
			}

			newTarget = ColorUtil.prependColorTag(newTarget, decorations.getColor());
		}

		if (decorations.getImage() != -1)
		{
			newTarget = "<img=" + decorations.getImage() + ">" + newTarget;
		}

		return newTarget;
	}

	@Value
	private static class Decorations
	{
		private final int image;
		private final Color color;
	}
}
