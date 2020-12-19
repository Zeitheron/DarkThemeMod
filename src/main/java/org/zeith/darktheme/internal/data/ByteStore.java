package org.zeith.darktheme.internal.data;

import java.util.BitSet;

public class ByteStore
{
	final BitSet bits;

	public ByteStore(byte data)
	{
		this.bits = BitSet.valueOf(new byte[]{ data });
	}

	public boolean get(int n)
	{
		return this.bits.get(n);
	}

	public void set(int n, boolean v)
	{
		this.bits.set(n, v);
	}

	public byte getData()
	{
		if(this.bits.toByteArray().length == 0)
			return 0;
		return this.bits.toByteArray()[0];
	}
}