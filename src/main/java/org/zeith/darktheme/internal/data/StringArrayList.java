package org.zeith.darktheme.internal.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class StringArrayList
		extends ArrayList<String>
{
	public StringArrayList(Collection<String> parent)
	{
		super(parent == null ? Collections.emptyList() : parent);
	}

	public StringArrayList()
	{
	}
}