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
package com.togcrowdsourcing;

import net.runelite.api.DecorativeObject;
import net.runelite.api.ObjectID;
import net.runelite.api.SpriteID;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.components.ComponentOrientation;
import net.runelite.client.ui.overlay.components.ImageComponent;
import net.runelite.client.ui.overlay.components.SplitComponent;

import javax.inject.Inject;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.time.Instant;
import java.util.ArrayList;

import static com.togcrowdsourcing.StreamOrderDetector.NUMBER_OF_TEAR_STREAMS;


class ToGCrowdsourcingOverlay extends OverlayPanel
{
	private final StreamOrderDetector streamOrderDetector;
	private final ToGCrowdsourcingConfig config;
	private static final int OVERLAY_SPLIT_GAP = 20;

	@Inject
	SpriteManager spriteManager;

	@Inject
	private ToGCrowdsourcingOverlay(StreamOrderDetector streamOrderDetector, ToGCrowdsourcingConfig config)
	{
		this.config = config;
		this.streamOrderDetector = streamOrderDetector;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		ArrayList<TearStream> paddedList = padWithNull(streamOrderDetector.getStreamList());

		TripleLineComponent topLine = TripleLineComponent.builder()
				.left(streamToString(paddedList.get(0)))
				.middle(streamToString(paddedList.get(1)))
				.right(streamToString(paddedList.get(2)))
				.leftColor(paddedList.get(0).getColor())
				.middleColor(paddedList.get(1).getColor())
				.rightColor(paddedList.get(2).getColor())
				.build();

		TripleLineComponent bottomLine = TripleLineComponent.builder()
				.left(streamToString(paddedList.get(3)))
				.middle(streamToString(paddedList.get(4)))
				.right(streamToString(paddedList.get(5)))
				.leftColor(paddedList.get(3).getColor())
				.middleColor(paddedList.get(4).getColor())
				.rightColor(paddedList.get(5).getColor())
				.build();

		SplitComponent tearColorsSplitComponent = SplitComponent.builder()
				.first(topLine)
				.second(bottomLine)
				.orientation(ComponentOrientation.VERTICAL)
				.build();


		ImageComponent checkboxImageComponent = new ImageComponent(getCheckBoxImage());
		SplitComponent tearColorsAndCheckboxSplitComponent = SplitComponent.builder()
				.first(tearColorsSplitComponent)
				.second(checkboxImageComponent)
				.orientation(ComponentOrientation.HORIZONTAL)
				.gap(new Point(OVERLAY_SPLIT_GAP, 0))
				.build();

		panelComponent.getChildren().add(
				tearColorsAndCheckboxSplitComponent
		);

//		panelComponent.getChildren().add(
//				topLine);
//
//		panelComponent.getChildren().add(
//				bottomLine);

		return super.render(graphics);
	}

	public String streamToString(TearStream object)
	{
		DecorativeObject tearStreamObject = object.getTearStreamObject();
		if (tearStreamObject == null)
		{
			return "-";
		}
		if (	tearStreamObject.getId() == ObjectID.BLUE_TEARS ||
				tearStreamObject.getId() == ObjectID.BLUE_TEARS_6665)
		{
			return "Blue";
		}
		if (	tearStreamObject.getId() == ObjectID.GREEN_TEARS ||
				tearStreamObject.getId() == ObjectID.GREEN_TEARS_6666)
		{
			return "Green";
		}
		else { return "Error"; }
	}

	public ArrayList<TearStream> padWithNull(ArrayList<TearStream> streamList)
	{
		ArrayList<TearStream> paddedList = new ArrayList<>();
		for (int i = 0; i < streamList.size(); i++)
		{
			paddedList.add(streamList.get(i));
		}

		for (int i = 0; i < NUMBER_OF_TEAR_STREAMS - streamList.size(); i++) {
			paddedList.add(new TearStream(null, 0, Instant.now()));
		}

		return paddedList;
	}

	private BufferedImage getCheckBoxImage()
	{
		int spriteID = streamOrderDetector.isDataValid() ? SpriteID.OPTIONS_ROUND_CHECK_BOX_CHECKED : SpriteID.OPTIONS_ROUND_CHECK_BOX_CROSSED;
		return spriteManager.getSprite(spriteID, 0);
	}
}