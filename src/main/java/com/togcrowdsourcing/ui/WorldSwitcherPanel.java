/*
 * Copyright (c) 2018, Psikoi <https://github.com/Psikoi>
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

import com.google.common.collect.Ordering;
import com.togcrowdsourcing.ToGCrowdsourcingConfig;
import com.togcrowdsourcing.WorldData;
import lombok.Getter;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.PluginPanel;
import net.runelite.http.api.worlds.World;
import net.runelite.http.api.worlds.WorldResult;
import net.runelite.http.api.worlds.WorldType;
import net.runelite.api.EnumComposition;

import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.annotation.Nullable;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

class WorldSwitcherPanel extends PluginPanel
{
	private static final Color ODD_ROW = new Color(44, 44, 44);

	@Getter
	private static final int WORLD_COLUMN_WIDTH = 60;

	@Getter
	private static final int HITS_COLUMN_WIDTH = 50;

	private final JPanel listContainer = new JPanel();

	private WorldTableHeader worldHeader;
	private WorldTableHeader hitsHeader;
	private WorldTableHeader activityHeader;

	private WorldOrder orderIndex = WorldOrder.STREAM_ORDER;
	private boolean ascendingOrder = false;

	private final ArrayList<WorldTableRow> rows = new ArrayList<>();
	private final WorldHopper worldHopper;

	WorldSwitcherPanel(WorldHopper worldHopper)
	{
		this.worldHopper = worldHopper;

		setBorder(null);
		setLayout(new DynamicGridLayout(0, 1));

		JPanel headerContainer = buildHeader();

		listContainer.setLayout(new GridLayout(0, 1));

		add(headerContainer);
		add(listContainer);
	}

	void switchCurrentHighlight(int newWorld, int lastWorld)
	{
		for (WorldTableRow row : rows)
		{
			if (row.getWorld().getId() == newWorld)
			{
				row.recolour(true);
			}
			else if (row.getWorld().getId() == lastWorld)
			{
				row.recolour(false);
			}
		}
	}

	void updateList()
	{
		rows.sort((r1, r2) ->
		{
			switch (orderIndex)
			{
				case WORLD:
					return getCompareValue(r1, r2, row -> row.getWorld().getId());
				case HITS:
					return getCompareValue(r1, r2, WorldTableRow::getUpdatedHitsCount);
				case STREAM_ORDER:
					return getCompareValueStreamOrder(r1, r2);
				default:
					return 0;
			}
		});

		listContainer.removeAll();

		for (int i = 0; i < rows.size(); i++)
		{
			WorldTableRow row = rows.get(i);
			row.setBackground(i % 2 == 0 ? ODD_ROW : ColorScheme.DARK_GRAY_COLOR);
			listContainer.add(row);
		}

		listContainer.revalidate();
		listContainer.repaint();
	}

	private int getCompareValueStreamOrder(WorldTableRow r1, WorldTableRow r2) {
		int order = !ascendingOrder ? 1 : -1;
		if (r1.getWorldData().getStream_order().equals("gggbbb"))
		{
			if (r2.getWorldData().getStream_order().equals("gggbbb"))
			{
				return getCompareHits(r1, r2) * order;
			}
			else
			{
				return -1 * order;
			}
		}
		if (r1.getWorldData().getStream_order().equals("bbbggg"))
		{
			if (r2.getWorldData().getStream_order().equals("gggbbb"))
			{
				return 1 * order;
			}
			else if (r2.getWorldData().getStream_order().equals("bbbggg"))
			{
				return getCompareHits(r1, r2) * order;
			}
			else
			{
				return -1 * order;
			}
		}
		if (r2.getWorldData().getStream_order().equals("gggbbb"))
		{
			if (r1.getWorldData().getStream_order().equals("gggbbb"))
			{
				return getCompareHits(r1, r2) * order;
			}
			else
			{
				return 1 * order;
			}
		}
		if (r2.getWorldData().getStream_order().equals("bbbggg"))
		{
			if (r1.getWorldData().getStream_order().equals("gggbbb"))
			{
				return -1 * order;
			}
			else if (r1.getWorldData().getStream_order().equals("bbbggg"))
			{
				return getCompareHits(r1, r2) * order;
			}
			else
			{
				return 1 * order;
			}
		}
		return r1.getWorldData().getWorld_number() - r2.getWorldData().getWorld_number();
	}

	private int getCompareHits(WorldTableRow r1, WorldTableRow r2)
	{
		int c = r2.getWorldData().getHits() - r1.getWorldData().getHits();

		if (c == 0)
		{
			return r1.getWorldData().getWorld_number() - r2.getWorldData().getWorld_number();
		}
		else
		{
			return c;
		}
	}

	private int getCompareValue(WorldTableRow row1, WorldTableRow row2, Function<WorldTableRow, Comparable> compareByFn)
	{
		Ordering<Comparable> ordering = Ordering.natural();
		if (!ascendingOrder)
		{
			ordering = ordering.reverse();
		}
		ordering = ordering.nullsLast();
		return ordering.compare(compareByFn.apply(row1), compareByFn.apply(row2));
	}

	void addNoDataMessage()
	{
		final GridBagConstraints c = new GridBagConstraints();
		final String[] messages = new String[]
		{
//			" ",
//			"    Worlds have reset!",
//			" ",
//			"    Please help to gather data",
//			"    by hopping worlds :)",
			" ",
			"    There is currently an issue",
			"    displaying worlds in the plugin.",
			"    We are currently looking into a",
			"    fix, but for now, please visit",
			"    https://togcrowdsourcing.com/",
			"    for a list of current worlds."
		};

		for (String message : messages)
		{
			listContainer.add(new JLabel(message), c);
		}
	}

	void addGetErrorMessage()
	{
		final GridBagConstraints c = new GridBagConstraints();
		final String[] messages = new String[]
				{
						" ",
						"    Error getting world data",
						" ",
						"    Server may be down,",
						"    Please try again later.",
						"    Really sorry for the inconvenience :(",
						" ",
						" ",
						" ",
						"    If this continues, please consider",
						"    posting an issue on the github repo",
						"    and I will try to resolve this asap:",
						" ",
						"    https://github.com/jcarbelbide",
						"    /tog-crowdsourcing-server/issues",
				};

		for (String message : messages)
		{
			listContainer.add(new JLabel(message), c);
		}
	}

	void populate(List<WorldData> worldDataList, ToGCrowdsourcingConfig config, @Nullable EnumComposition worldLocations)
	{
		rows.clear();

		if (worldHopper.isGetError())
		{
			listContainer.removeAll();
			addGetErrorMessage();
			return;
		}

		if (worldDataList.size() == 0)
		{
			listContainer.removeAll();
			addNoDataMessage();
			return;
		}

		WorldResult worldResult = worldHopper.getWorldService().getWorlds();
		if (worldResult == null) { return; }

		for (int i = 0; i < worldDataList.size(); i++)
		{
			WorldData worldData = worldDataList.get(i);
			World world = worldResult.findWorld(worldData.getWorld_number());
			if (world == null) { return; }

			if (shouldWorldBeSkipped(world, worldData, config)) { continue; }

			boolean isCurrentWorld = worldData.getWorld_number() == worldHopper.getCurrentWorld() && worldHopper.getLastWorld() != 0;

			rows.add(buildRow(
					worldData,
					i % 2 == 0,
					isCurrentWorld,
					worldLocations != null ? worldLocations.getIntValue(world.getId()) : -1)
			);
		}
		updateList();
	}

	private boolean shouldWorldBeSkipped(World world, WorldData worldData, ToGCrowdsourcingConfig config) {
		if (world == null) { return true; }
		if (world.getTypes().contains(WorldType.PVP) && config.hidePVPWorlds()) { return true; }		// Hide PVP Worlds if config item set.
		if (world.getTypes().contains(WorldType.HIGH_RISK) && config.hideHighRiskWorlds()) { return true; }
		if (!world.getTypes().contains(WorldType.MEMBERS)) { return true; }
		if (world.getTypes().contains(WorldType.NOSAVE_MODE)) { return true; }
		if (world.getTypes().contains(WorldType.DEADMAN)) { return true; }
		if (world.getTypes().contains(WorldType.TOURNAMENT)) { return true; }
		if (world.getTypes().contains(WorldType.SEASONAL)) { return true; }

		if (config.onlyShowOptimalWorlds() && !(worldData.getStream_order().equals("gggbbb") || worldData.getStream_order().equals("bbbggg"))) { return true; }

		return false;
	}

	private void orderBy(WorldOrder order)
	{
		worldHeader.highlight(false, ascendingOrder);
		hitsHeader.highlight(false, ascendingOrder);
		activityHeader.highlight(false, ascendingOrder);

		switch (order)
		{
			case WORLD:
				worldHeader.highlight(true, ascendingOrder);
				break;
			case HITS:
				hitsHeader.highlight(true, ascendingOrder);
				break;
			case STREAM_ORDER:
				activityHeader.highlight(true, ascendingOrder);
				break;
		}

		orderIndex = order;
		updateList();
	}

	/**
	 * Builds the entire table header.
	 */
	private JPanel buildHeader()
	{
		JPanel header = new JPanel(new BorderLayout());
		JPanel leftSide = new JPanel(new BorderLayout());
		JPanel rightSide = new JPanel(new BorderLayout());

		worldHeader = new WorldTableHeader("World", orderIndex == WorldOrder.WORLD, ascendingOrder, worldHopper::refresh);
		worldHeader.setPreferredSize(new Dimension(WORLD_COLUMN_WIDTH, 0));
		worldHeader.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent mouseEvent)
			{
				if (SwingUtilities.isRightMouseButton(mouseEvent))
				{
					return;
				}
				ascendingOrder = orderIndex != WorldOrder.WORLD || !ascendingOrder;
				orderBy(WorldOrder.WORLD);
			}
		});

		hitsHeader = new WorldTableHeader("Hits", orderIndex == WorldOrder.HITS, ascendingOrder, worldHopper::refresh);
		hitsHeader.setPreferredSize(new Dimension(HITS_COLUMN_WIDTH, 0));
		hitsHeader.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent mouseEvent)
			{
				if (SwingUtilities.isRightMouseButton(mouseEvent))
				{
					return;
				}
				ascendingOrder = orderIndex != WorldOrder.HITS || !ascendingOrder;
				orderBy(WorldOrder.HITS);
			}
		});

		activityHeader = new WorldTableHeader("Stream Order", orderIndex == WorldOrder.STREAM_ORDER, ascendingOrder, worldHopper::refresh);
		activityHeader.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent mouseEvent)
			{
				if (SwingUtilities.isRightMouseButton(mouseEvent))
				{
					return;
				}
				ascendingOrder = orderIndex != WorldOrder.STREAM_ORDER || !ascendingOrder;
				orderBy(WorldOrder.STREAM_ORDER);
			}
		});

		leftSide.add(worldHeader, BorderLayout.WEST);
		leftSide.add(hitsHeader, BorderLayout.CENTER);

		rightSide.add(activityHeader, BorderLayout.CENTER);

		header.add(leftSide, BorderLayout.WEST);
		header.add(rightSide, BorderLayout.CENTER);

		return header;
	}

	/**
	 * Builds a table row, that displays the world's information.
	 */
	private WorldTableRow buildRow(WorldData worldData, boolean stripe, boolean current, int worldLocation)
	{
		World world = worldHopper.getWorldService().getWorlds().findWorld(worldData.getWorld_number());
		WorldTableRow row = new WorldTableRow(
				world, worldData, current,
			worldHopper::hopTo,
			worldLocation
		);
		row.setBackground(stripe ? ODD_ROW : ColorScheme.DARK_GRAY_COLOR);
		return row;
	}

	/**
	 * Enumerates the multiple ordering options for the world list.
	 */
	private enum WorldOrder
	{
		WORLD,
		HITS,
		STREAM_ORDER
	}
}
