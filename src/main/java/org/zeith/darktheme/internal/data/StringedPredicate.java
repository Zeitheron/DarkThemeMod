package org.zeith.darktheme.internal.data;

import java.util.function.Predicate;

public class StringedPredicate<T>
		implements Predicate<T>
{
	final Predicate<T> superior;
	final String str;

	public StringedPredicate(Predicate<T> superior, String str)
	{
		this.superior = superior;
		this.str = str;
	}

	@Override
	public boolean test(T t)
	{
		if(this.superior == null)
		{
			System.out.println("ERROR IN \"" + this.str + "\"!!!");
			return false;
		}
		return this.superior.test(t);
	}

	@Override
	public String toString()
	{
		return this.str;
	}
}