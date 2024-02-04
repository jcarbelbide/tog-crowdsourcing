/*

BSD 2-Clause License

Copyright (c) 2022, JC Arbelbide
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this
   list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

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
			keyName = "onlyShowOptimalWorlds",
			name = "Only Show Optimal Worlds",
			description = "Only show worlds that have a 'gggbbb' or 'bbbggg' pattern. Off by default. ",
			position = 3
	)
	default boolean onlyShowOptimalWorlds()
	{
		return false;
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
			keyName = "showOverlay",
			name = "Show stream order overlay",
			description = "Show the overlay that shows the tear stream data being collected.",
			position = 5
	)
	default boolean showOverlay()
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
