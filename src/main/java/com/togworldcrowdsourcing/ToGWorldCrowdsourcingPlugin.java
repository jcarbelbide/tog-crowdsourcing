package net.runelite.client.plugins.togworldcrowdsourcing.src.main.java.com.togworldcrowdsourcing;

import com.google.inject.Provides;
import javax.inject.Inject;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.DecorativeObjectDespawned;
import net.runelite.api.events.DecorativeObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@PluginDescriptor(
	name = "Tears of Guthix Crowdsourcing"
)
public class ToGWorldCrowdsourcingPlugin extends Plugin
{
	private static final int TOG_REGION = 12948;
	private static final int STREAM_SHORT_INTERVAL = 600;
	private static final int STREAM_LONG_INTERVAL = 6600;
	private static final double STREAM_INTERVAL_TOLERANCE = 0.2;

	private boolean dataValid;

	private Instant lastInstant = Instant.now();

	@Inject
	private Client client;

	@Inject
	private ToGWorldCrowdsourcingConfig config;

	@Getter
	private final Map<DecorativeObject, Instant> streams = new HashMap<>();

	@Getter
	private ArrayList<DecorativeObject> streamList = new ArrayList<>();

	@Override
	protected void startUp()
	{
		log.info("ToG Started");
		dataValid = false;
	}

	@Override
	protected void shutDown()
	{
		log.info("ToG Started");
		streams.clear();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		switch (event.getGameState())
		{
			case LOADING:
			case LOGIN_SCREEN:
			case HOPPING:
				dataValid = false;
				streams.clear();
		}
	}

	@Subscribe
	public void onDecorativeObjectSpawned(DecorativeObjectSpawned event)
	{
		if (client.getLocalPlayer().getWorldLocation().getRegionID() != TOG_REGION) return;

		// TODO: Create list of the streams --> blue, blue, blue, green, green, green, etc.
		// Keep track of the time between the last stream and the current stream. If above a certain threshold and data is not yet valid, then

		DecorativeObject object = event.getDecorativeObject();

		if (object.getId() == ObjectID.BLUE_TEARS || object.getId() == ObjectID.BLUE_TEARS_6665 ||
				object.getId() == ObjectID.GREEN_TEARS || object.getId() == ObjectID.GREEN_TEARS_6666)
		{
//			streamList.add(object);
			long timeSinceLastSpawn = ChronoUnit.MILLIS.between(lastInstant, Instant.now());
			System.out.println("Color: " + streamToString(object) + " Time since last instant: " + timeSinceLastSpawn);
			lastInstant = Instant.now();
		}

	}

	private void verifyData()
	{
		// TODO: Create function to call to verify if this data is valid to send.
	}

	private String streamToString(DecorativeObject object)
	{
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

	@Subscribe
	public void onDecorativeObjectDespawned(DecorativeObjectDespawned event)
	{
		if (streams.isEmpty())
		{
			return;
		}

		DecorativeObject object = event.getDecorativeObject();
		streams.remove(object);
	}

	@Provides
	ToGWorldCrowdsourcingConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ToGWorldCrowdsourcingConfig.class);
	}
}
