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

import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Arrays;
import java.util.Objects;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

public class LastSeenFormatter
{
	@Getter
	@RequiredArgsConstructor
	private enum TimeUnit
	{
		DAYS(ChronoUnit.DAYS, "day", "days"),
		HOURS(ChronoUnit.HOURS, "hour", "hours"),
		MINUTES(ChronoUnit.MINUTES, "minute", "minutes");

		private final TemporalUnit temporalUnit;
		private final String singularForm;
		private final String pluralForm;
	}

	/**
	 * Format the given 'last seen' timestamp. Outputs relative time, e.g. "2 hours ago".
	 */
	public static String format(@Nullable Long lastSeenMillis)
	{
		if (lastSeenMillis == null)
		{
			return "never";
		}

		final long diffMillis = System.currentTimeMillis() - lastSeenMillis;

		return Arrays.stream(TimeUnit.values())
			.map(timeUnit -> formatIfInUnit(diffMillis, timeUnit))
			.filter(Objects::nonNull)
			.findFirst()
			.orElse("just now");
	}

	/**
	 * Format the given time duration in the given time unit, provided that the value is at least 1. Return null if the
	 * given time unit is not granular enough.
	 */
	@Nullable
	private static String formatIfInUnit(long diffMillis, TimeUnit timeUnit)
	{
		final long durationInUnit = diffMillis / timeUnit.getTemporalUnit().getDuration().toMillis();
		return durationInUnit > 0
			? String.format("%d %s ago", durationInUnit, durationInUnit == 1 ? timeUnit.getSingularForm() : timeUnit.getPluralForm())
			: null;
	}
}
