package com.kclog;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Fetches creature portraits from the official Old School RuneScape Wiki in the background.
 * Tries the NPC's chathead (its face) first, then the page's main image.
 * Any failure just leaves the placeholder; it never touches kill counting.
 */
class PortraitService
{
	static final int SIZE = 28;
	private static final String API = "https://oldschool.runescape.wiki/api.php";
	private static final String USER_AGENT = "KC Log RuneLite plugin (github.com/kclog)";

	private final OkHttpClient http;
	private final Map<String, ImageIcon> cache = new ConcurrentHashMap<>();
	private final Map<String, List<Consumer<ImageIcon>>> pending = new ConcurrentHashMap<>();
	private final Map<String, Boolean> missing = new ConcurrentHashMap<>();

	PortraitService(OkHttpClient http)
	{
		this.http = http;
	}

	/** Calls back on the Swing thread with the portrait, if one is found. */
	void get(String name, Consumer<ImageIcon> onReady)
	{
		ImageIcon hit = cache.get(name);
		if (hit != null)
		{
			onReady.accept(hit);
			return;
		}
		if (missing.containsKey(name))
		{
			return;
		}
		boolean first;
		synchronized (pending)
		{
			List<Consumer<ImageIcon>> waiters = pending.get(name);
			first = waiters == null;
			if (first)
			{
				waiters = new ArrayList<>();
				pending.put(name, waiters);
			}
			waiters.add(onReady);
		}
		if (first)
		{
			String title = wikiTitle(name);
			lookup("titles=" + enc("File:" + title + " chathead.png") + "&prop=imageinfo&iiprop=url&iiurlwidth=" + (SIZE * 2), name,
				() -> lookup("titles=" + enc(title) + "&redirects=1&prop=pageimages&pithumbsize=" + (SIZE * 2), name,
					() -> finish(name, null)));
		}
	}

	static String wikiTitle(String npcName)
	{
		// Strip RuneLite colour tags, then use wiki title casing (first letter upper).
		String n = npcName.replaceAll("<[^>]*>", "").replace('\u00A0', ' ').trim();
		return n.isEmpty() ? n : Character.toUpperCase(n.charAt(0)) + n.substring(1);
	}

	private static String enc(String s)
	{
		try
		{
			return URLEncoder.encode(s, StandardCharsets.UTF_8.name());
		}
		catch (Exception e)
		{
			return s;
		}
	}

	private void lookup(String query, String name, Runnable onMiss)
	{
		HttpUrl url = HttpUrl.parse(API + "?action=query&format=json&" + query);
		if (url == null)
		{
			onMiss.run();
			return;
		}
		http.newCall(new Request.Builder().url(url).header("User-Agent", USER_AGENT).build()).enqueue(new Callback()
		{
			@Override public void onFailure(Call call, IOException e) { onMiss.run(); }

			@Override
			public void onResponse(Call call, Response response)
			{
				try (Response r = response)
				{
					String imageUrl = r.isSuccessful() && r.body() != null ? findImageUrl(r.body().string()) : null;
					if (imageUrl == null)
					{
						onMiss.run();
					}
					else
					{
						download(imageUrl, name, onMiss);
					}
				}
				catch (Exception e)
				{
					onMiss.run();
				}
			}
		});
	}

	static String findImageUrl(String json)
	{
		JsonObject pages = new JsonParser().parse(json).getAsJsonObject().getAsJsonObject("query").getAsJsonObject("pages");
		for (Map.Entry<String, JsonElement> e : pages.entrySet())
		{
			JsonObject page = e.getValue().getAsJsonObject();
			if (page.has("thumbnail"))
			{
				return page.getAsJsonObject("thumbnail").get("source").getAsString();
			}
			if (page.has("imageinfo"))
			{
				JsonObject info = page.getAsJsonArray("imageinfo").get(0).getAsJsonObject();
				return info.has("thumburl") ? info.get("thumburl").getAsString() : info.get("url").getAsString();
			}
		}
		return null;
	}

	private void download(String imageUrl, String name, Runnable onMiss)
	{
		http.newCall(new Request.Builder().url(imageUrl).header("User-Agent", USER_AGENT).build()).enqueue(new Callback()
		{
			@Override public void onFailure(Call call, IOException e) { onMiss.run(); }

			@Override
			public void onResponse(Call call, Response response)
			{
				try (Response r = response; InputStream in = r.body() == null ? null : r.body().byteStream())
				{
					BufferedImage img = in == null ? null : ImageIO.read(in);
					if (img == null)
					{
						onMiss.run();
						return;
					}
					finish(name, new ImageIcon(fit(img)));
				}
				catch (Exception e)
				{
					onMiss.run();
				}
			}
		});
	}

	private static Image fit(BufferedImage img)
	{
		double scale = Math.min((double) SIZE / img.getWidth(), (double) SIZE / img.getHeight());
		int w = Math.max(1, (int) Math.round(img.getWidth() * scale));
		int h = Math.max(1, (int) Math.round(img.getHeight() * scale));
		return img.getScaledInstance(w, h, Image.SCALE_SMOOTH);
	}

	private void finish(String name, ImageIcon icon)
	{
		List<Consumer<ImageIcon>> waiters;
		synchronized (pending)
		{
			waiters = pending.remove(name);
		}
		if (icon == null)
		{
			missing.put(name, Boolean.TRUE);
			return;
		}
		cache.put(name, icon);
		if (waiters != null)
		{
			SwingUtilities.invokeLater(() -> waiters.forEach(w -> w.accept(icon)));
		}
	}
}
