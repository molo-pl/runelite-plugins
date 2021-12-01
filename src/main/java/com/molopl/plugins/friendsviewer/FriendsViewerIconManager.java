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
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.EnumComposition;
import net.runelite.api.EnumID;
import net.runelite.api.FriendsChatRank;
import net.runelite.api.clan.ClanTitle;
import net.runelite.client.game.ChatIconManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.util.ImageUtil;

/**
 * Based on {@link ChatIconManager}, with modifications specific for the plugin.
 */
public class FriendsViewerIconManager
{
	public static final Dimension IMAGE_DIMENSION = new Dimension(14, 14);
	private static final Color IMAGE_OUTLINE_COLOR = new Color(33, 33, 33);
	private static final int IMAGE_TOP_MARGIN = 2;

	private final ChatIconManager chatIconManager;
	private final Client client;
	private final SpriteManager spriteManager;

	private BufferedImage[] friendsChatRankImages;
	private BufferedImage[] clanRankImages;

	@Inject
	private FriendsViewerIconManager(ChatIconManager chatIconManager, Client client, SpriteManager spriteManager)
	{
		this.chatIconManager = chatIconManager;
		this.client = client;
		this.spriteManager = spriteManager;
	}

	@Nullable
	public BufferedImage getRankImage(FriendsViewerFontSize fontSize, FriendsChatRank friendsChatRank)
	{
		switch (fontSize)
		{
			case REGULAR:
			{
				return friendsChatRankImages != null && friendsChatRank != FriendsChatRank.UNRANKED
					? friendsChatRankImages[friendsChatRank.ordinal() - 1]
					: null;
			}
			case SMALL:
			{
				return chatIconManager.getRankImage(friendsChatRank);
			}
			default:
			{
				throw new UnsupportedOperationException("Unknown font size: " + fontSize);
			}
		}
	}

	@Nullable
	public BufferedImage getRankImage(FriendsViewerFontSize fontSize, ClanTitle clanTitle)
	{
		switch (fontSize)
		{
			case REGULAR:
			{
				if (clanRankImages == null)
				{
					return null;
				}

				int rank = clanTitle.getId();
				int idx = clanRankToIdx(rank);
				return clanRankImages[idx];
			}
			case SMALL:
			{
				return chatIconManager.getRankImage(clanTitle);
			}
			default:
			{
				throw new UnsupportedOperationException("Unknown font size: " + fontSize);
			}
		}
	}

	public void loadRankIcons()
	{
		if (friendsChatRankImages != null)
		{
			return;
		}

		final EnumComposition friendsChatIcons = client.getEnum(EnumID.FRIENDS_CHAT_RANK_ICONS);
		final EnumComposition clanIcons = client.getEnum(EnumID.CLAN_RANK_GRAPHIC);

		friendsChatRankImages = new BufferedImage[friendsChatIcons.size()];
		clanRankImages = new BufferedImage[clanIcons.size()];

		for (int i = 0; i < friendsChatIcons.size(); i++)
		{
			final int idx = i;
			spriteManager.getSpriteAsync(friendsChatIcons.getIntValue(friendsChatIcons.getKeys()[i]), 0,
				sprite -> friendsChatRankImages[idx] = prepareImage(sprite, true));
		}

		for (int i = 0; i < clanIcons.size(); i++)
		{
			final int key = clanIcons.getKeys()[i];
			final int idx = clanRankToIdx(key);
			spriteManager.getSpriteAsync(clanIcons.getIntValue(key), 0,
				sprite -> clanRankImages[idx] = prepareImage(sprite, false));
		}
	}

	private static BufferedImage prepareImage(BufferedImage sprite, boolean outline)
	{
		return Optional.ofNullable(sprite)
			.map(image -> ImageUtil.resizeCanvas(image, IMAGE_DIMENSION.width, IMAGE_DIMENSION.height))
			.map(image -> outline ? ImageUtil.outlineImage(image, IMAGE_OUTLINE_COLOR) : image)
			.map(FriendsViewerIconManager::addMargin)
			.orElse(null);
	}

	private static BufferedImage addMargin(BufferedImage sprite)
	{
		final BufferedImage newImage = new BufferedImage(sprite.getWidth(), sprite.getHeight() + IMAGE_TOP_MARGIN, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D graphics = newImage.createGraphics();
		graphics.drawImage(sprite, 0, IMAGE_TOP_MARGIN, null);
		graphics.dispose();
		return newImage;
	}

	private static int clanRankToIdx(int key)
	{
		// keys are -6 to 265, with no 0
		return key < 0 ? ~key : (key + 5);
	}
}
