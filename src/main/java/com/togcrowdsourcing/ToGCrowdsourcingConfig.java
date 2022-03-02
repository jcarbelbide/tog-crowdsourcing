package com.togcrowdsourcing;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("example")
public interface ToGCrowdsourcingConfig extends Config
{
	String GROUP = "worldhopper";

	@ConfigItem(
			keyName = "quickhopOutOfDanger",
			name = "Quick-hop out of dangerous worlds",
			description = "Don't hop to a PVP/high risk world when quick-hopping",
			position = 2
	)
	default boolean quickhopOutOfDanger()
	{
		return true;
	}

	@ConfigItem(
			keyName = "showSidebar",
			name = "Show world switcher sidebar",
			description = "Show sidebar containing all worlds that mimics in-game interface",
			position = 4
	)
	default boolean showSidebar()
	{
		return true;
	}

	@ConfigItem(
			keyName = "showMessage",
			name = "Show world hop message in chat",
			description = "Shows what world is being hopped to in the chat",
			position = 6
	)
	default boolean showWorldHopMessage()
	{
		return true;
	}
}
