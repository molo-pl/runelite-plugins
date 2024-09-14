/*
 * Copyright (c) 2021, molo-pl <https://github.com/molo-pl>
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
package com.molopl.plugins.friendsviewer;

import com.google.inject.Provides;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Friend;
import net.runelite.api.FriendsChatManager;
import net.runelite.api.FriendsChatMember;
import net.runelite.api.GameState;
import net.runelite.api.NameableContainer;
import net.runelite.api.Player;
import net.runelite.api.clan.ClanChannel;
import net.runelite.api.clan.ClanChannelMember;
import net.runelite.api.clan.ClanRank;
import net.runelite.api.clan.ClanSettings;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;

@Slf4j
@PluginDescriptor(
	name = "Friends and Clan Viewer",
	description = "Always see clanmates and friends when they are online",
	tags = {"friends", "list", "viewer", "online", "clan", "cc"}
)
public class FriendsViewerPlugin extends Plugin
{
	@Inject
	private Client client;
	@Inject
	private OverlayManager overlayManager;
	@Inject
	private ClientThread clientThread;

	@Inject
	private FriendsViewerIconManager iconManager;
	@Inject
	private FriendsViewerConfig config;

	private FriendsViewerOverlay friendsOverlay;
	private FriendsViewerOverlay chatChannelOverlay;
	private FriendsViewerOverlay yourClanOverlay;
	private FriendsViewerOverlay guestClanOverlay;

	@Override
	protected void startUp()
	{
		clientThread.invokeLater(() ->
		{
			if (client.getGameState().getState() > GameState.STARTING.getState())
			{
				iconManager.loadRankIcons();
			}
		});

		friendsOverlay = new FriendsViewerOverlay(client, config, "Friends", config::showFriends);
		chatChannelOverlay = new FriendsViewerOverlay(client, config, "Chat-channel", config::showChatChannel);
		yourClanOverlay = new FriendsViewerOverlay(client, config, "Your Clan", config::showYourClan);
		guestClanOverlay = new FriendsViewerOverlay(client, config, "Guest Clan", config::showGuestClan);

		overlayManager.add(friendsOverlay);
		overlayManager.add(chatChannelOverlay);
		overlayManager.add(yourClanOverlay);
		overlayManager.add(guestClanOverlay);
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(friendsOverlay);
		overlayManager.remove(chatChannelOverlay);
		overlayManager.remove(yourClanOverlay);
		overlayManager.remove(guestClanOverlay);
	}

	@Provides
	public FriendsViewerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(FriendsViewerConfig.class);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() != GameState.LOGIN_SCREEN)
		{
			return;
		}

		iconManager.loadRankIcons();

		friendsOverlay.setEntries(null);
		chatChannelOverlay.setEntries(null);
		yourClanOverlay.setEntries(null);
		guestClanOverlay.setEntries(null);
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (client.getGameState() != GameState.LOGGED_IN || client.getTickCount() % 5 != 0)
		{
			return;
		}

		updateFriends(config.showFriends());
		updateChatChannel(config.showChatChannel());
		updateClan(config.showYourClan(), yourClanOverlay, client.getClanChannel(), client.getClanSettings());
		updateClan(config.showGuestClan(), guestClanOverlay, client.getGuestClanChannel(), client.getGuestClanSettings());
	}

	private void updateFriends(boolean enabled)
	{
		final NameableContainer<Friend> friendContainer = client.getFriendContainer();
		if (!enabled || friendContainer == null)
		{
			friendsOverlay.setEntries(null);
			return;
		}

		friendsOverlay.setEntries(Arrays.stream(friendContainer.getMembers())
			.filter(friend -> friend.getWorld() > 0)
			.sorted(Comparator.comparing(Friend::getName, String::compareToIgnoreCase))
			.map(friend -> new FriendsViewerEntry(Text.toJagexName(friend.getName()), friend.getWorld(), null))
			.collect(Collectors.toList()));
	}

	private void updateChatChannel(boolean enabled)
	{
		final FriendsChatManager friendsChatManager = client.getFriendsChatManager();
		if (!enabled || friendsChatManager == null)
		{
			chatChannelOverlay.setEntries(null);
			return;
		}

		chatChannelOverlay.setEntries(Arrays.stream(friendsChatManager.getMembers())
			.filter(clanmate -> !Text.toJagexName(clanmate.getName()).equals(getLocalPlayerName()))
			.sorted(Comparator.comparing(FriendsChatMember::getRank).reversed()
				.thenComparing(FriendsChatMember::getName, String::compareToIgnoreCase))
			.map(clanmate -> new FriendsViewerEntry(
				Text.toJagexName(clanmate.getName()),
				clanmate.getWorld(),
				iconManager.getRankImage(config.fontSize(), clanmate.getRank())))
			.collect(Collectors.toList()));
	}

	private void updateClan(boolean enabled, FriendsViewerOverlay overlay, ClanChannel clanChannel, ClanSettings clanSettings)
	{
		if (!enabled || clanChannel == null || clanSettings == null)
		{
			overlay.setEntries(null);
			return;
		}

		overlay.setEntries(clanChannel.getMembers().stream()
			.filter(clanmate -> !Text.toJagexName(clanmate.getName()).equals(getLocalPlayerName()))
			.sorted(Comparator.comparing(ClanChannelMember::getRank, Comparator.comparing(ClanRank::getRank)).reversed()
				.thenComparing(ClanChannelMember::getName, String::compareToIgnoreCase))
			.map(clanmate -> new FriendsViewerEntry(
				Text.toJagexName(clanmate.getName()),
				clanmate.getWorld(),
				Optional.ofNullable(clanSettings.titleForRank(clanmate.getRank()))
					.map(title -> iconManager.getRankImage(config.fontSize(), title))
					.orElse(null)))
			.collect(Collectors.toList()));
	}

	private String getLocalPlayerName()
	{
		return Optional.ofNullable(client.getLocalPlayer())
			.map(Player::getName)
			.map(Text::toJagexName)
			.orElse(null);
	}
}
