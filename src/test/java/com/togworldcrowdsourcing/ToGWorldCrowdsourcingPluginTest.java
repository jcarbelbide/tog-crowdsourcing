package net.runelite.client.plugins.togcrowdsourcing.src.test.java.com.togworldcrowdsourcing;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;
import net.runelite.client.plugins.togcrowdsourcing.src.main.java.com.togcrowdsourcing.ToGCrowdsourcingPlugin;

public class ToGWorldCrowdsourcingPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(ToGCrowdsourcingPlugin.class);
		RuneLite.main(args);
	}
}