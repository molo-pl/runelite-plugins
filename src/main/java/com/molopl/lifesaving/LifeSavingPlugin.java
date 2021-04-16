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
package com.molopl.lifesaving;

import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.itemcharges.ItemChargeConfig;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;

@Slf4j
@PluginDescriptor(
	name = "Life-Saving Jewellery",
	description = "Adds infoboxes and notifications for Phoenix necklace and Ring of life",
	tags = {"phoenix", "necklace", "ring", "life"}
)
public class LifeSavingPlugin extends Plugin
{
	@Inject
	private Client client;
	@Inject
	private LifeSavingConfig config;
	@Inject
	private InfoBoxManager infoBoxManager;
	@Inject
	private ItemManager itemManager;

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
		if (!event.getGroup().equals(ItemChargeConfig.GROUP))
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
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		// TODO: implement me!
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event.getItemContainer() != client.getItemContainer(InventoryID.EQUIPMENT))
		{
			return;
		}

		final Item[] items = event.getItemContainer().getItems();

		if (config.ringOfLifeInfobox())
		{
			updateInfobox(LifeSavingItem.RING_OF_LIFE, items);
		}
		if (config.phoenixNecklaceInfobox())
		{
			updateInfobox(LifeSavingItem.PHOENIX_NECKLACE, items);
		}
	}

	private void updateInfobox(LifeSavingItem type, Item[] wornItems)
	{
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
}
