package com.togcrowdsourcing;

import javax.inject.Inject;

import com.togcrowdsourcing.ui.WorldHopper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.DecorativeObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.ui.overlay.OverlayManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;

@Slf4j
public class StreamOrderDetector
{
    private static final int TOG_REGION = 12948;
    private static final int STREAM_SHORT_INTERVAL = 600;
    private static final int STREAM_LONG_INTERVAL = 6600;
    private static final double STREAM_INTERVAL_TOLERANCE = 0.2;
    public static final int NUMBER_OF_TEAR_STREAMS = 6;

    @Getter
    private boolean dataValid;

    private Instant lastSpawnInstant = Instant.now();

    @Inject
    private Client client;

    private ToGCrowdsourcingConfig config;

    private WorldHopper worldHopper;

    @Inject
    private ToGCrowdsourcingOverlay overlay;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private CrowdsourcingManager crowdsourcingManager;

    @Getter
    private ArrayList<TearStream> streamList = new ArrayList<>();

    @Inject
    private StreamOrderDetector()
    {

    }

    public void startUpStreamOrderDetector(ToGCrowdsourcingConfig config, WorldHopper worldHopper)
    {
        log.info("ToG Started");
        dataValid = false;
        this.config = config;
        this.worldHopper = worldHopper;
        overlayManager.add(overlay);
    }

    public void shutDownStreamOrderDetector()
    {
        log.info("ToG Started");
        overlayManager.remove(overlay);
        streamList.clear();
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        switch (event.getGameState())
        {
            case LOADING:
            case LOGIN_SCREEN:
            case HOPPING:
        }
        dataValid = false;
        streamList.clear();
    }

    @Subscribe
    public void onDecorativeObjectSpawned(DecorativeObjectSpawned event)
    {
        if (dataValid) { return; }
        if (client.getGameState() != GameState.LOGGED_IN) { return; }
        if (client.getLocalPlayer().getWorldLocation().getRegionID() != TOG_REGION) return;

        // Keep track of the time between the last stream and the current stream. If above a certain threshold and data is not yet valid, then clear working list

        DecorativeObject object = event.getDecorativeObject();

        if (object.getId() == ObjectID.BLUE_TEARS || object.getId() == ObjectID.BLUE_TEARS_6665 ||
                object.getId() == ObjectID.GREEN_TEARS || object.getId() == ObjectID.GREEN_TEARS_6666)
        {
            Instant spawnInstant = Instant.now();
            long timeSinceLastSpawn = ChronoUnit.MILLIS.between(lastSpawnInstant, spawnInstant);
            TearStream tearStream = new TearStream(object, timeSinceLastSpawn, spawnInstant);
            lastSpawnInstant = spawnInstant;

//            System.out.println("Color: " + overlay.streamToString(tearStream) + " Time since last spawn: " + timeSinceLastSpawn);
            // Do not want to update unless it has been around 1 tick. Otherwise, it could be the client loading streams in at a random time and calling this function 6 times in quick succession.
            // If the above comment is true, then clear the list and return. It is potentially bad data.
            // TODO: Implement this function above. In addition, look into taking out lastInstant. See if we can just use the last element in the array.
            if (timeSinceLastSpawn < STREAM_SHORT_INTERVAL * (1 - STREAM_INTERVAL_TOLERANCE))
            {
                streamList.clear();
//                System.out.println("Stream Cleared from onDecorativeObjectSpawned");
                return;
            }

            streamList.add(tearStream);
//            System.out.println(streamListToStringForAPI(overlay.padWithNull(streamList)));
        }


    }

    @Subscribe
    private void onGameTick(GameTick event)
    {
        if (dataValid) { return; }
        verifyDataIsValid();
    }

