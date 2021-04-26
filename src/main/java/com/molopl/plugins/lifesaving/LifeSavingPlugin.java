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
package com.molopl.plugins.lifesaving;

import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import org.apache.commons.lang3.StringUtils;

@Slf4j
@PluginDescriptor(
	name = "Life-Saving Jewellery",
	description = "Infoboxes and notifications for Phoenix necklace and Ring of life",
	tags = {"phoenix", "necklace", "ring", "life"}
)
public class LifeSavingPlugin extends Plugin
{
	private static final String RING_OF_LIFE_USED_MSG = "Your Ring of Life saves you and is destroyed in the process.";
	private static final String PHOENIX_NECKLACE_USED_MSG = "Your phoenix necklace heals you, but is destroyed in the process.";

	@Inject
	private Client client;
	@Inject
	private LifeSavingConfig config;
	@Inject
	private InfoBoxManager infoBoxManager;
	@Inject
	private ItemManager itemManager;
	@Inject
	private Notifier notifier;
	@Inject
	private ClientThread clientThread;

	@Provides
	public LifeSavingConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(LifeSavingConfig.class);
	}

	@Override
	public void startUp()
	{
	}

	@Override
	public void shutDown()
	{
		infoBoxManager.removeIf(LifeSavingInfoBox.class::isInstance);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!event.getGroup().equals(LifeSavingConfig.GROUP))
		{
			return;
		}

		if (!config.ringOfLifeInfobox())
		{
			removeInfobox(LifeSavingItem.RING_OF_LIFE);
		}

		if (!config.phoenixNecklaceInfobox())
		{
			removeInfobox(LifeSavingItem.PHOENIX_NECKLACE);
		}

		clientThread.invokeLater(() ->
		{
			final ItemContainer itemContainer = client.getItemContainer(InventoryID.EQUIPMENT);
			if (itemContainer == null)
			{
				return;
			}

			final Item[] items = itemContainer.getItems();
			updateInfobox(config.ringOfLifeInfobox(), LifeSavingItem.RING_OF_LIFE, items);
			updateInfobox(config.phoenixNecklaceInfobox(), LifeSavingItem.PHOENIX_NECKLACE, items);
		});
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event.getType() != ChatMessageType.GAMEMESSAGE)
		{
			return;
		}

		final String message = event.getMessage();
		checkNotification(config.ringOfLifeNotification(), LifeSavingItem.RING_OF_LIFE, message, RING_OF_LIFE_USED_MSG);
		checkNotification(config.phoenixNecklaceNotification(), LifeSavingItem.PHOENIX_NECKLACE, message, PHOENIX_NECKLACE_USED_MSG);
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event.getItemContainer() != client.getItemContainer(InventoryID.EQUIPMENT))
		{
			return;
		}

		final Item[] items = event.getItemContainer().getItems();
		updateInfobox(config.ringOfLifeInfobox(), LifeSavingItem.RING_OF_LIFE, items);
		updateInfobox(config.phoenixNecklaceInfobox(), LifeSavingItem.PHOENIX_NECKLACE, items);
	}

	private void updateInfobox(boolean enabled, LifeSavingItem type, Item[] wornItems)
	{
		if (!enabled)
		{
			return;
		}

		removeInfobox(type);

		final int itemId = type.getItemId();
		final int slotIdx = type.getSlot().getSlotIdx();

		if (wornItems.length <= slotIdx)
		{
			return;
		}

		final Item wornItem = wornItems[slotIdx];
		if (wornItem.getId() != itemId)
		{
			return;
		}

		final String name = itemManager.getItemComposition(itemId).getName();
		final BufferedImage image = itemManager.getImage(itemId);
		final LifeSavingInfoBox infobox = new LifeSavingInfoBox(this, image, type, name);
		infoBoxManager.addInfoBox(infobox);
	}

	private void removeInfobox(final LifeSavingItem type)
	{
		infoBoxManager.removeIf(infoBox -> infoBox instanceof LifeSavingInfoBox && ((LifeSavingInfoBox) infoBox).getType() == type);
	}

	private void checkNotification(boolean enabled, LifeSavingItem type, String actualMessage, String expectedMessage)
	{
		if (enabled && StringUtils.contains(actualMessage, expectedMessage))
		{
			notifier.notify(String.format("Your %s is destroyed!", itemManager.getItemComposition(type.getItemId()).getName()));
		}
	}
}
