package org.zeith.darktheme.internal;

import org.zeith.darktheme.json.JSONArray;
import org.zeith.darktheme.json.JSONObject;
import org.zeith.darktheme.json.JSONTokener;
import org.zeith.darktheme.DarkThemeMod;

import java.io.*;
import java.util.*;

public class ScriptBrowser
{
	private static final List<FetchableScript> styles = new ArrayList<FetchableScript>();
	private static final Map<String, FetchableScript> styleMap = new HashMap<String, FetchableScript>();
	private static final Map<UUID, FetchableScript> styleBidMap = new HashMap<UUID, FetchableScript>();
	private static final Map<UUID, String> styleBidMD5Map = new HashMap<UUID, String>();
	private static final List<String> defaultDs = new ArrayList<String>();
	public static final List<FetchableScript> stylesList = Collections.unmodifiableList(styles);
	public static final Map<String, FetchableScript> idToStyleMap = Collections.unmodifiableMap(styleMap);
	public static final Map<UUID, FetchableScript> bidToStyleMap = Collections.unmodifiableMap(styleBidMap);
	public static final Map<UUID, String> bidToMd5Map = Collections.unmodifiableMap(styleBidMD5Map);
	public static final List<String> defaultStyle = Collections.unmodifiableList(defaultDs);
	private static boolean workingOnIt = false;

	public static void load()
	{
		if(styles.isEmpty() && !workingOnIt)
		{
			workingOnIt = true;
			new Thread(ScriptBrowser::reload, "DarkThemeBrowserFetcher").start();
		}
	}

	public static void reload()
	{
		styles.clear();
		styleMap.clear();
		styleBidMap.clear();
		styleBidMD5Map.clear();
		defaultDs.clear();
		try
		{
			try(InputStream index = HttpUtil.open("https://zeitheron.github.io/DarkScriptThemes/index.json");)
			{
				JSONArray arr = (JSONArray) new JSONTokener(index).nextValue();
				for(Object obj : arr)
				{
					FetchableScript fs = new FetchableScript((JSONObject) obj);
					styles.add(fs);
					styleMap.put(fs.id, fs);
					styleBidMap.put(fs.bid, fs);
					styleBidMD5Map.put(fs.bid, fs.md5);
				}
			} catch(Throwable arr)
			{
				throw arr;
			}
		} catch(IOException ioe)
		{
			ioe.printStackTrace();
		}
		try
		{
			try(InputStream defds = HttpUtil.open("https://zeitheron.github.io/DarkScriptThemes/default.ds");)
			{
				String ln;
				BufferedReader br = new BufferedReader(new InputStreamReader(defds));
				while((ln = br.readLine()) != null)
				{
					defaultDs.add(ln);
				}
			} catch(Throwable throwable2)
			{
				throw throwable2;
			}
		} catch(IOException ioe)
		{
			ioe.printStackTrace();
		}
		workingOnIt = false;
	}

	public static FetchableScript getById(String id)
	{
		return idToStyleMap.get(id);
	}

	public static FetchableScript getByBid(UUID bid)
	{
		return bidToStyleMap.get(bid);
	}

	public static String getMD5ByBid(UUID bid)
	{
		return bidToMd5Map.get(bid);
	}

	public static class FetchableScript
	{
		public final String name;
		public final String icon;
		public final String description;
		public final String id;
		public final String md5;
		public final String file;
		public final UUID bid;
		public final List<String> dependencies;
		public final List<String> authors;

		public FetchableScript(JSONObject obj)
		{
			this.name = obj.getString("name");
			this.icon = obj.getString("icon");
			this.description = obj.getString("description");
			this.id = obj.getString("id");
			this.md5 = obj.getString("md5");
			this.file = obj.getString("file");
			this.bid = UUID.fromString(obj.getString("bid"));
			ArrayList<String> dl = new ArrayList<String>();
			JSONArray ar = obj.optJSONArray("deps");
			if(ar != null)
			{
				for(Object o : ar)
				{
					dl.add(o.toString());
				}
			}
			this.dependencies = Collections.unmodifiableList(dl);
			dl = new ArrayList();
			ar = obj.optJSONArray("authors");
			if(ar != null)
			{
				for(Object o : ar)
				{
					dl.add(o.toString());
				}
			}
			this.authors = Collections.unmodifiableList(dl);
		}

		public String getDwnUrl()
		{
			return "https://zeitheron.github.io/DarkScriptThemes/scripts/" + this.file;
		}

		public boolean installed()
		{
			return DarkThemeMod.styleBIDS.contains(this.bid);
		}

		public void download(boolean reload)
		{
			File exist = new File(DarkThemeMod.darkScripts, this.file);
			File existDisabled = new File(DarkThemeMod.darkScripts, this.file + ".disabled");
			if(!exist.isFile() && existDisabled.isFile())
			{
				exist = existDisabled;
			}
			if(!exist.isFile() || !MD5.getMD5Checksum(exist).equals(this.md5))
			{
				File tmp = new File(exist.getAbsolutePath() + ".tmp");
				boolean silent = false;
				try(InputStream in = HttpUtil.open(this.getDwnUrl());
					FileOutputStream fos = new FileOutputStream(tmp);)
				{
					int r;
					byte[] buf = new byte[1024];
					while((r = in.read(buf)) > 0)
					{
						fos.write(buf, 0, r);
					}
					silent = true;
				} catch(IOException ioe)
				{
					ioe.printStackTrace();
				}
				if(silent)
				{
					exist.delete();
					tmp.renameTo(exist);
					if(reload)
					{
						DarkThemeMod.loadStyles(exist != existDisabled);
					}
				}
			}
		}
	}
}

