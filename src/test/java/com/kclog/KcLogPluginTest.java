package com.kclog;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class KcLogPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(KcLogPlugin.class);
		RuneLite.main(args);
	}
}
