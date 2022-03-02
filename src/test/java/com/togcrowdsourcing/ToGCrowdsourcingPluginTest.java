package com.togcrowdsourcing;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class ToGCrowdsourcingPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(ToGCrowdsourcingPlugin.class);
		RuneLite.main(args);
	}
}