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

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class FishBarrelWidgetParserTest
{
	private final FishBarrelWidgetParser parser = new FishBarrelWidgetParser();

	@Test
	public void testInvalidMessages()
	{
		assertEquals(-1, parser.parse("Hello, World!"));
		assertEquals(-1, parser.parse("28 x Raw swordfish"));
	}

	@Test
	public void testValidEmptyMessage()
	{
		assertEquals(0, parser.parse("The barrel is empty."));
	}

	@Test
	public void testValidMessages()
	{
		assertEquals(27, parser.parse(String.join("<br>",
			"The barrel contains:",
			"27 x Raw anglerfish")));
		assertEquals(12, parser.parse(String.join("<br>",
			"The barrel contains:",
			"1 x Raw anglerfish, 2 x Raw monkfish, 3 x Raw",
			"shrimps, 1 x Raw anchovies, 1 x Raw salmon, 1 x Raw",
			"cod, 1 x Raw macerel, 1 x Raw tuna, 1 x Raw bass")));
	}
}
