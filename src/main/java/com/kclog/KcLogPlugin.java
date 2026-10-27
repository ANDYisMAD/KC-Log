package com.kclog;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

@PluginDescriptor(name = "KC Log", description = "Tracks monster kill counts reported by RuneLite", tags = {"kc", "kill", "count", "today"})
public class KcLogPlugin extends Plugin
{
	@Inject private Client client;
	@Inject private ClientThread clientThread;
	@Inject private ConfigManager configManager;
	@Inject private ClientToolbar clientToolbar;
	@Inject private KcLogConfig config;
	@Inject private Gson gson;

	private final KillStore store = new KillStore();
	private KcLogPanel panel;
	private NavigationButton navButton;
	private Timer dayRolloverTimer;
	private String lastDate = LocalDate.now().toString();

	@Provides
	KcLogConfig provideConfig(ConfigManager cm)
	{
		return cm.getConfig(KcLogConfig.class);
	}

	@Override
	protected void startUp()
	{
		panel = new KcLogPanel(this);
		BufferedImage icon = ImageUtil.loadImageResource(getClass(), "icon.png");
		navButton = NavigationButton.builder().tooltip("KC Log").icon(icon).priority(7).panel(panel).build();
		clientToolbar.addNavigation(navButton);
		if (client.getGameState() == GameState.LOGGED_IN)
		{
			load();
		}

		// Midnight can pass while the game is open, so re-check the date once a minute.
		dayRolloverTimer = new Timer(60_000, e -> clientThread.invokeLater(() ->
		{
			String now = LocalDate.now().toString();
			if (!now.equals(lastDate))
			{
				lastDate = now;
				prune();
				save();
				refresh();
			}
		}));
		dayRolloverTimer.start();
	}

	@Override
	protected void shutDown()
	{
		if (dayRolloverTimer != null)
		{
			dayRolloverTimer.stop();
			dayRolloverTimer = null;
		}
		if (navButton != null)
		{
			clientToolbar.removeNavigation(navButton);
			navButton = null;
		}
		panel = null;
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged e)
	{
		if (e.getGameState() == GameState.LOGGED_IN)
		{
			load();
		}
	}

	/** Records kills when RuneLite reports loot for an NPC. */
	@Subscribe
	public void onNpcLootReceived(NpcLootReceived e)
	{
		NPC npc = e.getNpc();
		if (npc == null || npc.getName() == null)
		{
			return;
		}

		String name = npc.getName();
		String date = LocalDate.now().toString();
		lastDate = date;
		store.record(name, date);
		prune();
		save();
		refresh();
		if (config.chatMessage())
		{
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Your " + name + " kill count is: " + store.all().get(name) + ".", null);
		}
	}

	private void load()
	{
		// Clear in-memory state first so switching accounts cannot retain the previous profile's counts.
		store.resetAll();
		String json = configManager.getRSProfileConfiguration(KcLogConfig.GROUP, "kills");
		if (json != null && !json.isEmpty())
		{
			Map<String, Integer> parsed = parseCounts(json);
			store.replaceAllKills(parsed);
		}

		String dailyJson = configManager.getRSProfileConfiguration(KcLogConfig.GROUP, "daily");
		if (dailyJson != null && !dailyJson.isEmpty())
		{
			store.replaceAllDaily(parseDailyCounts(dailyJson));
		}

		lastDate = LocalDate.now().toString();
		prune();
		refresh();
	}

	private Map<String, Integer> parseCounts(String json)
	{
		Map<String, Integer> counts = new HashMap<>();
		try
		{
			JsonObject object = new JsonParser().parse(json).getAsJsonObject();
			for (Map.Entry<String, JsonElement> entry : object.entrySet())
			{
				if (entry.getValue() != null && entry.getValue().isJsonPrimitive() && entry.getValue().getAsJsonPrimitive().isNumber())
				{
					int count = entry.getValue().getAsInt();
					if (count >= 0)
					{
						counts.put(entry.getKey(), count);
					}
				}
			}
		}
		catch (RuntimeException ignored)
		{
			// Ignore malformed saved data instead of preventing the plugin from starting.
		}
		return counts;
	}

	private Map<String, Map<String, Integer>> parseDailyCounts(String json)
	{
		Map<String, Map<String, Integer>> result = new HashMap<>();
		try
		{
			JsonObject dates = new JsonParser().parse(json).getAsJsonObject();
			for (Map.Entry<String, JsonElement> dateEntry : dates.entrySet())
			{
				if (dateEntry.getValue() != null && dateEntry.getValue().isJsonObject())
				{
					Map<String, Integer> counts = new HashMap<>();
					for (Map.Entry<String, JsonElement> countEntry : dateEntry.getValue().getAsJsonObject().entrySet())
					{
						JsonElement value = countEntry.getValue();
						if (value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber())
						{
							int count = value.getAsInt();
						if (count >= 0)
						{
							counts.put(countEntry.getKey(), count);
						}
						}
					}
					result.put(dateEntry.getKey(), counts);
				}
			}
		}
		catch (RuntimeException ignored)
		{
			// Ignore malformed saved data instead of preventing the plugin from starting.
		}
		return result;
	}

	private void prune()
	{
		int days = Math.max(1, config.historyDays());
		store.pruneBefore(LocalDate.now().minusDays(days - 1L).toString());
	}

	private void save()
	{
		configManager.setRSProfileConfiguration(KcLogConfig.GROUP, "kills", gson.toJson(store.all()));
		configManager.setRSProfileConfiguration(KcLogConfig.GROUP, "daily", gson.toJson(store.daily()));
	}

	void reset(String name)
	{
		clientThread.invokeLater(() ->
		{
			if (name == null)
			{
				store.resetAll();
			}
			else
			{
				store.reset(name);
			}
			save();
			refresh();
		});
	}

	private void refresh()
	{
		Map<String, Integer> all = store.all();
		Map<String, Integer> today = store.day(LocalDate.now().toString());
		KcLogPanel currentPanel = panel;
		if (currentPanel != null)
		{
			SwingUtilities.invokeLater(() ->
			{
				if (panel == currentPanel)
				{
					currentPanel.update(all, today);
				}
			});
		}
	}
}
