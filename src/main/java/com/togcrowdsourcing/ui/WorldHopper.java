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
package com.togcrowdsourcing.ui;

import com.google.common.base.Stopwatch;
import com.togcrowdsourcing.CrowdsourcingManager;
import com.togcrowdsourcing.ToGCrowdsourcingConfig;
import com.togcrowdsourcing.ToGCrowdsourcingPlugin;
import com.togcrowdsourcing.WorldData;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
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
import com.togcrowdsourcing.ping.Ping;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.*;
import net.runelite.http.api.worlds.World;
import net.runelite.http.api.worlds.WorldResult;

import javax.inject.Inject;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class WorldHopper
{
	private static final int REFRESH_THROTTLE = 60_000; // ms
	private static final int MAX_PLAYER_COUNT = 1950;

	private static final int DISPLAY_SWITCHER_MAX_ATTEMPTS = 3;

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

	private ToGCrowdsourcingConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private WorldHopperPingOverlay worldHopperOverlay;

	@Getter
	@Inject
	private WorldService worldService;

	@Getter
	@Inject
	private CrowdsourcingManager crowdsourcingManager;

	private ScheduledExecutorService hopperExecutorService;

	private NavigationButton navButton;
	private WorldSwitcherPanel panel;

	private net.runelite.api.World quickHopTargetWorld;
	private int displaySwitcherAttempts = 0;

	@Getter
	private int lastWorld;

	private ScheduledFuture<?> pingFuture, currPingFuture;
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

	@Getter(AccessLevel.PACKAGE)
	private int currentPing;

	private final Map<Integer, Integer> storedPings = new HashMap<>();

	public void startUpWorldHopper(ToGCrowdsourcingConfig config)
	{
		currentPing = -1;

		panel = new WorldSwitcherPanel(this);
		this.config = config;

		final BufferedImage icon = ImageUtil.loadImageResource(ToGCrowdsourcingPlugin.class, "/tog-icon.png");
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

		// The plugin has its own executor for pings, as it blocks for a long time
		hopperExecutorService = new ExecutorServiceExceptionLogger(Executors.newSingleThreadScheduledExecutor());
		// Run the first-run ping
		hopperExecutorService.execute(this::pingInitialWorlds);

		// Give some initial delay - this won't run until after pingInitialWorlds finishes from tick() anyway
		pingFuture = hopperExecutorService.scheduleWithFixedDelay(this::pingNextWorld, 15, 3, TimeUnit.SECONDS);
		currPingFuture = hopperExecutorService.scheduleWithFixedDelay(this::pingCurrentWorld, 15, 1, TimeUnit.SECONDS);

		// populate initial world list
		updateList();

		crowdsourcingManager.makeGetRequest(this);
	}

	public void shutDownWorldHopper()
	{
		pingFuture.cancel(true);
		pingFuture = null;

		currPingFuture.cancel(true);
		currPingFuture = null;

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
				case "ping":
					if (config.ping())
					{
						SwingUtilities.invokeLater(() -> panel.showPing());
					}
					else
					{
						SwingUtilities.invokeLater(() -> panel.hidePing());
					}
					break;
			}
			if (event.getKey().equals("showOverlay")) { return; }
			else { updateList(); }
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
//		System.out.println("onWorldFetch");
		synchronized (this)
		{
			crowdsourcingManager.makeGetRequest(this);
			updateList();
		}
	}

	/**
	 * This method ONLY updates the list's UI, not the actual world list and data it displays.
	 */
	public void updateList()
	{
//		if (isGetError()) { return; }
		SwingUtilities.invokeLater(() -> panel.populate(worldData, config));
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

	private void pingInitialWorlds()
	{
		WorldResult worldResult = worldService.getWorlds();
		if (worldResult == null || !config.showSidebar() || !config.ping())
		{
			return;
		}

		Stopwatch stopwatch = Stopwatch.createStarted();

		for (World world : worldResult.getWorlds())
		{
			int ping = ping(world);
			SwingUtilities.invokeLater(() -> panel.updatePing(world.getId(), ping));
		}

		stopwatch.stop();

		log.debug("Done pinging worlds in {}", stopwatch.elapsed());
	}

	/**
	 * Ping the next world
	 */
	private void pingNextWorld()
	{
		WorldResult worldResult = worldService.getWorlds();
		if (worldResult == null || !config.showSidebar() || !config.ping())
		{
			return;
		}

		List<World> worlds = worldResult.getWorlds();
		if (worlds.isEmpty())
		{
			return;
		}

		if (currentWorld >= worlds.size())
		{
			// Wrap back around
			currentWorld = 0;
		}

		World world = worlds.get(currentWorld++);

		// If we are displaying the ping overlay, there is a separate scheduled task for the current world
		boolean displayPing = config.displayPing() && client.getGameState() == GameState.LOGGED_IN;
		if (displayPing && client.getWorld() == world.getId())
		{
			return;
		}

		int ping = ping(world);
		log.trace("Ping for world {} is: {}", world.getId(), ping);
		SwingUtilities.invokeLater(() -> panel.updatePing(world.getId(), ping));
	}

	/**
	 * Ping the current world for the ping overlay
	 */
	private void pingCurrentWorld()
	{
		WorldResult worldResult = worldService.getWorlds();
		// There is no reason to ping the current world if not logged in, as the overlay doesn't draw
		if (worldResult == null || !config.displayPing() || client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		final World currentWorld = worldResult.findWorld(client.getWorld());
		if (currentWorld == null)
		{
			log.debug("unable to find current world: {}", client.getWorld());
			return;
		}

		currentPing = ping(currentWorld);
		log.trace("Ping for current world is: {}", currentPing);

		SwingUtilities.invokeLater(() -> panel.updatePing(currentWorld.getId(), currentPing));
	}

	Integer getStoredPing(World world)
	{
		if (!config.ping())
		{
			return null;
		}

		return storedPings.get(world.getId());
	}

	private int ping(World world)
	{
		int ping = Ping.ping(world);
		storedPings.put(world.getId(), ping);
		return ping;
	}
}
