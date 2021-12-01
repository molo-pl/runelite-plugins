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

import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup(FriendsViewerConfig.CONFIG_GROUP)
public interface FriendsViewerConfig extends Config
{
	String CONFIG_GROUP = "friendListViewer";

	@ConfigItem(
		keyName = "maxFriends",
		name = "Max Players",
		description = "Maximum number of friends or clanmates to show",
		position = 1
	)
	default int maxPlayers()
	{
		return 10;
	}

	@ConfigSection(
		name = "Overlays",
		description = "Overlays",
		position = 2
	)
	String overlaysSection = "overlaysSection";

	@ConfigItem(
		keyName = "showFriends",
		name = "Show Friends",
		description = "Show or hide Friends overlay",
		position = 1,
		section = overlaysSection
	)
	default boolean showFriends()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showChatChannel",
		name = "Show Chat-channel",
		description = "Show or hide Chat-channel overlay",
		position = 2,
		section = overlaysSection
	)
	default boolean showChatChannel()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showYourClan",
		name = "Show Your Clan",
		description = "Show or hide Your Clan overlay",
		position = 3,
		section = overlaysSection
	)
	default boolean showYourClan()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showGuestClan",
		name = "Show Guest Clan",
		description = "Show or hide Guest Clan overlay",
		position = 4,
		section = overlaysSection
	)
	default boolean showGuestClan()
	{
		return false;
	}

	@ConfigItem(
		keyName = "fontSize",
		name = "Font Size",
		description = "Font size to use on the overlay",
		position = 5,
		section = overlaysSection
	)
	default FriendsViewerFontSize fontSize()
	{
		return FriendsViewerFontSize.REGULAR;
	}

	@ConfigSection(
		name = "Overlay Colors",
		description = "Overlay colors",
		position = 3
	)
	String colorSection = "colorSection";

	@ConfigItem(
		keyName = "sameWorldColor",
		name = "Same World Color",
		description = "The color for highlighting the same world as currently logged in to",
		position = 1,
		section = colorSection
	)
	default Color sameWorldColor()
	{
		return Color.GREEN;
	}

	@ConfigItem(
		keyName = "differentWorldColor",
		name = "Different World Color",
		description = "The color for different worlds than currently logged in to",
		position = 2,
		section = colorSection
	)
	default Color differentWorldColor()
	{
		return Color.YELLOW;
	}
}
