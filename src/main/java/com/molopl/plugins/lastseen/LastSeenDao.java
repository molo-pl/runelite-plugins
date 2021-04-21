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

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;

@Slf4j
public class LastSeenDao
{
	public static final String CONFIG_GROUP = "lastSeen";
	public static final String KEY_PREFIX = "lastSeen_";

	private final ConfigManager configManager;
	private final Map<String, Long> cache = new HashMap<>();

	@Inject
	public LastSeenDao(ConfigManager configManager)
	{
		this.configManager = configManager;
	}

	@Nullable
	public Long getLastSeen(String displayName)
	{
		if (cache.containsKey(displayName))
		{
			return cache.get(displayName);
		}

		final String entry = configManager.getConfiguration(CONFIG_GROUP, KEY_PREFIX + displayName);
		if (entry == null)
		{
			return null;
		}

		try
		{
			final long timestampMillis = Long.parseLong(entry);
			cache.put(displayName, timestampMillis);
			return timestampMillis;
		}
		catch (NumberFormatException e)
		{
			log.info("Invalid value stored as 'last seen' for player " + displayName + ": " + entry, e);
			return null;
		}
	}

	public void setLastSeen(String displayName, long timestampMillis)
	{
		final Long lastSeen = getLastSeen(displayName);
		if (lastSeen == null || lastSeen < timestampMillis)
		{
			cache.put(displayName, timestampMillis);
			configManager.setConfiguration(CONFIG_GROUP, KEY_PREFIX + displayName, Long.toString(timestampMillis));
		}
	}

	public void deleteLastSeen(String displayName)
	{
		cache.remove(displayName);
		configManager.unsetConfiguration(CONFIG_GROUP, KEY_PREFIX + displayName);
	}

	public void migrateLastSeen(String oldDisplayName, String newDisplayName)
	{
		final Long lastSeen = getLastSeen(oldDisplayName);
		if (lastSeen != null)
		{
			setLastSeen(newDisplayName, lastSeen);
			deleteLastSeen(oldDisplayName);
		}
	}
}
