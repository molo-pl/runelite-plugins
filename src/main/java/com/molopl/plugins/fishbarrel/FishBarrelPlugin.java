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
package com.molopl.plugins.fishbarrel;

import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.ItemID;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.widgets.Widget;
import static net.runelite.api.widgets.WidgetInfo.TO_CHILD;
import static net.runelite.api.widgets.WidgetInfo.TO_GROUP;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@Slf4j
@PluginDescriptor(
	name = "Fish Barrel",
	description = "Shows how many fish are in the fish barrel",
	tags = {"fish", "barrel", "tempoross"}
)
public class FishBarrelPlugin extends Plugin
{
	private static final Pattern FISH_CAUGHT_MESSAGE = Pattern.compile("^You catch a ([a-z]+)[.!]?$");
	private static final Pattern DOUBLE_CATCH_MESSAGE = Pattern.compile("^(The spirit flakes|Rada's blessing) enabled you to catch an extra fish.$");

	/**
	 * Maps the name of the fish as it appears in chat message to corresponding item ID.
	 */
	private static final Map<String, Integer> FISH_TYPES = ImmutableMap.<String, Integer>builder()
		.put("tuna", ItemID.RAW_TUNA)
		.put("swordfish", ItemID.RAW_SWORDFISH)
		.put("shark", ItemID.RAW_SHARK)
		.put("lobster", ItemID.RAW_LOBSTER)
		.put("mackerel", ItemID.RAW_MACKEREL)
		.put("cod", ItemID.RAW_COD)
		.build();

	@Inject
	private Client client;
	@Inject
	private OverlayManager overlayManager;
	@Inject
	private FishBarrelOverlay fishBarrelOverlay;
	@Inject
	private ClientThread clientThread;

	/**
	 * Number of 'fish caught' chat messages since last barrel's state update.
	 */
	private final AtomicInteger fishCaughtMessages = new AtomicInteger();
	/**
	 * Number of new fish in inventory since last barrel's state update.
	 */
	private final AtomicInteger newFishInInventory = new AtomicInteger();

	// too keep track of user's inventory
	private Set<Integer> inventoryItems = new HashSet<>();
	private Set<Integer> equipmentItems = new HashSet<>();

	@Override
	public void startUp()
	{
		overlayManager.add(fishBarrelOverlay);

		// initialize barrel
		FishBarrel.INSTANCE.setHolding(0);
		FishBarrel.INSTANCE.setUnknown(true);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			// initialize inventory
			updateInventory(InventoryID.INVENTORY, items -> inventoryItems = items);
			updateInventory(InventoryID.EQUIPMENT, items -> equipmentItems = items);
		}
	}

	@Override
	public void shutDown()
	{
		overlayManager.remove(fishBarrelOverlay);
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event.getType() != ChatMessageType.SPAM || !hasOpenBarrel())
		{
			return;
		}

		final Matcher matcher = FISH_CAUGHT_MESSAGE.matcher(event.getMessage());
		if (matcher.matches())
		{
			final String fishName = matcher.group(1);
			if (FISH_TYPES.containsKey(fishName))
			{
				fishCaughtMessages.incrementAndGet();
			}
		}
		else if (DOUBLE_CATCH_MESSAGE.matcher(event.getMessage()).matches())
		{
			fishCaughtMessages.incrementAndGet();
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event.getContainerId() == InventoryID.INVENTORY.getId())
		{
			for (final Item newItemId : event.getItemContainer().getItems())
			{
				if (FISH_TYPES.containsValue(newItemId.getId()))
				{
					newFishInInventory.incrementAndGet();
				}
			}
			updateInventory(InventoryID.INVENTORY, items -> inventoryItems = items);
		}
		else if (event.getContainerId() == InventoryID.EQUIPMENT.getId())
		{
			updateInventory(InventoryID.EQUIPMENT, items -> equipmentItems = items);
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (fishCaughtMessages.get() > 0)
		{
			final FishBarrel barrel = FishBarrel.INSTANCE;

			if (newFishInInventory.get() == 0)
			{
				// all fish went to barrel
				barrel.setHolding(Math.min(FishBarrel.CAPACITY, barrel.getHolding() + fishCaughtMessages.get()));
			}
			else
			{
				// barrel is full
				barrel.setHolding(FishBarrel.CAPACITY);
				barrel.setUnknown(false);
			}
		}

		fishCaughtMessages.set(0);
		newFishInInventory.set(0);
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		int itemId = -1;
		switch (event.getMenuAction())
		{
			case ITEM_FIRST_OPTION:
			case ITEM_SECOND_OPTION:
			case ITEM_THIRD_OPTION:
			case ITEM_FOURTH_OPTION:
			case ITEM_FIFTH_OPTION:
				itemId = event.getId();
				break;

			case CC_OP:
			case CC_OP_LOW_PRIORITY:
				int widgetId = event.getWidgetId();
				Widget widget = client.getWidget(TO_GROUP(widgetId), TO_CHILD(widgetId));
				if (widget != null)
				{
					int child = event.getActionParam();
					if (child == -1)
					{
						return;
					}

					widget = widget.getChild(child);
					if (widget != null)
					{
						itemId = widget.getItemId();
					}
				}
				break;

			default:
				return;
		}

		if (!FishBarrel.ITEM_IDS.contains(itemId))
		{
			return;
		}

		if ("Empty".equals(event.getMenuOption()))
		{
			FishBarrel.INSTANCE.setHolding(0);
			FishBarrel.INSTANCE.setUnknown(false);
		}
	}

	private void updateInventory(InventoryID inventoryID, Consumer<Set<Integer>> consumer)
	{
		clientThread.invokeLater(() ->
		{
			final ItemContainer itemContainer = client.getItemContainer(inventoryID);
			if (itemContainer != null)
			{
				final Set<Integer> itemIds = Arrays.stream(itemContainer.getItems())
					.map(Item::getId)
					.collect(Collectors.toSet());
				consumer.accept(itemIds);
			}
		});
	}

	private boolean hasOpenBarrel()
	{
		for (final int itemId : FishBarrel.OPEN_ITEM_IDS)
		{
			if (inventoryItems.contains(itemId) || equipmentItems.contains(itemId))
			{
				return true;
			}
		}
		return false;
	}
}
