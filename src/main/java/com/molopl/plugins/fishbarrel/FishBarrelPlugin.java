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
import com.google.common.collect.ImmutableSet;
import java.util.Arrays;
import java.util.Collection;
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
import net.runelite.api.Skill;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.FakeXpDrop;
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
	/**
	 * Regex to recognize a chat message as an indicator of a caught fish.
	 * <p>
	 * This regex matches only the beginning of the message, since there may be some extra text afterwards
	 * (for example, infernal eels chat messages are followed by "It hardens as you handle it with your ice gloves.")
	 */
	private static final Pattern FISH_CAUGHT_MESSAGE = Pattern.compile("^You catch (an?|some|[0-9]+) ([a-zA-Z]+)[.!]");

	private static final String RADA_DOUBLE_CATCH_MESSAGE = "Rada's blessing enabled you to catch an extra fish.";
	private static final String FLAKES_DOUBLE_CATCH_MESSAGE = "The spirit flakes enabled you to catch an extra fish.";
	private static final String CORMORANT_CATCH_MESSAGE = "Your cormorant returns with its catch.";

	private static final String BANK_FULL_MESSAGE = "Your bank could not hold your fish.";

	/**
	 * Maps the name of the fish as it appears in chat message to corresponding item ID.
	 */
	private static final Map<String, Integer> FISH_TYPES_BY_NAME = ImmutableMap.<String, Integer>builder()
		.put("shrimps", ItemID.RAW_SHRIMPS)
		.put("sardine", ItemID.RAW_SARDINE)
		.put("Karambwanji", ItemID.KARAMBWANJI) // test
		.put("herring", ItemID.RAW_HERRING)
		.put("anchovies", ItemID.RAW_ANCHOVIES)
		.put("mackerel", ItemID.RAW_MACKEREL)
		.put("trout", ItemID.RAW_TROUT)
		.put("cod", ItemID.RAW_COD)
		.put("pike", ItemID.RAW_PIKE)
		.put("slimy swamp eel", ItemID.RAW_SLIMY_EEL)
		.put("salmon", ItemID.RAW_SALMON)
		.put("tuna", ItemID.RAW_TUNA)
		.put("rainbow fish", ItemID.RAW_RAINBOW_FISH)
		.put("cave eel", ItemID.RAW_CAVE_EEL)
		.put("lobster", ItemID.RAW_LOBSTER)
		.put("bass", ItemID.RAW_BASS)
		.put("leaping trout", ItemID.LEAPING_TROUT)
		.put("swordfish", ItemID.RAW_SWORDFISH)
		.put("lava eel", ItemID.RAW_LAVA_EEL)
		.put("leaping salmon", ItemID.LEAPING_SALMON)
		.put("monkfish", ItemID.RAW_MONKFISH)
		.put("Karambwan", ItemID.RAW_KARAMBWAN)
		.put("leaping sturgeon", ItemID.LEAPING_STURGEON)
		.put("shark", ItemID.RAW_SHARK)
		.put("infernal eel", ItemID.INFERNAL_EEL)
		.put("minnows", ItemID.MINNOW)
		.put("anglerfsh", ItemID.RAW_ANGLERFISH)
		.put("dark crab", ItemID.RAW_DARK_CRAB)
		.put("sacred eel", ItemID.SACRED_EEL)
		.build();

	/**
	 * A set of possible fish caught with a cormorant on Molch island.
	 */
	private static final Set<Integer> MOLCH_ISLAND_FISH_TYPES = ImmutableSet.of(
		ItemID.BLUEGILL,
		ItemID.COMMON_TENCH,
		ItemID.MOTTLED_EEL,
		ItemID.GREATER_SIREN
	);

	private static final Set<Integer> ALL_FISH_TYPES = ImmutableSet.<Integer>builder()
		.addAll(FISH_TYPES_BY_NAME.values())
		.addAll(MOLCH_ISLAND_FISH_TYPES)
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
	/**
	 * Number of cooking XP drops since last barrel's state update (to take infernal harpoon into account).
	 */
	private final AtomicInteger cookingXpDrops = new AtomicInteger();

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
		if (event.getType() == ChatMessageType.GAMEMESSAGE && hasAnyOfItems(FishBarrel.BARREL_IDS))
		{
			if (BANK_FULL_MESSAGE.equals(event.getMessage()))
			{
				// couldn't deposit all fish, we've lost track
				FishBarrel.INSTANCE.setUnknown(true);
			}
		}
		else if (event.getType() == ChatMessageType.SPAM && hasAnyOfItems(FishBarrel.OPEN_BARREL_IDS))
		{
			final Matcher matcher = FISH_CAUGHT_MESSAGE.matcher(event.getMessage());
			if (matcher.matches())
			{
				final String fishName = matcher.group(2);
				if (FISH_TYPES_BY_NAME.containsKey(fishName))
				{
					final String fishCountStr = matcher.group(1);
					final int fishCount;
					switch (fishCountStr)
					{
						case "a":
						case "an":
						case "some":
							fishCount = 1;
							break;
						default:
							try
							{
								fishCount = Integer.parseInt(fishCountStr);
							}
							catch (NumberFormatException e)
							{
								return;
							}
							break;
					}
					fishCaughtMessages.updateAndGet(i -> i + fishCount);
				}
			}
			else
			{
				switch (event.getMessage())
				{
					case RADA_DOUBLE_CATCH_MESSAGE:
					case FLAKES_DOUBLE_CATCH_MESSAGE:
						// TODO: handle double catches
					case CORMORANT_CATCH_MESSAGE:
						fishCaughtMessages.incrementAndGet();
						break;
					default:
				}
			}
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event.getContainerId() == InventoryID.INVENTORY.getId())
		{
			for (final Item newItemId : event.getItemContainer().getItems())
			{
				if (ALL_FISH_TYPES.contains(newItemId.getId()))
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
	public void onFakeXpDrop(FakeXpDrop event)
	{
		if (event.getSkill() == Skill.COOKING)
		{
			cookingXpDrops.incrementAndGet();
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (fishCaughtMessages.get() > 0)
		{
			final FishBarrel barrel = FishBarrel.INSTANCE;

			// if all fish went to barrel
			if (newFishInInventory.get() == 0)
			{
				// infernal harpoon can reduce number of caught fish
				final int delta = Math.max(0, fishCaughtMessages.get() - cookingXpDrops.get());
				barrel.setHolding(Math.min(FishBarrel.CAPACITY, barrel.getHolding() + delta));
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
		cookingXpDrops.set(0);
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

		if (!FishBarrel.BARREL_IDS.contains(itemId))
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

	private boolean hasAnyOfItems(Collection<Integer> itemIds)
	{
		for (final int itemId : itemIds)
		{
			if (inventoryItems.contains(itemId) || equipmentItems.contains(itemId))
			{
				return true;
			}
		}
		return false;
	}
}
