package com.kclog;

import static org.junit.Assert.*;
import org.junit.Test;

public class EngagementTrackerTest
{
	@Test public void countsEngagedDeathWithoutLoot()
	{
		EngagementTracker t = new EngagementTracker();
		t.engage(5, 10);
		assertTrue(t.onDeath(5, 20));
	}

	@Test public void ignoresNpcYouNeverFought()
	{
		assertFalse(new EngagementTracker().onDeath(5, 20));
	}

	@Test public void countsEachDeathOnce()
	{
		EngagementTracker t = new EngagementTracker();
		t.engage(5, 10);
		assertTrue(t.onDeath(5, 20));
		assertFalse(t.onDeath(5, 21));
	}

	@Test public void respawnCanBeCountedAgain()
	{
		EngagementTracker t = new EngagementTracker();
		t.engage(5, 10);
		t.onDeath(5, 20);
		t.forget(5);
		t.engage(5, 40);
		assertTrue(t.onDeath(5, 50));
	}

	@Test public void staleEngagementDoesNotCount()
	{
		EngagementTracker t = new EngagementTracker();
		t.engage(5, 0);
		assertFalse(t.onDeath(5, EngagementTracker.TIMEOUT_TICKS + 1));
	}
}
