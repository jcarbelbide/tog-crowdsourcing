/*
 * Copyright (c) 2018, Infinitay <https://github.com/Infinitay>
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
package net.runelite.client.plugins.togworldcrowdsourcing.src.main.java.com.togworldcrowdsourcing;

import net.runelite.api.DecorativeObject;
import net.runelite.api.ObjectID;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.util.ColorUtil;

import javax.inject.Inject;
import java.awt.*;
import java.util.ArrayList;

class ToGWorldCrowdsourcingOverlay extends OverlayPanel
{
	private final ToGWorldCrowdsourcingPlugin plugin;
	private final ToGWorldCrowdsourcingConfig config;

	private final Color BLUE_TEARS_COLOR = ColorUtil.colorWithAlpha(Color.CYAN, 100);
	private final Color GREEN_TEARS_COLOR = ColorUtil.colorWithAlpha(Color.GREEN, 100);

	@Inject
	private ToGWorldCrowdsourcingOverlay(ToGWorldCrowdsourcingPlugin plugin, ToGWorldCrowdsourcingConfig config)
	{
		this.config = config;
		this.plugin = plugin;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		ArrayList<DecorativeObject> paddedList = padWithNull(plugin.getStreamList());

		TripleLineComponent topLine = TripleLineComponent.builder()
				.left(streamToString(paddedList.get(0)))
				.middle(streamToString(paddedList.get(1)))
				.right(streamToString(paddedList.get(2)))
				.leftColor(determineColor(paddedList.get(0)))
				.middleColor(determineColor(paddedList.get(1)))
				.rightColor(determineColor(paddedList.get(2)))
				.build();

		TripleLineComponent bottomLine = TripleLineComponent.builder()
				.left(streamToString(paddedList.get(3)))
				.middle(streamToString(paddedList.get(4)))
				.right(streamToString(paddedList.get(5)))
				.leftColor(determineColor(paddedList.get(3)))
				.middleColor(determineColor(paddedList.get(4)))
				.rightColor(determineColor(paddedList.get(5)))
				.build();

		panelComponent.getChildren().add(
				topLine);

		panelComponent.getChildren().add(
				bottomLine);

		return super.render(graphics);
	}

	private Color determineColor(DecorativeObject object) {
		if (object == null)
		{
			return Color.RED;
		}
		if (	object.getId() == ObjectID.BLUE_TEARS ||
				object.getId() == ObjectID.BLUE_TEARS_6665)
		{
			return BLUE_TEARS_COLOR;
		}
		if (	object.getId() == ObjectID.GREEN_TEARS ||
				object.getId() == ObjectID.GREEN_TEARS_6666)
		{
		return GREEN_TEARS_COLOR;
		}
		else { return Color.RED; }
	}

	public String streamToString(DecorativeObject object)
	{
		if (object == null)
		{
			return "-";
		}
		if (	object.getId() == ObjectID.BLUE_TEARS ||
				object.getId() == ObjectID.BLUE_TEARS_6665)
		{
			return "Blue";
		}
		if (	object.getId() == ObjectID.GREEN_TEARS ||
				object.getId() == ObjectID.GREEN_TEARS_6666)
		{
			return "Green";
		}
		else { return "Error"; }
	}

	public ArrayList<DecorativeObject> padWithNull(ArrayList<DecorativeObject> streamList)
	{
		ArrayList<DecorativeObject> paddedList = new ArrayList<>();
		for (int i = 0; i < streamList.size(); i++)
		{
			paddedList.add(streamList.get(i));
		}

		for (int i = 0; i < 6 - streamList.size(); i++) {
			paddedList.add(null);
		}

		return paddedList;
	}
}