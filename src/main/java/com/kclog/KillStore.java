package com.kclog;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Kill counts held in memory: an all-time total per monster, plus a per-day
 * breakdown keyed by ISO date (yyyy-MM-dd) so the panel can show a "Today" view.
 */
final class KillStore
{
	private final Map<String, Integer> kills = new HashMap<>();
	private final Map<String, Map<String, Integer>> daily = new TreeMap<>();

	synchronized void record(String monster, String date)
	{
		kills.merge(monster, 1, Integer::sum);
		daily.computeIfAbsent(date, d -> new HashMap<>()).merge(monster, 1, Integer::sum);
	}

	synchronized Map<String, Integer> all()
	{
		return new HashMap<>(kills);
	}

	synchronized Map<String, Integer> day(String date)
	{
		Map<String, Integer> d = daily.get(date);
		return d == null ? new HashMap<>() : new HashMap<>(d);
	}

	synchronized Map<String, Map<String, Integer>> daily()
	{
		Map<String, Map<String, Integer>> copy = new TreeMap<>();
		for (Map.Entry<String, Map<String, Integer>> en : daily.entrySet())
		{
			copy.put(en.getKey(), new HashMap<>(en.getValue()));
		}
		return copy;
	}

	synchronized int totalKills()
	{
		return kills.values().stream().mapToInt(Integer::intValue).sum();
	}

	synchronized void replaceAllKills(Map<String, Integer> newKills)
	{
		kills.clear();
		if (newKills != null) kills.putAll(newKills);
	}

	synchronized void replaceAllDaily(Map<String, ? extends Map<String, Integer>> newDaily)
	{
		daily.clear();
		if (newDaily == null) return;
		for (Map.Entry<String, ? extends Map<String, Integer>> en : newDaily.entrySet())
		{
			if (en.getKey() != null && en.getValue() != null)
			{
				daily.put(en.getKey(), new HashMap<>(en.getValue()));
			}
		}
	}

	/**
	 * Drop day buckets strictly older than cutoffDate. ISO dates sort as strings,
	 * so a plain string compare is enough here.
	 */
	synchronized void pruneBefore(String cutoffDate)
	{
		daily.keySet().removeIf(date -> date.compareTo(cutoffDate) < 0);
	}

	synchronized void reset(String monster)
	{
		kills.remove(monster);
		for (Map<String, Integer> day : daily.values()) day.remove(monster);
	}

	synchronized void resetAll()
	{
		kills.clear();
		daily.clear();
	}
}
