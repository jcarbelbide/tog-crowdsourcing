// TODO add license

package com.togcrowdsourcing;

import lombok.Data;

@Data
public class WorldData
{
    private final int world_number;
    private final String stream_order;
    private final int hits;

    public WorldData(int world_number, String stream_order, int hits) {
        this.world_number = world_number;
        this.stream_order = stream_order;
        this.hits = hits;
    }
}
