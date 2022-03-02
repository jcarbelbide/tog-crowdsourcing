package com.togcrowdsourcing;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("togcrowdsourcing")
public interface ToGCrowdsourcingConfig extends Config
{
	String GROUP = "togcrowdsourcing";

	@ConfigItem(
			keyName = "hidePVPWorlds",
			name = "Hide PVP Worlds",
			description = "Hide PVP worlds in the list of worlds. On by default. ",
			position = 1
	)
	default boolean hidePVPWorlds()
	{
		return true;
	}

	@ConfigItem(
			keyName = "hideHighRiskWorlds",
			name = "Hide High Risk Worlds",
			description = "Hide High Risk worlds in the list of worlds. Off by default. ",
			position = 2
	)
	default boolean hideHighRiskWorlds()
	{
		return false;
	}

	@ConfigItem(
			keyName = "showSidebar",
			name = "Show world switcher sidebar",
			description = "Show sidebar containing all worlds that mimics in-game interface",
			position = 3
	)
	default boolean showSidebar()
	{
		return true;
	}

	@ConfigItem(
			keyName = "showOverlay",
			name = "Show stream order overlay",
			description = "Show the overlay that shows the tear stream data being collected.",
			position = 4
	)
	default boolean showOverlay()
	{
		return true;
	}

	@ConfigItem(
			keyName = "showMessage",
			name = "Show world hop message in chat",
			description = "Shows what world is being hopped to in the chat",
			position = 5
	)
	default boolean showWorldHopMessage()
	{
		return true;
	}
}
