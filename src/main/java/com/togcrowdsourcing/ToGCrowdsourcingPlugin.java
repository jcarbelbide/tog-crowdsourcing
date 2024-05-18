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

import javax.inject.Inject;

import com.google.inject.Provides;
import com.togcrowdsourcing.ui.WorldHopper;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.task.Schedule;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

@PluginDescriptor(
		name = "Tears of Guthix Crowdsourcing",
		description = "Crowdsource ToG stream orders to help players find the optimal world for the week."
)
public class ToGCrowdsourcingPlugin extends Plugin
{
	private static final int REFRESH_INTERVAL_ON_ERROR = 10;
	private int numFailedCallsToAPI = 0;

	@Inject
	private EventBus eventBus;

	@Inject
	private ToGCrowdsourcingConfig config;

	@Inject
	private StreamOrderDetector streamOrderDetector;

	@Inject
	private WorldHopper worldHopper;

	@Override
	protected void startUp() throws Exception
	{
		eventBus.register(streamOrderDetector);
		streamOrderDetector.startUpStreamOrderDetector(config, worldHopper);

		eventBus.register(worldHopper);
		worldHopper.startUpWorldHopper(config);
	}

	@Override
	protected void shutDown() throws Exception
	{
		eventBus.unregister(streamOrderDetector);
		streamOrderDetector.shutDownStreamOrderDetector();

		eventBus.unregister(worldHopper);
		worldHopper.shutDownWorldHopper();
	}

	@Schedule(
			period = REFRESH_INTERVAL_ON_ERROR,
			unit = ChronoUnit.SECONDS,
			asynchronous = true
	)
	public void refreshOnError()
	{
		if (!worldHopper.isGetError()) {
			System.out.print("\nno error, not refreshing");
			numFailedCallsToAPI = 0;
			return;
		}

		numFailedCallsToAPI++;

		if (!shouldRetry()) {
			return;
		}

		System.out.printf("\nretrying - numFailedCallsToAPI: %s", numFailedCallsToAPI);

		synchronized (worldHopper)
		{
			worldHopper.getCrowdsourcingManager().makeGetRequest(worldHopper);
			worldHopper.updateList();
		}
	}

	private boolean shouldRetry()
	{
		// Seconds passed between failed calls is numFailedCallsToAPI * 10
		// we can attempt to retry at the following intervals:
		// 10, 20, 30, 60, 120, 300, 300, 300, ..., 300
		ArrayList<Integer> retryIntervalWhitelist = new ArrayList<>(Arrays.asList(0, 1, 2, 3, 6, 12, 30));
		int maxRetryInterval = Collections.max(retryIntervalWhitelist);

		// If the number of current retries is in the whitelist, we should retry.
		if (retryIntervalWhitelist.contains(numFailedCallsToAPI)) {
			System.out.print("\nnum failed calls is in white list, retrying");
			return true;
		}

		// Cap out retry interval to the greatest one defined
		if (numFailedCallsToAPI % maxRetryInterval == 0) {
			System.out.print("\nnum failed calls has reached max, retrying");
			return true;
		}

		System.out.printf("\nshould not retry - numFailedCallsToAPI: %s, maxRetryInterval: %s, modulo: %b", numFailedCallsToAPI, maxRetryInterval, numFailedCallsToAPI % maxRetryInterval == 0);

		return false;
	}

	@Provides
	ToGCrowdsourcingConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ToGCrowdsourcingConfig.class);
	}
}
