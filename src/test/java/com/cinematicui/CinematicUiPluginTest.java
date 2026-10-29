package com.cinematicui;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class CinematicUiPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(CinematicUiPlugin.class);
		RuneLite.main(args);
	}
}
