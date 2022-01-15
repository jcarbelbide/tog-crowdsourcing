package net.runelite.client.plugins.togworldcrowdsourcing.src.main.java.com.togworldcrowdsourcing;

import lombok.Getter;
import net.runelite.api.DecorativeObject;
import net.runelite.api.ObjectID;
import net.runelite.client.util.ColorUtil;

import java.awt.*;
import java.time.Instant;

public class TearStream
{
    private final Color BLUE_TEARS_COLOR = ColorUtil.colorWithAlpha(Color.CYAN, 100);
    private final Color GREEN_TEARS_COLOR = ColorUtil.colorWithAlpha(Color.GREEN, 100);

    @Getter
    private DecorativeObject tearStreamObject;

    @Getter
    private long timeSinceLastSpawn;

    @Getter
    private Color color;

    @Getter
    private Instant spawnInstant;

    TearStream(DecorativeObject tearStreamObject, long timeSinceLastChange, Instant spawnInstant)
    {
        this.tearStreamObject = tearStreamObject;
        this.timeSinceLastSpawn = timeSinceLastChange;
        this.spawnInstant = spawnInstant;
        this.color = determineColor(tearStreamObject);
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
}
