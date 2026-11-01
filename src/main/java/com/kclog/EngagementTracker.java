package com.kclog;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Remembers which NPCs the player attacked or hit recently, so a death can be counted
 * whether or not the NPC drops loot, and each death is counted only once.
 * Plain Java so it can be unit tested without RuneLite.
 */
class EngagementTracker
{
	/** About 60 seconds of game ticks; covers long fights and multi-combat tagging. */
	static final int TIMEOUT_TICKS = 100;

	private final Map<Integer, Integer> lastEngaged = new HashMap<>();
	private final Set<Integer> counted = new HashSet<>();

	/** Player attacked, targeted, or landed any hitsplat on this NPC. */
	void engage(int npcIndex, int tick)
	{
		lastEngaged.put(npcIndex, tick);
		counted.remove(npcIndex);
	}

	/** Returns true exactly once per engaged death; false if the player never fought it. */
	boolean onDeath(int npcIndex, int tick)
	{
		Integer when = lastEngaged.get(npcIndex);
		if (when == null || tick - when > TIMEOUT_TICKS || counted.contains(npcIndex))
		{
			return false;
		}
		counted.add(npcIndex);
		return true;
	}

	/** NPC left the scene: forget it so a respawn using the same index starts fresh. */
	void forget(int npcIndex)
	{
		lastEngaged.remove(npcIndex);
		counted.remove(npcIndex);
	}

	void expire(int tick)
	{
		Iterator<Map.Entry<Integer, Integer>> it = lastEngaged.entrySet().iterator();
		while (it.hasNext())
		{
			Map.Entry<Integer, Integer> e = it.next();
			if (tick - e.getValue() > TIMEOUT_TICKS * 2)
			{
				counted.remove(e.getKey());
				it.remove();
			}
		}
	}

	void clear()
	{
		lastEngaged.clear();
		counted.clear();
	}
}
