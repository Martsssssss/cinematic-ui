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

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Keybind;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup(CinematicUiConfig.GROUP)
public interface CinematicUiConfig extends Config
{
	String GROUP = "cinematicui";

	enum FadeSpeed
	{
		INSTANT("Instant", 0),
		FAST("Fast", 250),
		SMOOTH("Smooth", 800);

		private final String name;
		private final int millis;

		FadeSpeed(String name, int millis)
		{
			this.name = name;
			this.millis = millis;
		}

		public int getMillis()
		{
			return millis;
		}

		@Override
		public String toString()
		{
			return name;
		}
	}

	enum HoverWake
	{
		ELEMENT("Hovered element"),
		EVERYTHING("Everything");

		private final String name;

		HoverWake(String name)
		{
			this.name = name;
		}

		@Override
		public String toString()
		{
			return name;
		}
	}

	enum ElementMode
	{
		FADE("Fade"),
		ALWAYS_SHOW("Always show");

		private final String name;

		ElementMode(String name)
		{
			this.name = name;
		}

		@Override
		public String toString()
		{
			return name;
		}
	}

	@ConfigSection(
		name = "General",
		description = "When and how the interface fades",
		position = 0
	)
	String generalSection = "general";

	@ConfigSection(
		name = "Waking",
		description = "Bringing the interface back while it is faded",
		position = 1
	)
	String wakingSection = "waking";

	@ConfigSection(
		name = "Safety",
		description = "Show the interface when you need it",
		position = 2
	)
	String safetySection = "safety";

	@ConfigSection(
		name = "Elements",
		description = "Choose which parts of the interface fade",
		position = 3,
		closedByDefault = true
	)
	String elementsSection = "elements";

	@ConfigSection(
		name = "Plugins to hide",
		description = "Other plugins to switch off while the interface is faded, so their overlays go too",
		position = 4,
		closedByDefault = true
	)
	String pluginsSection = "plugins";

	@ConfigItem(
		keyName = "hotkey",
		name = "Hotkey",
		description = "Fades the interface straight away, or brings it back",
		section = generalSection,
		position = 0
	)
	default Keybind hotkey()
	{
		return Keybind.NOT_SET;
	}

	@Range(max = 600)
	@Units(Units.SECONDS)
	@ConfigItem(
		keyName = "idleDelay",
		name = "Idle delay",
		description = "Seconds without mouse movement or a keypress before fading. 0 turns it off",
		section = generalSection,
		position = 1
	)
	default int idleDelay()
	{
		return 10;
	}

	@Range(min = 10, max = 100)
	@Units(Units.PERCENT)
	@ConfigItem(
		keyName = "fadeAmount",
		name = "Fade amount",
		description = "How far elements fade. At 100% they are hidden completely and can't be clicked",
		section = generalSection,
		position = 2
	)
	default int fadeAmount()
	{
		return 100;
	}

	@ConfigItem(
		keyName = "fadeSpeed",
		name = "Fade speed",
		description = "How quickly elements fade out and back in",
		section = generalSection,
		position = 3
	)
	default FadeSpeed fadeSpeed()
	{
		return FadeSpeed.SMOOTH;
	}

	@ConfigItem(
		keyName = "hoverWakes",
		name = "Hover wakes",
		description = "While faded, hovering over an element shows just that element, or everything",
		section = wakingSection,
		position = 0
	)
	default HoverWake hoverWakes()
	{
		return HoverWake.ELEMENT;
	}

	@Range(min = 1, max = 30)
	@Units(Units.SECONDS)
	@ConfigItem(
		keyName = "chatTypingWake",
		name = "Show chat after typing",
		description = "How long the chatbox stays visible after you last typed in it",
		section = wakingSection,
		position = 1
	)
	default int chatTypingWake()
	{
		return 3;
	}

	@ConfigItem(
		keyName = "showChatOnPm",
		name = "Show chat on new PM",
		description = "Briefly show the chat when you get a private message, for the same time as Show chat after typing",
		section = wakingSection,
		position = 2
	)
	default boolean showChatOnPm()
	{
		return true;
	}

	@Range(max = 99)
	@Units(Units.PERCENT)
	@ConfigItem(
		keyName = "hpThreshold",
		name = "Show below HP",
		description = "Show everything when your Hitpoints drop below this. 0 turns it off",
		section = safetySection,
		position = 0
	)
	default int hpThreshold()
	{
		return 30;
	}

	@Range(max = 99)
	@Units(Units.PERCENT)
	@ConfigItem(
		keyName = "prayerThreshold",
		name = "Show below Prayer",
		description = "Show everything when your Prayer drops below this while a prayer is active. 0 turns it off",
		section = safetySection,
		position = 1
	)
	default int prayerThreshold()
	{
		return 15;
	}

	@ConfigItem(
		keyName = "showInCombat",
		name = "Show in combat",
		description = "Never fade while you're in combat",
		section = safetySection,
		position = 2
	)
	default boolean showInCombat()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showInActivities",
		name = "Show during activities",
		description = "Never fade while a minigame or activity display is on screen, such as raids, Wintertodt or Pest Control",
		section = safetySection,
		position = 3
	)
	default boolean showInActivities()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showInWilderness",
		name = "Show in the Wilderness",
		description = "Never fade in the Wilderness or on PvP worlds",
		section = safetySection,
		position = 4
	)
	default boolean showInWilderness()
	{
		return true;
	}

	@ConfigItem(
		keyName = "hiddenPlugins",
		name = "<html><p style='width:160px'>Type plugin names exactly as they appear in the plugin list, "
			+ "separated by commas. They switch off while the interface is faded and back on when it returns."
			+ "<br><br>Anything they track while switched off won't be recorded, so pick display-only plugins "
			+ "where you can.</p></html>",
		description = "",
		section = pluginsSection,
		position = 0
	)
	default String hiddenPlugins()
	{
		return "";
	}

	@ConfigItem(
		keyName = "chatbox",
		name = "Chatbox",
		description = "The chatbox and its tabs",
		section = elementsSection,
		position = 0
	)
	default ElementMode chatbox()
	{
		return ElementMode.FADE;
	}

	@ConfigItem(
		keyName = "minimap",
		name = "Minimap and orbs",
		description = "The minimap, compass and status orbs",
		section = elementsSection,
		position = 1
	)
	default ElementMode minimap()
	{
		return ElementMode.FADE;
	}

	@ConfigItem(
		keyName = "sidePanel",
		name = "Inventory and side panel",
		description = "The side panel, its tabs and whatever is open in it",
		section = elementsSection,
		position = 2
	)
	default ElementMode sidePanel()
	{
		return ElementMode.FADE;
	}

	@ConfigItem(
		keyName = "xpDrops",
		name = "XP drops",
		description = "The XP drop counter and drops",
		section = elementsSection,
		position = 3
	)
	default ElementMode xpDrops()
	{
		return ElementMode.FADE;
	}

	@ConfigItem(
		keyName = "statusIcons",
		name = "Status icons",
		description = "The multicombat icon, Wilderness and PvP icons, buff bar and stat boosts",
		section = elementsSection,
		position = 4
	)
	default ElementMode statusIcons()
	{
		return ElementMode.FADE;
	}

	@ConfigItem(
		keyName = "splitChat",
		name = "Split private chat",
		description = "Private messages shown above the chatbox, if you use split private chat",
		section = elementsSection,
		position = 5
	)
	default ElementMode splitChat()
	{
		return ElementMode.FADE;
	}
}