    private void verifyDataIsValid()
    {
        // If stream list has 6 in a row with short delays, then we know its valid
        if (streamList.size() == 0) { return; }
        else if (streamList.size() > NUMBER_OF_TEAR_STREAMS)
        {
            streamList.clear();
        }
        else if (streamList.size() < NUMBER_OF_TEAR_STREAMS)
        {
            // TODO: May want to consider ignoring first index. Only time it would matter is if somehow the first index
            //  is less than a game tick since last spawn, which I think is unlikely if not impossible to occur. Could
            //  only occur during a hop. Even then, can it occur?
            // If it has been longer than STREAM_SHORT_INTERVAL ms, then clear the list.
            // However, we do not want to clear the list if its too short either. But the problem is, this function gets
            // called almost right after adding a stream.
            // We need two conditions:
            // 		1. Clear if the time between last stream is less than SHORT interval.
            // 		2. Clear if it has been longer than SHORT since the last time we added a stream.
            Instant currentInstant = Instant.now();
            Instant lastSpawnInstant = streamList.get(streamList.size() - 1).getSpawnInstant();
            long timeSinceLastSpawn = ChronoUnit.MILLIS.between(lastSpawnInstant, currentInstant);
            long timeBetweenSpawnForLatestTear = streamList.get(streamList.size() - 1).getTimeSinceLastSpawn();
//            System.out.println("5 or under streams");
            if (timeBetweenSpawnForLatestTear < STREAM_SHORT_INTERVAL * (1 - STREAM_INTERVAL_TOLERANCE) ||      // Self explanatory, if the time between spawns stored in the latest tear is less than the short interval, clear. Should never happen.
                    timeSinceLastSpawn > STREAM_SHORT_INTERVAL * (1 - STREAM_INTERVAL_TOLERANCE))               // If the time between now and the last spawn was greater than the short interval (600ms), clear the list. Streams need to spawn within 600ms of each other.
            {
                streamList.clear();
//                System.out.println("Stream Cleared from verifyDataValid. Since: " + timeSinceLastSpawn + " Between: " + timeBetweenSpawnForLatestTear);
            }
        }
        else if (streamList.size() == NUMBER_OF_TEAR_STREAMS)
        {
            // If it manages to store 6 streams with all of our checks, the data is always valid. Every hop, the stream
            // list is cleared, so no worries about getting a stream from a previous world. If it only reaches 5 because
            // we hopped in the middle of the streams changing, then the whole list will be cleared from the previous if
            // statement (and onGameStateChanged()).
//            System.out.println("Data IS valid!");
            dataValid = true;
            submitToAPI();
            return;
        }
    }

    public ArrayList<String> streamListToStringArray(ArrayList<TearStream> tearStreamArrayList)
    {
        ArrayList<String> streamListStringArray = new ArrayList<>();

        for (TearStream tearStream : tearStreamArrayList)
        {
            streamListStringArray.add(overlay.streamToString(tearStream));
        }

        return streamListStringArray;
    }

    private String streamListToStringForAPI(ArrayList<TearStream> tearStreamArrayList)
    {
        StringBuilder stringBuilder = new StringBuilder();

        for (TearStream tearStream : tearStreamArrayList)
        {
            stringBuilder.append(streamToStringForAPI(tearStream));
        }

        return stringBuilder.toString();
    }

    private String streamToStringForAPI(TearStream object)
    {
        DecorativeObject tearStreamObject = object.getTearStreamObject();
        if (tearStreamObject == null)
        {
            return "-";
        }
        if (	tearStreamObject.getId() == ObjectID.BLUE_TEARS ||
                tearStreamObject.getId() == ObjectID.BLUE_TEARS_6665)
        {
            return "b";
        }
        if (	tearStreamObject.getId() == ObjectID.GREEN_TEARS ||
                tearStreamObject.getId() == ObjectID.GREEN_TEARS_6666)
        {
            return "g";
        }
        else { return "Error"; }
    }

    private void submitToAPI()
    {
        synchronized (this)
        {
            int currentWorld = client.getWorld();
            String streamOrder = streamListToStringForAPI(streamList);
            WorldData worldData = new WorldData(currentWorld, streamOrder, 1);
            crowdsourcingManager.submitToAPI(worldData);
            crowdsourcingManager.makeGetRequest(worldHopper);
        }
    }
}
