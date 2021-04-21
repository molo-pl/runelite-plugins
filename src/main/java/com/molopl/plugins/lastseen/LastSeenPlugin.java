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
package com.molopl.plugins.lastseen;

import com.google.common.base.Strings;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import javax.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Friend;
import net.runelite.api.GameState;
import net.runelite.api.Nameable;
import net.runelite.api.NameableContainer;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.NameableNameChanged;
import net.runelite.api.events.RemovedFriend;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;
import org.apache.commons.lang3.ObjectUtils;

@Slf4j
@PluginDescriptor(
	name = "Last Seen",
	description = "Check when you've last seen your friends online",
	tags = {"last", "seen", "online"}
)
public class LastSeenPlugin extends Plugin
{
	@Inject
	private Client client;
	@Inject
	private ClientThread clientThread;
	@Inject
	private OverlayManager overlayManager;

	@Inject
	private LastSeenOverlay overlay;
	@Inject
	private LastSeenDao dao;

	/**
	 * Holds information about what to display on hover for the friends list.
	 */
	@Getter
	private LastSeenHoverInfo hoverInfo;

	/**
	 * In-memory mapping of user display names to last seen timestamp for current game session.
	 */
	private final Map<String, Long> lastSeenThisSession = new HashMap<>();

	/**
	 * Indicator of the last game tick in which data has been persisted.
	 */
	private int lastPersistedTick;

	@Override
	protected void startUp()
	{
		overlayManager.add(overlay);
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
	}

	private String getLastSeenText(@Nullable Long lastSeenMillis)
	{
		if (lastSeenMillis == null)
		{
			return "never";
		}

		final long diffMillis = System.currentTimeMillis() - lastSeenMillis;
		return ObjectUtils.firstNonNull(
			getLastSeenTextIfInUnit(diffMillis, ChronoUnit.DAYS, "day", "days"),
			getLastSeenTextIfInUnit(diffMillis, ChronoUnit.HOURS, "hour", "hours"),
			getLastSeenTextIfInUnit(diffMillis, ChronoUnit.MINUTES, "minute", "minutes"),
			"just now"
		);
	}

	private String getLastSeenTextIfInUnit(long diffMillis, TemporalUnit temporalUnit, String singular, String plural)
	{
		final long durationInUnit = diffMillis / temporalUnit.getDuration().toMillis();
		return durationInUnit > 0
			? String.format("%d %s ago", durationInUnit, durationInUnit == 1 ? singular : plural)
			: null;
	}

	private void setHoverInfo(String displayName)
	{
		hoverInfo = null;
		if (!Strings.isNullOrEmpty(displayName))
		{
			final Long lastSeen = lastSeenThisSession.getOrDefault(displayName, dao.getLastSeen(displayName));
			hoverInfo = new LastSeenHoverInfo(displayName, "Last online: " + getLastSeenText(lastSeen));
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		persistState();
	}

	@Subscribe
	public void onNameableNameChanged(NameableNameChanged event)
	{
		final Nameable nameable = event.getNameable();
		if (nameable instanceof Friend)
		{
			if (nameable.getPrevName() != null)
			{
				dao.migrateLastSeen(
					Text.toJagexName(nameable.getPrevName()),
					Text.toJagexName(nameable.getName())
				);
			}
		}
	}

	@Subscribe
	public void onRemovedFriend(RemovedFriend event)
	{
		dao.deleteLastSeen(Text.toJagexName(event.getNameable().getName()));
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		final int groupId = WidgetInfo.TO_GROUP(event.getActionParam1());

		if (groupId == WidgetInfo.FRIENDS_LIST.getGroupId() && event.getOption().equals("Message"))
		{
			setHoverInfo(Text.toJagexName(Text.removeTags(event.getTarget())));
		}
		else
		{
			hoverInfo = null;
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		final int tickCount = client.getTickCount();
		if (tickCount >= lastPersistedTick + 100)
		{
			persistState();
		}

		clientThread.invokeLater(() ->
		{
			final NameableContainer<Friend> friendContainer = client.getFriendContainer();
			if (friendContainer == null)
			{
				return;
			}

			final long currentTimeMillis = System.currentTimeMillis();
			Arrays.stream(friendContainer.getMembers())
				.filter(friend -> friend.getWorld() > 0)
				.map(Friend::getName)
				.map(Text::toJagexName)
				.forEach(displayName -> lastSeenThisSession.put(displayName, currentTimeMillis));
		});
	}

	private void persistState()
	{
		lastPersistedTick = client.getTickCount();
		lastSeenThisSession.forEach(dao::setLastSeen);
		lastSeenThisSession.clear();
	}
}
