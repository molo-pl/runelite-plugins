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

import com.molopl.plugins.fishbarrel.FishBarrelWidgetParser.ParseResult;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class FishBarrelWidgetParserTest
{
	private final FishBarrelWidgetParser parser = new FishBarrelWidgetParser();

	@Test
	public void testInvalidMessage()
	{
		assertEquals(ParseResult.INVALID, parser.parse("Hello, World!"));
	}

	@Test
	public void testValidEmptyMessage()
	{
		assertEquals(ParseResult.VALID, parser.parse("The barrel is empty."));
		assertEquals(0, parser.getFishCount());
	}

	@Test
	public void testValidSingleWidgetMessage()
	{
		assertEquals(ParseResult.VALID, parser.parse(String.join("<br>",
			"The barrel contains:",
			"1 x Raw anglerfish, 2 x Raw monkfish, 3 x Raw",
			"shrimps, 1 x Raw anchovies, 1 x Raw salmon, 1 x Raw",
			"cod, 1 x Raw macerel, 1 x Raw tuna, 1 x Raw bass")));
		assertEquals(12, parser.getFishCount());
	}

	@Test
	public void testValidMultiWidgetMessage()
	{
		assertEquals(ParseResult.INCOMPLETE, parser.parse(String.join("<br>",
			"The barrel contains:",
			"1 x Raw anglerfish, 2 x Raw monkfish, 3 x Raw",
			"shrimps, 1 x Raw anchovies, 1 x Raw salmon, 1 x Raw",
			"cod, 1 x Raw macerel, 1 x Raw tuna, 1 x Raw bass,")));
		assertEquals(ParseResult.VALID, parser.parse(String.join("<br>",
			"1 x Raw swordfish, 1 x Raw lobster, 1 x Raw shark,",
			"1 x Raw manta ray, 1 x Raw sea turtle")));
		assertEquals(17, parser.getFishCount());
	}

	@Test
	public void testSecondWidgetMessageOnly()
	{
		assertEquals(ParseResult.INVALID, parser.parse("28 x Raw swordfish"));
	}

	@Test
	public void testValidToInvalidMessage()
	{
		assertEquals(ParseResult.VALID, parser.parse(String.join("<br>",
			"The barrel contains:",
			"10 x Raw swordfish, 10 x Raw shark")));
		assertEquals(ParseResult.INVALID, parser.parse("Hello, World!"));
	}

	@Test
	public void testInvalidToValidMessage()
	{
		assertEquals(ParseResult.INVALID, parser.parse("Hello, World!"));
		assertEquals(ParseResult.VALID, parser.parse(String.join("<br>",
			"The barrel contains:",
			"28 x Raw anglerfish")));

		assertEquals(28, parser.getFishCount());
	}
}
