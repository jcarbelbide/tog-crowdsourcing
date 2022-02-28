/*
 * Copyright (c) 2017, Adam <Adam@sigterm.info>
 * Copyright (c) 2018, Lotto <https://github.com/devLotto>
 * Copyright (c) 2019, gregg1494 <https://github.com/gregg1494>
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
package net.runelite.client.plugins.togcrowdsourcing.src.main.java.com.togcrowdsourcing.ui;

import com.google.common.collect.ImmutableList;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.clan.ClanChannel;
import net.runelite.api.clan.ClanChannelMember;
import net.runelite.api.events.*;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.WorldsFetch;
import net.runelite.client.game.WorldService;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.togcrowdsourcing.src.main.java.com.togcrowdsourcing.CrowdsourcingManager;
import net.runelite.client.plugins.togcrowdsourcing.src.main.java.com.togcrowdsourcing.ToGCrowdsourcingConfig;
import net.runelite.client.plugins.togcrowdsourcing.src.main.java.com.togcrowdsourcing.WorldData;
import net.runelite.client.plugins.worldhopper.WorldHopperPlugin;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.*;
import net.runelite.http.api.worlds.World;
import net.runelite.http.api.worlds.WorldResult;
import net.runelite.http.api.worlds.WorldType;

import javax.inject.Inject;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Slf4j
public class WorldHopper
{
	private static final int REFRESH_THROTTLE = 60_000; // ms
	private static final int MAX_PLAYER_COUNT = 1950;

	private static final int DISPLAY_SWITCHER_MAX_ATTEMPTS = 3;

	private static final String HOP_TO = "Hop-to";
	private static final String KICK_OPTION = "Kick";
	private static final ImmutableList<String> BEFORE_OPTIONS = ImmutableList.of("Add friend", "Remove friend", KICK_OPTION);
	private static final ImmutableList<String> AFTER_OPTIONS = ImmutableList.of("Message");

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ConfigManager configManager;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private KeyManager keyManager;

	@Inject
	private ChatMessageManager chatMessageManager;

	@Inject
	private ToGCrowdsourcingConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Getter
	@Inject
	private WorldService worldService;

	@Inject
	private CrowdsourcingManager crowdsourcingManager;

	private ScheduledExecutorService hopperExecutorService;

	private NavigationButton navButton;
	private WorldSwitcherPanel panel;

	private net.runelite.api.World quickHopTargetWorld;
	private int displaySwitcherAttempts = 0;

	@Getter
	private int lastWorld;

	private int currentWorld;
	private Instant lastFetch;

	@Getter
	@Setter
	private boolean getError;

	@Getter
	@Setter
	private ArrayList<WorldData> worldData = new ArrayList<>();

	@Inject
	private WorldHopper()
	{

	}

	public void startUpWorldHopper()
	{
		panel = new WorldSwitcherPanel(this);

		BufferedImage icon = ImageUtil.loadImageResource(WorldHopperPlugin.class, "icon.png");
		navButton = NavigationButton.builder()
			.tooltip("ToG Crowdsourcing")
			.icon(icon)
			.priority(10)
			.panel(panel)
			.build();

		if (config.showSidebar())
		{
			clientToolbar.addNavigation(navButton);
		}

		// populate initial world list
		updateList();

		crowdsourcingManager.makeGetRequest(this);
	}

	public void shutDownWorldHopper()
	{
		clientToolbar.removeNavigation(navButton);

	}

	@Subscribe
	public void onConfigChanged(final ConfigChanged event)
	{
		if (event.getGroup().equals(ToGCrowdsourcingConfig.GROUP))
		{
			switch (event.getKey())
			{
				case "showSidebar":
					if (config.showSidebar())
					{
						clientToolbar.addNavigation(navButton);
					}
					else
					{
						clientToolbar.removeNavigation(navButton);
					}
					break;
			}
		}
	}

	int getCurrentWorld()
	{
		return client.getWorld();
	}

	void hopTo(World world)
	{
		clientThread.invoke(() -> hop(world.getId()));
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		// If the player has disabled the side bar plugin panel, do not update the UI
		if (config.showSidebar() && gameStateChanged.getGameState() == GameState.LOGGED_IN)
		{
			if (lastWorld != client.getWorld())
			{
				int newWorld = client.getWorld();
				panel.switchCurrentHighlight(newWorld, lastWorld);
				lastWorld = newWorld;
			}
		}
	}

	@Subscribe
	public void onWorldListLoad(WorldListLoad worldListLoad)
	{
		System.out.println("onWorldListLoad");
		if (!config.showSidebar())
		{
			return;
		}

		Map<Integer, Integer> worldData = new HashMap<>();

		for (net.runelite.api.World w : worldListLoad.getWorlds())
		{
			worldData.put(w.getId(), w.getPlayerCount());
		}

		panel.updateListData(worldData);
		this.lastFetch = Instant.now(); // This counts as a fetch as it updates populations
	}

	// This is the right click refresh menu item
	void refresh()
	{
		Instant now = Instant.now();
		if (lastFetch != null && now.toEpochMilli() - lastFetch.toEpochMilli() < REFRESH_THROTTLE)
		{
			log.debug("Throttling world refresh");
			return;
		}

		lastFetch = now;
		worldService.refresh();
	}

	@Subscribe
	public void onWorldsFetch(WorldsFetch worldsFetch)
	{
		System.out.println("onWorldFetch");
		updateList();
	}

	/**
	 * This method ONLY updates the list's UI, not the actual world list and data it displays.
	 */
	public void updateList()
	{
		SwingUtilities.invokeLater(() -> panel.populate(worldData));
	}

	private void hop(int worldId)
	{
		assert client.isClientThread();

		WorldResult worldResult = worldService.getWorlds();
		// Don't try to hop if the world doesn't exist
		World world = worldResult.findWorld(worldId);
		if (world == null)
		{
			return;
		}

		final net.runelite.api.World rsWorld = client.createWorld();
		rsWorld.setActivity(world.getActivity());
		rsWorld.setAddress(world.getAddress());
		rsWorld.setId(world.getId());
		rsWorld.setPlayerCount(world.getPlayers());
		rsWorld.setLocation(world.getLocation());
		rsWorld.setTypes(WorldUtil.toWorldTypes(world.getTypes()));

		if (client.getGameState() == GameState.LOGIN_SCREEN)
		{
			// on the login screen we can just change the world by ourselves
			client.changeWorld(rsWorld);
			return;
		}

		if (config.showWorldHopMessage())
		{
			String chatMessage = new ChatMessageBuilder()
				.append(ChatColorType.NORMAL)
				.append("Quick-hopping to World ")
				.append(ChatColorType.HIGHLIGHT)
				.append(Integer.toString(world.getId()))
				.append(ChatColorType.NORMAL)
				.append("..")
				.build();

			chatMessageManager
				.queue(QueuedMessage.builder()
					.type(ChatMessageType.CONSOLE)
					.runeLiteFormattedMessage(chatMessage)
					.build());
		}

		quickHopTargetWorld = rsWorld;
		displaySwitcherAttempts = 0;
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (quickHopTargetWorld == null)
		{
			return;
		}

		if (client.getWidget(WidgetInfo.WORLD_SWITCHER_LIST) == null)
		{
			client.openWorldHopper();

			if (++displaySwitcherAttempts >= DISPLAY_SWITCHER_MAX_ATTEMPTS)
			{
				String chatMessage = new ChatMessageBuilder()
					.append(ChatColorType.NORMAL)
					.append("Failed to quick-hop after ")
					.append(ChatColorType.HIGHLIGHT)
					.append(Integer.toString(displaySwitcherAttempts))
					.append(ChatColorType.NORMAL)
					.append(" attempts.")
					.build();

				chatMessageManager
					.queue(QueuedMessage.builder()
						.type(ChatMessageType.CONSOLE)
						.runeLiteFormattedMessage(chatMessage)
						.build());

				resetQuickHopper();
			}
		}
		else
		{
			client.hopToWorld(quickHopTargetWorld);
			resetQuickHopper();
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event.getType() != ChatMessageType.GAMEMESSAGE)
		{
			return;
		}

		if (event.getMessage().equals("Please finish what you're doing before using the World Switcher."))
		{
			resetQuickHopper();
		}
	}

	private void resetQuickHopper()
	{
		displaySwitcherAttempts = 0;
		quickHopTargetWorld = null;
	}

}
