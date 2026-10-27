package com.kclog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class KillStoreTest
{
	@Test
	public void todayCountsAreSeparateFromAllTime()
	{
		KillStore s = new KillStore();
		s.record("Goblin", "2026-10-24");
		s.record("Goblin", "2026-10-24");
		s.record("Goblin", "2026-10-25");

		assertEquals(3, s.all().get("Goblin").intValue());
		assertEquals(2, s.day("2026-10-24").get("Goblin").intValue());
		assertEquals(1, s.day("2026-10-25").get("Goblin").intValue());
	}

	@Test
	public void newDayStartsAtZero()
	{
		KillStore s = new KillStore();
		s.record("Goblin", "2026-10-24");
		assertTrue(s.day("2026-10-25").isEmpty());
		assertEquals(1, s.all().get("Goblin").intValue());
	}

	@Test
	public void pruneDropsOnlyDaysOlderThanTheCutoff()
	{
		KillStore s = new KillStore();
		s.record("Goblin", "2026-09-01");
		s.record("Goblin", "2026-09-24");
		s.record("Goblin", "2026-10-24");

		s.pruneBefore("2026-09-24");

		assertFalse(s.daily().containsKey("2026-09-01"));
		assertTrue(s.daily().containsKey("2026-09-24"));
		assertTrue(s.daily().containsKey("2026-10-24"));
		// All-time totals are never touched by pruning.
		assertEquals(3, s.all().get("Goblin").intValue());
	}

	@Test
	public void resetOneMonsterLeavesTheOthersAlone()
	{
		KillStore s = new KillStore();
		s.record("Goblin", "2026-10-24");
		s.record("Orc", "2026-10-24");
		s.record("Orc", "2026-10-25");

		s.reset("Goblin");

		assertFalse(s.all().containsKey("Goblin"));
		assertFalse(s.day("2026-10-24").containsKey("Goblin"));
		assertEquals(2, s.all().get("Orc").intValue());
		assertEquals(1, s.day("2026-10-25").get("Orc").intValue());
	}

	@Test
	public void resetAllClearsBothViews()
	{
		KillStore s = new KillStore();
		s.record("Goblin", "2026-10-24");
		s.resetAll();

		assertEquals(0, s.totalKills());
		assertTrue(s.daily().isEmpty());
	}
}
