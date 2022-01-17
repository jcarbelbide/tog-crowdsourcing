package net.runelite.client.plugins.togcrowdsourcing.src.main.java.com.togcrowdsourcing;

import com.google.inject.Provides;
import javax.inject.Inject;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.DecorativeObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;

@Slf4j
@PluginDescriptor(
	name = "Tears of Guthix Crowdsourcing"
)
public class ToGCrowdsourcingPlugin extends Plugin
{
	private static final int TOG_REGION = 12948;
	private static final int STREAM_SHORT_INTERVAL = 600;
	private static final int STREAM_LONG_INTERVAL = 6600;
	private static final double STREAM_INTERVAL_TOLERANCE = 0.2;
	public static final int NUMBER_OF_TEAR_STREAMS = 6;

	@Getter
	private boolean dataValid;

	private Instant lastSpawnInstant = Instant.now();

	private GameTick gameTick;

	@Inject
	private Client client;

	@Inject
	private ToGCrowdsourcingConfig config;

	@Inject
	private ToGCrowdsourcingOverlay overlay;

	@Inject
	private OverlayManager overlayManager;

	@Getter
	private ArrayList<TearStream> streamList = new ArrayList<>();

	@Override
	protected void startUp()
	{
		log.info("ToG Started");
		dataValid = false;
		overlayManager.add(overlay);
	}

	@Override
	protected void shutDown()
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

		// TODO: Create list of the streams --> blue, blue, blue, green, green, green, etc.
		// Keep track of the time between the last stream and the current stream. If above a certain threshold and data is not yet valid, then

		DecorativeObject object = event.getDecorativeObject();

		if (object.getId() == ObjectID.BLUE_TEARS || object.getId() == ObjectID.BLUE_TEARS_6665 ||
				object.getId() == ObjectID.GREEN_TEARS || object.getId() == ObjectID.GREEN_TEARS_6666)
		{
			Instant spawnInstant = Instant.now();
			long timeSinceLastSpawn = ChronoUnit.MILLIS.between(lastSpawnInstant, spawnInstant);
			TearStream tearStream = new TearStream(object, timeSinceLastSpawn, spawnInstant);
			lastSpawnInstant = spawnInstant;

			System.out.println("Color: " + overlay.streamToString(tearStream) + " Time since last spawn: " + timeSinceLastSpawn);
			// Do not want to update unless it has been around 1 tick. Otherwise, it could be the client loading streams in at a random time and calling this function 6 times in quick succession.
			// If the above comment is true, then clear the list and return. It is potentially bad data.
			// TODO: Implement this function above. In addition, look into taking out lastInstant. See if we can just use the last element in the array.
			if (timeSinceLastSpawn < STREAM_SHORT_INTERVAL * (1 - STREAM_INTERVAL_TOLERANCE))
			{
				streamList.clear();
				System.out.println("Stream Cleared from onDecorativeObjectSpawned");
				return;
			}

//			if (timeSinceLastSpawn > STREAM_LONG_INTERVAL * (1 - STREAM_INTERVAL_TOLERANCE))
//			{
//				System.out.println(streamListToString(streamList));
//				streamList.clear();
//			}

			streamList.add(tearStream);
			System.out.println(streamListToString(overlay.padWithNull(streamList)));
		}


	}

	@Subscribe
	private void onGameTick(GameTick event)
	{
		// fix after the list is 6, it will add one more to the list to make it 7, then clear, so the list wil be 5
		if (dataValid) { return; }
		verifyDataIsValid();
	}

	private void verifyDataIsValid()
	{
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
//			if (timeSinceLastSpawn > STREAM_SHORT_INTERVAL * (1 - STREAM_INTERVAL_TOLERANCE))
			// (Math.abs(timeSinceLastSpawn - STREAM_SHORT_INTERVAL) > Math.abs(STREAM_SHORT_INTERVAL * STREAM_INTERVAL_TOLERANCE)
			System.out.println("5 or under streams");
			if (timeBetweenSpawnForLatestTear < STREAM_SHORT_INTERVAL * (1 - STREAM_INTERVAL_TOLERANCE) ||
					timeSinceLastSpawn > STREAM_SHORT_INTERVAL * (1 - STREAM_INTERVAL_TOLERANCE))
			{
				streamList.clear();
				System.out.println("Stream Cleared from verifyDataValid. Since: " + timeSinceLastSpawn + " Between: " + timeBetweenSpawnForLatestTear);
			}
		}
		else if (streamList.size() == NUMBER_OF_TEAR_STREAMS)
		{
//			// Tear 0 must be LONG
//			if (streamList.get(0).getTimeSinceLastSpawn() < STREAM_LONG_INTERVAL * (1 - STREAM_INTERVAL_TOLERANCE))
//			{
//				System.out.println("Data NOT valid (First Stream not Valid)");
//				streamList.clear();
//				return;
//			}
//			// Tear 1-5 must be SHORT
//			for (int i = 1; i < NUMBER_OF_TEAR_STREAMS; i++)
//			{
//				if (Math.abs(streamList.get(i).getTimeSinceLastSpawn() - STREAM_SHORT_INTERVAL) > Math.abs(STREAM_SHORT_INTERVAL * STREAM_INTERVAL_TOLERANCE))
//				{
//					System.out.println("Data NOT valid (1-5 not valid)");
//					streamList.clear();
//					return;
//				}
//			}
			// I think if it manages to store 6 streams, the data is always valid. Every hop, the stream list is cleared,
			// so no worries about getting a stream from a previous world. If it only reaches 5 because we hopped in the
			// middle of the streams changing, then the whole list will be cleared from the previous if statement.
			System.out.println("Data IS valid!");
			dataValid = true;
			return;
		}
		// If stream list has 6 in a row with short delays, then we know its valid
		// if stream list has 6, its valid if first has delay of around long, and rest are short
		// TODO: Create function to call to verify if this data is valid to send.
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

	public String streamListToString(ArrayList<TearStream> tearStreamArrayList)
	{
		StringBuilder stringBuilder = new StringBuilder();

		for (TearStream tearStream : tearStreamArrayList)
		{
			stringBuilder.append(overlay.streamToString(tearStream) + " ");
		}

		return stringBuilder.toString();
	}

	@Provides
	ToGCrowdsourcingConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ToGCrowdsourcingConfig.class);
	}
}