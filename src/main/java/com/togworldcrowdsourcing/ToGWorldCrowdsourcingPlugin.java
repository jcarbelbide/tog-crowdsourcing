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
import net.runelite.client.ui.overlay.OverlayManager;

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

	@Inject
	private ToGWorldCrowdsourcingOverlay overlay;

	@Inject
	private OverlayManager overlayManager;

	@Getter
	private ArrayList<DecorativeObject> streamList = new ArrayList<>();

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
				streamList.clear();
			case HOPPING:
				dataValid = false;
				streamList.clear();
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
			long timeSinceLastSpawn = ChronoUnit.MILLIS.between(lastInstant, Instant.now());
			if (timeSinceLastSpawn > STREAM_LONG_INTERVAL * (1 - STREAM_INTERVAL_TOLERANCE))
			{
				System.out.println(streamListToString(streamList));
				streamList.clear();
			}
			streamList.add(object);
			System.out.println("Color: " + overlay.streamToString(object) + " Time since last instant: " + timeSinceLastSpawn);
			lastInstant = Instant.now();
			System.out.println(streamListToString(overlay.padWithNull(streamList)));
		}


	}

	private void verifyData()
	{
		if (streamList.size() > 6)
		{
			streamList.clear();
		}
		// If stream list has 6 in a row with short delays, then we know its valid
		// if stream list has 6, its valid if first has delay of around long, and rest are short
		// TODO: Create function to call to verify if this data is valid to send.
	}

	public ArrayList<String> streamListToStringArray(ArrayList<DecorativeObject> objectArrayList)
	{
		ArrayList<String> streamListStringArray = new ArrayList<>();

		for (DecorativeObject object : objectArrayList)
		{
			streamListStringArray.add(overlay.streamToString(object));
		}

		return streamListStringArray;
	}

	public String streamListToString(ArrayList<DecorativeObject> objectArrayList)
	{
		StringBuilder stringBuilder = new StringBuilder();

		for (DecorativeObject object : objectArrayList)
		{
			stringBuilder.append(overlay.streamToString(object) + " ");
		}

		return stringBuilder.toString();
	}

	@Provides
	ToGWorldCrowdsourcingConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ToGWorldCrowdsourcingConfig.class);
	}
}
