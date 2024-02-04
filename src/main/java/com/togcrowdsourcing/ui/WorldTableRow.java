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

import com.togcrowdsourcing.WorldData;
import lombok.AccessLevel;
import lombok.Getter;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.ImageUtil;
import net.runelite.http.api.worlds.World;
import net.runelite.http.api.worlds.WorldRegion;
import net.runelite.http.api.worlds.WorldType;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;
import net.runelite.client.plugins.worldhopper.WorldHopperPlugin;

class WorldTableRow extends JPanel
{
	private static final ImageIcon FLAG_AUS;
	private static final ImageIcon FLAG_UK;
	private static final ImageIcon FLAG_US;
	private static final ImageIcon FLAG_US_EAST;
	private static final ImageIcon FLAG_US_WEST;
	private static final ImageIcon FLAG_GER;

	private static final int WORLD_COLUMN_WIDTH = WorldSwitcherPanel.getWORLD_COLUMN_WIDTH();
	private static final int HITS_COLUMN_WIDTH = WorldSwitcherPanel.getHITS_COLUMN_WIDTH();

	private static final Color CURRENT_WORLD = new Color(66, 227, 17);
	private static final Color DANGEROUS_WORLD = new Color(251, 62, 62);
	private static final Color TOURNAMENT_WORLD = new Color(79, 145, 255);
	private static final Color MEMBERS_WORLD = new Color(210, 193, 53);
	private static final Color FREE_WORLD = new Color(200, 200, 200);
	private static final Color SEASONAL_WORLD = new Color(133, 177, 178);
	private static final Color GGGBBB_WORLD = new Color(36, 195, 250);
	private static final Color BBBGGG_WORLD = new Color(128, 217, 255);
//	private static final Color BBBGGG_WORLD = new Color(187, 113, 255);

	static
	{
		FLAG_AUS = new ImageIcon(ImageUtil.loadImageResource(WorldHopperPlugin.class, "flag_aus.png"));
		FLAG_UK = new ImageIcon(ImageUtil.loadImageResource(WorldHopperPlugin.class, "flag_uk.png"));
		FLAG_US = new ImageIcon(ImageUtil.loadImageResource(WorldHopperPlugin.class, "flag_us.png"));
		FLAG_US_EAST = new ImageIcon(ImageUtil.loadImageResource(WorldHopperPlugin.class, "flag_us_east.png"));
		FLAG_US_WEST = new ImageIcon(ImageUtil.loadImageResource(WorldHopperPlugin.class, "flag_us_west.png"));
		FLAG_GER = new ImageIcon(ImageUtil.loadImageResource(WorldHopperPlugin.class, "flag_ger.png"));
	}

	private static final int LOCATION_US_WEST = -73;
	private static final int LOCATION_US_EAST = -42;

	private JLabel worldField;
	private JLabel hitsField;
	private JLabel streamOrderField;
	private final int worldLocation; // from enum WORLD_LOCATIONS

	@Getter
	private final World world;

	@Getter
	private final WorldData worldData;

	@Getter(AccessLevel.PACKAGE)
	private int updatedHitsCount;

	private Color lastBackground;

	WorldTableRow(World world, WorldData worldData, boolean current, Consumer<World> onSelect, int worldLocation)
	{
		this.world = world;
		this.worldData = worldData;
		this.updatedHitsCount = worldData.getHits();
		this.worldLocation = worldLocation;

		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(2, 0, 2, 0));

		addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent mouseEvent)
			{
				if (mouseEvent.getClickCount() == 2)
				{
					if (onSelect != null)
					{
						onSelect.accept(world);
					}
				}
			}

			@Override
			public void mousePressed(MouseEvent mouseEvent)
			{
				if (mouseEvent.getClickCount() == 2)
				{
					setBackground(getBackground().brighter());
				}
			}

			@Override
			public void mouseReleased(MouseEvent mouseEvent)
			{
				if (mouseEvent.getClickCount() == 2)
				{
					setBackground(getBackground().darker());
				}
			}

			@Override
			public void mouseEntered(MouseEvent mouseEvent)
			{
				WorldTableRow.this.lastBackground = getBackground();
				setBackground(getBackground().brighter());
			}

			@Override
			public void mouseExited(MouseEvent mouseEvent)
			{
				setBackground(lastBackground);
			}
		});

		final JPopupMenu popupMenu = new JPopupMenu();
		popupMenu.setBorder(new EmptyBorder(5, 5, 5, 5));

		setComponentPopupMenu(popupMenu);

		JPanel leftSide = new JPanel(new BorderLayout());
		JPanel rightSide = new JPanel(new BorderLayout());
		leftSide.setOpaque(false);
		rightSide.setOpaque(false);

		JPanel worldField = buildWorldField();
		worldField.setPreferredSize(new Dimension(WORLD_COLUMN_WIDTH, 0));
		worldField.setOpaque(false);

		JPanel hitsField = buildHitsField();
		hitsField.setPreferredSize(new Dimension(HITS_COLUMN_WIDTH, 0));
		hitsField.setOpaque(false);

		JPanel activityField = buildStreamOrderField();
		activityField.setBorder(new EmptyBorder(5, 5, 5, 5));
		activityField.setOpaque(false);

		recolour(current);

		leftSide.add(worldField, BorderLayout.WEST);
		leftSide.add(hitsField, BorderLayout.CENTER);
		rightSide.add(activityField, BorderLayout.CENTER);

		add(leftSide, BorderLayout.WEST);
		add(rightSide, BorderLayout.CENTER);
	}

	private static String hitsCountString(int hitsCount)
	{
		return hitsCount < 0 ? "OFF" : Integer.toString(hitsCount);
	}

	public void recolour(boolean current)
	{
		hitsField.setForeground(current ? CURRENT_WORLD : Color.WHITE);

		if (current)
		{
			streamOrderField.setForeground(CURRENT_WORLD);
			worldField.setForeground(CURRENT_WORLD);
			return;
		}
		else if (world.getTypes().contains(WorldType.PVP)
			|| world.getTypes().contains(WorldType.HIGH_RISK)
			|| world.getTypes().contains(WorldType.DEADMAN))
		{
			streamOrderField.setForeground(DANGEROUS_WORLD);
		}
		else if (world.getTypes().contains(WorldType.SEASONAL))
		{
			streamOrderField.setForeground(SEASONAL_WORLD);
		}
		else if (world.getTypes().contains(WorldType.NOSAVE_MODE))
		{
			streamOrderField.setForeground(TOURNAMENT_WORLD);
		}
		else if (worldData.getStream_order().equals("gggbbb"))
		{
			streamOrderField.setForeground(GGGBBB_WORLD);
		}
		else if (worldData.getStream_order().equals("bbbggg"))
		{
			streamOrderField.setForeground(BBBGGG_WORLD);
		}
		else
		{
			streamOrderField.setForeground(Color.WHITE);
		}

		worldField.setForeground(world.getTypes().contains(WorldType.MEMBERS) ? MEMBERS_WORLD : FREE_WORLD);
	}

	/**
	 * Builds the hits list field (containing the amount of hits logged in that world).
	 */
	private JPanel buildHitsField()
	{
		JPanel column = new JPanel(new BorderLayout());
		column.setBorder(new EmptyBorder(0, 5, 0, 5));

		hitsField = new JLabel(hitsCountString(worldData.getHits()));
		hitsField.setFont(FontManager.getRunescapeSmallFont());

		column.add(hitsField, BorderLayout.WEST);

		return column;
	}

	/**
	 * Builds the activity list field (containing that world's activity/theme).
	 */
	private JPanel buildStreamOrderField()
	{
		JPanel column = new JPanel(new BorderLayout());
		column.setBorder(new EmptyBorder(0, 5, 0, 5));

		String streamOrder = worldData.getStream_order();
		streamOrderField = new JLabel(streamOrder);
		streamOrderField.setFont(FontManager.getRunescapeSmallFont());
		if (streamOrder != null && streamOrder.length() > 16)
		{
			streamOrderField.setToolTipText(streamOrder);
			// Pass up events - https://stackoverflow.com/a/14932443
			streamOrderField.addMouseListener(new MouseAdapter()
			{
				@Override
				public void mouseClicked(MouseEvent e)
				{
					dispatchEvent(e);
				}

				@Override
				public void mousePressed(MouseEvent e)
				{
					dispatchEvent(e);
				}

				@Override
				public void mouseReleased(MouseEvent e)
				{
					dispatchEvent(e);
				}

				@Override
				public void mouseEntered(MouseEvent e)
				{
					dispatchEvent(e);
				}

				@Override
				public void mouseExited(MouseEvent e)
				{
					dispatchEvent(e);
				}
			});
		}

		column.add(streamOrderField, BorderLayout.WEST);

		return column;
	}

	/**
	 * Builds the world list field (containing the country's flag and the world index).
	 */
	private JPanel buildWorldField()
	{
		JPanel column = new JPanel(new BorderLayout(7, 0));
		column.setBorder(new EmptyBorder(0, 5, 0, 5));

		worldField = new JLabel(world.getId() + "");

		ImageIcon flagIcon = getFlag(world.getRegion(), worldLocation);
		if (flagIcon != null)
		{
			JLabel flag = new JLabel(flagIcon);
			column.add(flag, BorderLayout.WEST);
		}
		column.add(worldField, BorderLayout.CENTER);

		return column;
	}

	private static ImageIcon getFlag(WorldRegion region, int worldLocation)
	{
		if (region == null)
		{
			return null;
		}

		switch (region)
		{
			case UNITED_STATES_OF_AMERICA:
				switch (worldLocation)
				{
					case LOCATION_US_WEST:
						return FLAG_US_WEST;
					case LOCATION_US_EAST:
						return FLAG_US_EAST;
					default:
						return FLAG_US;
				}
			case UNITED_KINGDOM:
				return FLAG_UK;
			case AUSTRALIA:
				return FLAG_AUS;
			case GERMANY:
				return FLAG_GER;
			default:
				return null;
		}
	}
}
