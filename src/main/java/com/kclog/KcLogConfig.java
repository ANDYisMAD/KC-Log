package com.kclog;

import net.runelite.client.config.*;

@ConfigGroup(KcLogConfig.GROUP)
public interface KcLogConfig extends Config
{
	String GROUP = "kclog";

	@ConfigItem(keyName = "chatMessage", name = "Chat message on kill", description = "Show your new kill count in chat after each kill")
	default boolean chatMessage() { return false; }

	@ConfigItem(keyName = "historyDays", name = "Daily history days", description = "How many days of per-day kill counts to keep")
	@Range(min = 1, max = 365)
	default int historyDays() { return 30; }
}
