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

import lombok.Getter;
import net.runelite.api.DecorativeObject;
import net.runelite.api.ObjectID;
import net.runelite.client.util.ColorUtil;

import java.awt.Color;
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
