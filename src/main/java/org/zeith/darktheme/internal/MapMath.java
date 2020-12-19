package org.zeith.darktheme.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MapMath
{
	public static <K, V> void add(Map<K, List<V>> src, Map<K, List<V>> dst)
	{
		for(K k : src.keySet())
		{
			if(!dst.containsKey(k))
			{
				dst.put(k, new ArrayList<>());
			}
			dst.get(k).addAll(src.get(k));
		}
	}

	public static <K, V> void remove(Map<K, List<V>> src, Map<K, List<V>> dst)
	{
		for(K k : src.keySet())
		{
			if(!dst.containsKey(k)) continue;
			List<V> vs = dst.get(k);
			vs.removeAll(src.get(k));
			if(!vs.isEmpty()) continue;
			dst.remove(k);
		}
	}

	public static <K, V> void remove(Map<K, List<V>> map, K k, V v)
	{
		if(map.containsKey(k))
		{
			List<V> vs = map.get(k);
			vs.remove(v);
			if(vs.isEmpty())
			{
				map.remove(k);
			}
		}
	}

	public static <K, V> void add(Map<K, List<V>> map, K k, V v)
	{
		if(!map.containsKey(k))
			map.put(k, new ArrayList<>());
		map.get(k).add(v);
	}
}

