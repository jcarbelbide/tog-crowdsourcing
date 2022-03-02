/*
 * Copyright (c) 2018, Tomas Slusny <slusnucky@gmail.com>
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

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import net.runelite.client.ui.overlay.components.ComponentConstants;
import net.runelite.client.ui.overlay.components.LayoutableRenderableEntity;
import net.runelite.client.ui.overlay.components.TextComponent;
import net.runelite.client.util.Text;

import java.awt.*;

@Setter
@Builder
public class TripleLineComponent implements LayoutableRenderableEntity
{
	private String left;
	private String right;
	private String middle;

	@Builder.Default
	private Color leftColor = Color.WHITE;

	@Builder.Default
	private Color rightColor = Color.WHITE;

	@Builder.Default
	private Color middleColor = Color.WHITE;

	private Font leftFont;

	private Font rightFont;

	private Font middleFont;

	@Builder.Default
	private Point preferredLocation = new Point();

	@Builder.Default
	private Dimension preferredSize = new Dimension(ComponentConstants.STANDARD_WIDTH, 0);

	@Builder.Default
	@Getter
	private final Rectangle bounds = new Rectangle();

	@Override
	public Dimension render(Graphics2D graphics)
	{
		// Prevent NPEs
		final String left = MoreObjects.firstNonNull(this.left, "");
		final String right = MoreObjects.firstNonNull(this.right, "");
		final String middle = MoreObjects.firstNonNull(this.middle, "");

		final Font leftFont = MoreObjects.firstNonNull(this.leftFont, graphics.getFont());
		final Font rightFont = MoreObjects.firstNonNull(this.rightFont, graphics.getFont());
		final Font middleFont = MoreObjects.firstNonNull(this.middleFont, graphics.getFont());

		final FontMetrics lfm = graphics.getFontMetrics(leftFont);
		final FontMetrics rfm = graphics.getFontMetrics(rightFont);
		final FontMetrics mfm = graphics.getFontMetrics(middleFont);

		final int fmHeight = Math.max(lfm.getHeight(), Math.max(rfm.getHeight(), mfm.getHeight()));
		final int baseX = preferredLocation.x;
		final int baseY = preferredLocation.y + fmHeight;
		int x = baseX;
		int y = baseY;
		final int leftFullWidth = getLineWidth(left, lfm);
		final int rightFullWidth = getLineWidth(right, rfm);
		final int middleFullWidth = getLineWidth(middle, rfm);
		final TextComponent textComponent = new TextComponent();

		// TODO: Change below if statement
		if (preferredSize.width < leftFullWidth + rightFullWidth + middleFullWidth)
		{
			int leftSmallWidth = preferredSize.width;
			int rightSmallWidth = 0;
			int middleSmallWidth = 0;

			if (!Strings.isNullOrEmpty(right))
			{
				rightSmallWidth = (preferredSize.width / 3);
				leftSmallWidth -= rightSmallWidth;
			}

			if (!Strings.isNullOrEmpty(middle))
			{
				middleSmallWidth = (preferredSize.width / 3);
				leftSmallWidth -= middleSmallWidth;
			}

			final String[] leftSplitLines = lineBreakText(left, leftSmallWidth, lfm);
			final String[] rightSplitLines = lineBreakText(right, rightSmallWidth, rfm);
			final String[] middleSplitLines = lineBreakText(middle, middleSmallWidth, mfm);

			int lineCount = Math.max(leftSplitLines.length, Math.max(rightSplitLines.length, middleSplitLines.length));

			for (int i = 0; i < lineCount; i++)
			{
				if (i < leftSplitLines.length)
				{
					final String leftText = leftSplitLines[i];
					textComponent.setPosition(new Point(x, y));
					textComponent.setText(leftText);
					textComponent.setColor(leftColor);
					textComponent.setFont(leftFont);
					textComponent.render(graphics);
				}

				if (i < rightSplitLines.length)
				{
					final String rightText = rightSplitLines[i];
					textComponent.setPosition(new Point(x + preferredSize.width - getLineWidth(rightText, rfm), y));
					textComponent.setText(rightText);
					textComponent.setColor(rightColor);
					textComponent.setFont(rightFont);
					textComponent.render(graphics);
				}

				if (i < middleSplitLines.length)
				{
					final String middleText = middleSplitLines[i];
					textComponent.setPosition(new Point(x + (preferredSize.width - getLineWidth(middleText, mfm) / 2), y));
					textComponent.setText(middleText);
					textComponent.setColor(middleColor);
					textComponent.setFont(middleFont);
					textComponent.render(graphics);
				}

				y += fmHeight;
			}

			final Dimension dimension = new Dimension(preferredSize.width, y - baseY);
			bounds.setLocation(preferredLocation);
			bounds.setSize(dimension);
			return dimension;
		}

		if (!left.isEmpty())
		{
			textComponent.setPosition(new Point(x, y));
			textComponent.setText(left);
			textComponent.setColor(leftColor);
			textComponent.setFont(leftFont);
			textComponent.render(graphics);
		}

		if (!right.isEmpty())
		{
			textComponent.setPosition(new Point(x + preferredSize.width - rightFullWidth, y));
			textComponent.setText(right);
			textComponent.setColor(rightColor);
			textComponent.setFont(rightFont);
			textComponent.render(graphics);
		}

		if (!middle.isEmpty())
		{
			textComponent.setPosition(new Point(x + (preferredSize.width - middleFullWidth) / 2, y));
			textComponent.setText(middle);
			textComponent.setColor(middleColor);
			textComponent.setFont(middleFont);
			textComponent.render(graphics);
		}

		y += fmHeight;

		final Dimension dimension = new Dimension(preferredSize.width, y - baseY);
		bounds.setLocation(preferredLocation);
		bounds.setSize(dimension);
		return dimension;
	}

	private static int getLineWidth(final String line, final FontMetrics metrics)
	{
		return metrics.stringWidth(Text.removeTags(line));
	}

	private static String[] lineBreakText(String text, int maxWidth, FontMetrics metrics)
	{
		final String[] words = text.split(" ");

		if (words.length == 0)
		{
			return new String[0];
		}

		final StringBuilder wrapped = new StringBuilder(words[0]);
		int spaceLeft = maxWidth - metrics.stringWidth(wrapped.toString());

		for (int i = 1; i < words.length; i++)
		{
			final String word = words[i];
			final int wordLen = metrics.stringWidth(word);
			final int spaceWidth = metrics.stringWidth(" ");

			if (wordLen + spaceWidth > spaceLeft)
			{
				wrapped.append("\n").append(word);
				spaceLeft = maxWidth - wordLen;
			}
			else
			{
				wrapped.append(" ").append(word);
				spaceLeft -= spaceWidth + wordLen;
			}
		}

		return wrapped.toString().split("\n");
	}
}

