package net.runelite.client.plugins.togcrowdsourcing.src.test.java.com.togcrowdsourcing;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;
import net.runelite.client.plugins.togcrowdsourcing.src.main.java.com.togcrowdsourcing.ToGCrowdsourcingPlugin;

public class ToGCrowdsourcingPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(ToGCrowdsourcingPlugin.class);
		RuneLite.main(args);
	}
}