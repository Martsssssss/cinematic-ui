/*
 * Copyright (c) 2026, Marts
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
package com.cinematicui;

import net.runelite.api.gameval.InterfaceID.ToplevelOsrsStretch;
import net.runelite.api.gameval.InterfaceID.ToplevelPreEoc;

/**
 * The parts of the interface the plugin can fade. Each one lists the top level
 * containers that hold it in the classic and modern resizable layouts.
 */
enum UiElement
{
	CHATBOX(
		new int[]{ToplevelOsrsStretch.CHAT_CONTAINER},
		new int[]{ToplevelPreEoc.CHAT_CONTAINER}
	),
	MINIMAP(
		new int[]{ToplevelOsrsStretch.MAP_CONTAINER},
		new int[]{ToplevelPreEoc.MAP_CONTAINER}
	),
	SIDE_PANEL(
		new int[]{ToplevelOsrsStretch.SIDE_MENU},
		new int[]{
			ToplevelPreEoc.SIDE_STATIC_LAYER,
			ToplevelPreEoc.SIDE_MOVABLE_LAYER,
			ToplevelPreEoc.SIDE_CONTAINER,
		}
	),
	XP_DROPS(
		new int[]{ToplevelOsrsStretch.XP_DROPS},
		new int[]{ToplevelPreEoc.XP_DROPS}
	),
	STATUS_ICONS(
		new int[]{
			ToplevelOsrsStretch.MULTIWAY_ICON,
			ToplevelOsrsStretch.PVP_ICONS,
			ToplevelOsrsStretch.BUFF_BAR,
			ToplevelOsrsStretch.STAT_BOOSTS_HUD,
		},
		new int[]{
			ToplevelPreEoc.MULTIWAY_ICON,
			ToplevelPreEoc.PVP_ICONS,
			ToplevelPreEoc.BUFF_BAR,
			ToplevelPreEoc.STAT_BOOSTS_HUD,
		}
	),
	SPLIT_CHAT(
		new int[]{ToplevelOsrsStretch.PM_CONTAINER},
		new int[]{ToplevelPreEoc.PM_CONTAINER}
	);

	private final int[] classic;
	private final int[] modern;

	UiElement(int[] classic, int[] modern)
	{
		this.classic = classic;
		this.modern = modern;
	}

	int[] components(boolean modernLayout)
	{
		return modernLayout ? modern : classic;
	}
}
