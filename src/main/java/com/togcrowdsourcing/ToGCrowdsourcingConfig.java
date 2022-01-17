package net.runelite.client.plugins.togcrowdsourcing.src.main.java.com.togcrowdsourcing;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.ui.overlay.components.ComponentConstants;

@ConfigGroup("example")
public interface ToGCrowdsourcingConfig extends Config
{
	@ConfigItem(
		keyName = "greeting",
		name = "Welcome Greeting",
		description = "The message to show to the user when they login"
	)
	default String greeting()
	{
		return "Hello";
	}

	@ConfigItem(
			keyName = "imagePointX",
			name = "Image Point X",
			description = "Image X.",
			position = 1
	)
	default int getImagePointX()
	{
		return 0;
	}

	@ConfigItem(
			keyName = "imagePointY",
			name = "Image Point Y",
			description = "Image Y.",
			position = 2
	)
	default int getImagePointY()
	{
		return 0;
	}

	@ConfigItem(
			keyName = "splitPointX",
			name = "Split Point X",
			description = "Split X.",
			position = 3
	)
	default int getSplitPointX()
	{
		return 0;
	}

	@ConfigItem(
			keyName = "splitPointY",
			name = "Split Point Y",
			description = "Split Y.",
			position = 4
	)
	default int getSplitPointY()
	{
		return 0;
	}

	@ConfigItem(
			keyName = "dimensionX",
			name = "Dimension X",
			description = "Dimension X.",
			position = 5
	)
	default int getDimensionX()
	{
		return ComponentConstants.STANDARD_WIDTH;
	}

	@ConfigItem(
			keyName = "dimensionY",
			name = "Dimension Y",
			description = "Dimension Y.",
			position = 6
	)
	default int getDimensionY()
	{
		return 0;
	}
}
