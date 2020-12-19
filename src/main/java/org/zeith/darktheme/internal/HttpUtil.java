package org.zeith.darktheme.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.HashMap;

public class HttpUtil
{
	public static InputStream open(String url) throws IOException
	{
		HttpURLConnection conn;
		HashMap<String, Integer> visited = new HashMap<>();
		block3:
		while(true)
		{
            if(visited.compute(url, (key, count) -> count == null ? 1 : count + 1) > 3)
				throw new IOException("Stuck in redirect loop");
			URL resourceUrl = new URL(url);
			conn = (HttpURLConnection) resourceUrl.openConnection();
			conn.setConnectTimeout(15000);
			conn.setReadTimeout(15000);
			conn.setInstanceFollowRedirects(false);
			conn.setRequestProperty("User-Agent", "Mozilla/5.0...");
			switch(conn.getResponseCode())
			{
				case 301:
				case 302:
				{
					String location = conn.getHeaderField("Location");
					location = URLDecoder.decode(location, "UTF-8");
					URL base = new URL(url);
					URL next = new URL(base, location);
					url = next.toExternalForm();
					continue block3;
				}
			}
			break;
		}
		return conn.getInputStream();
	}

	public static InputStream openCached(String url) throws IOException
	{
		return CacheStore.readWithCachingURL(url);
	}
}

