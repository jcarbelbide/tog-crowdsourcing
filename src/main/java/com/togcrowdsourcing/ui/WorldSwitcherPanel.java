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
package net.runelite.client.plugins.togcrowdsourcing.src.main.java.com.togcrowdsourcing.ui;

import com.google.common.collect.Ordering;
import net.runelite.client.plugins.togcrowdsourcing.src.main.java.com.togcrowdsourcing.WorldData;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.PluginPanel;
import net.runelite.http.api.worlds.World;
import net.runelite.http.api.worlds.WorldResult;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

class WorldSwitcherPanel extends PluginPanel
{
	private static final Color ODD_ROW = new Color(44, 44, 44);

	private static final int WORLD_COLUMN_WIDTH = 60;
	private static final int PLAYERS_COLUMN_WIDTH = 40;

	private final JPanel listContainer = new JPanel();

	private WorldTableHeader worldHeader;
	private WorldTableHeader playersHeader;
	private WorldTableHeader activityHeader;

	private WorldOrder orderIndex = WorldOrder.WORLD;
	private boolean ascendingOrder = true;

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

	void updateListData(Map<Integer, Integer> worldData)
	{
		for (WorldTableRow worldTableRow : rows)
		{
			World world = worldTableRow.getWorld();
			Integer playerCount = worldData.get(world.getId());
			if (playerCount != null)
			{
				worldTableRow.updateHitsCount(playerCount);
			}
		}

		// If the list is being ordered by player count, then it has to be re-painted
		// to properly display the new data
//		if (orderIndex == WorldOrder.HITS)
//		{
//			updateList();
//		}
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
					// Leave empty activity worlds on the bottom of the list
					return getCompareValue(r1, r2, row ->
					{
						String streamOrder = row.getWorldData().getStream_order();
						return !streamOrder.equals("-") ? streamOrder : null;
					});
				default:
					return 0;
			}
		});

		// TODO sort by stream order first.
//		rows.sort((r1, r2) ->
//		{
//			boolean b1 = plugin.isFavorite(r1.getWorld());
//			boolean b2 = plugin.isFavorite(r2.getWorld());
//			return Boolean.compare(b2, b1);
//		});

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

	void populate(List<WorldData> worldDataList)
	{
		rows.clear();

//		WorldResult worldResult = worldHopper.getWorldService().getWorlds();

		for (int i = 0; i < worldDataList.size(); i++)
		{
			WorldData worldData = worldDataList.get(i);
//			World world = worldResult.findWorld(worldData.getWorld_number());
			// Don't try to add world if the world doesn't exist
//			if (world == null)
//			{
//				continue;
//			}

			boolean isCurrentWorld = worldData.getWorld_number() == worldHopper.getCurrentWorld() && worldHopper.getLastWorld() != 0;

			rows.add(buildRow(worldData, i % 2 == 0, isCurrentWorld));
		}

		updateList();
	}

	private void orderBy(WorldOrder order)
	{
		worldHeader.highlight(false, ascendingOrder);
		playersHeader.highlight(false, ascendingOrder);
		activityHeader.highlight(false, ascendingOrder);

		switch (order)
		{
			case WORLD:
				worldHeader.highlight(true, ascendingOrder);
				break;
			case HITS:
				playersHeader.highlight(true, ascendingOrder);
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

		playersHeader = new WorldTableHeader("Hits", orderIndex == WorldOrder.HITS, ascendingOrder, worldHopper::refresh);
		playersHeader.setPreferredSize(new Dimension(PLAYERS_COLUMN_WIDTH, 0));
		playersHeader.addMouseListener(new MouseAdapter()
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
		leftSide.add(playersHeader, BorderLayout.CENTER);

		rightSide.add(activityHeader, BorderLayout.CENTER);

		header.add(leftSide, BorderLayout.WEST);
		header.add(rightSide, BorderLayout.CENTER);

		return header;
	}

	/**
	 * Builds a table row, that displays the world's information.
	 */
	private WorldTableRow buildRow(WorldData worldData, boolean stripe, boolean current)
	{
		World world = worldHopper.getWorldService().getWorlds().findWorld(worldData.getWorld_number());
		WorldTableRow row = new WorldTableRow(
				world, worldData, current,
			worldHopper::hopTo
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
		STREAM_ORDER		// TODO: Switch around with hits
	}
}
