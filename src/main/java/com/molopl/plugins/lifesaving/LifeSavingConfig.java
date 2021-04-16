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

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(LifeSavingConfig.GROUP)
public interface LifeSavingConfig extends Config
{
	String GROUP = "lifeSavingJewellery";

	@ConfigItem(
		keyName = "ringOfLifeInfobox",
		name = "Ring of life infobox",
		description = "Show infobox when Ring of life is worn"
	)
	default boolean ringOfLifeInfobox()
	{
		return true;
	}

	@ConfigItem(
		keyName = "ringOfLifeNotification",
		name = "Ring of life notification",
		description = "Notify when Ring of life is destroyed"
	)
	default boolean ringOfLifeNotification()
	{
		return true;
	}

	@ConfigItem(
		keyName = "phoenixNecklaceInfobox",
		name = "Phoenix necklace infobox",
		description = "Show infobox when Phoenix necklace is worn"
	)
	default boolean phoenixNecklaceInfobox()
	{
		return true;
	}

	@ConfigItem(
		keyName = "phoenixNecklaceNotification",
		name = "Phoenix necklace notification",
		description = "Notify when Phoenix necklace is destroyed"
	)
	default boolean phoenixNecklaceNotification()
	{
		return true;
	}
}
