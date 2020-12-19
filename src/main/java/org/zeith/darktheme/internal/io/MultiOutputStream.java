package org.zeith.darktheme.internal.io;

import java.io.IOException;
import java.io.OutputStream;

public class MultiOutputStream
		extends OutputStream
{
	final OutputStream[] streams;

	public MultiOutputStream(OutputStream... streams)
	{
		this.streams = streams;
	}

	@Override
	public void write(int b) throws IOException
	{
		for(OutputStream out : this.streams)
		{
			out.write(b);
		}
	}

	@Override
	public void write(byte[] b) throws IOException
	{
		for(OutputStream out : this.streams)
		{
			out.write(b);
		}
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException
	{
		for(OutputStream out : this.streams)
		{
			out.write(b, off, len);
		}
	}

	@Override
	public void flush() throws IOException
	{
		for(OutputStream out : this.streams)
		{
			out.flush();
		}
	}

	@Override
	public void close() throws IOException
	{
		for(OutputStream out : this.streams)
		{
			out.close();
		}
	}
}