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

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

/**
 * A parser for messages from chat box widgets, for obtaining the contents of the fish barrel.
 */
public class FishBarrelWidgetParser
{
	public enum ParseResult
	{
		INVALID,
		INCOMPLETE,
		VALID
	}

	private static final String EMPTY_MESSAGE = "The barrel is empty.";
	private static final String FIRST_MESSAGE_PREFIX = "The barrel contains: ";

	private static final String MESSAGE_ENTRY_REGEX = "([0-9]+) x [a-zA-Z ]+,? ?";
	private static final Pattern MESSAGE_ENTRY_PATTERN = Pattern.compile(MESSAGE_ENTRY_REGEX);
	private static final Pattern FULL_MESSAGE_PATTERN = Pattern.compile("^(" + MESSAGE_ENTRY_REGEX + ")+$");

	// if a message ends with either of these, we consider it incomplete
	private static final Collection<String> INCOMPLETE_INDICATORS = ImmutableList.of("Raw", ",");

	@Getter
	private int fishCount;

	public ParseResult parse(String message)
	{
		if (StringUtils.isBlank(message))
		{
			return ParseResult.INVALID;
		}
		message = StringUtils.replace(message, "<br>", " ").trim();

		if (EMPTY_MESSAGE.equals(message))
		{
			fishCount = 0;
			return ParseResult.VALID;
		}

		if (message.startsWith(FIRST_MESSAGE_PREFIX))
		{
			fishCount = 0;
			message = message.substring(FIRST_MESSAGE_PREFIX.length());
		}
		else if (fishCount == 0)
		{
			// if we're not in the middle of parsing, this is an error
			return ParseResult.INVALID;
		}

		if (!FULL_MESSAGE_PATTERN.matcher(message).matches())
		{
			return ParseResult.INVALID;
		}

		final Matcher matcher = MESSAGE_ENTRY_PATTERN.matcher(message);
		while (matcher.find())
		{
			try
			{
				fishCount += Integer.parseInt(matcher.group(1));
			}
			catch (NumberFormatException e)
			{
				return ParseResult.INVALID;
			}
		}
		return INCOMPLETE_INDICATORS.stream().anyMatch(message::endsWith)
			? ParseResult.INCOMPLETE
			: ParseResult.VALID;
	}
}
