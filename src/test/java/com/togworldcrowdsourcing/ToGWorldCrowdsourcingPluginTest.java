package net.runelite.client.plugins.togworldcrowdsourcing.src.test.java.com.togworldcrowdsourcing;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;
import net.runelite.client.plugins.togworldcrowdsourcing.src.main.java.com.togworldcrowdsourcing.ToGWorldCrowdsourcingPlugin;

public class ToGWorldCrowdsourcingPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(ToGWorldCrowdsourcingPlugin.class);
		RuneLite.main(args);
	}
}