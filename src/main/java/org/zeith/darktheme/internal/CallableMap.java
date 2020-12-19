package org.zeith.darktheme.internal;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

public class CallableMap<K, V>
		implements Map<K, V>
{
	final Map<K, V> parent;
	final BiFunction<K, V, V> transformer;

	public CallableMap(Map<K, V> parent, BiFunction<K, V, V> transformer)
	{
		this.parent = parent;
		this.transformer = transformer;
	}

	@Override
	public void clear()
	{
		this.parent.clear();
	}

	@Override
	public boolean containsKey(Object key)
	{
		return this.parent.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value)
	{
		return this.parent.containsValue(value);
	}

	@Override
	public Set<Map.Entry<K, V>> entrySet()
	{
		return this.parent.entrySet();
	}

	@Override
	public V get(Object key)
	{
		return this.parent.get(key);
	}

	@Override
	public boolean isEmpty()
	{
		return this.parent.isEmpty();
	}

	@Override
	public Set<K> keySet()
	{
		return this.parent.keySet();
	}

	@Override
	public V put(K key, V value)
	{
		return this.parent.put(key, this.transformer.apply(key, value));
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m)
	{
		this.parent.putAll(m);
	}

	@Override
	public V remove(Object key)
	{
		return this.parent.remove(key);
	}

	@Override
	public int size()
	{
		return this.parent.size();
	}

	@Override
	public Collection<V> values()
	{
		return this.parent.values();
	}
}

