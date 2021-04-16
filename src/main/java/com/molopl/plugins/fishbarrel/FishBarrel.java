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
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.ItemID;

public enum FishBarrel
{
	STATE;

	public static final int CAPACITY = 28;

	public static final Collection<Integer> BARREL_IDS = ImmutableList.of(
		ItemID.FISH_BARREL,
		ItemID.OPEN_FISH_BARREL,
		ItemID.FISH_SACK_BARREL,
		ItemID.OPEN_FISH_SACK_BARREL
	);

	public static final Collection<Integer> OPEN_BARREL_IDS = ImmutableList.of(
		ItemID.OPEN_FISH_BARREL,
		ItemID.OPEN_FISH_SACK_BARREL
	);

	@Getter
	@Setter
	private int holding;

	@Getter
	@Setter
	private boolean unknown = true;
}
