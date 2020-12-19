package org.zeith.darktheme.internal.data;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Rectangle2F
{
	final float x1;
	final float y1;
	final float x2;
	final float y2;
	final float w;
	final float h;

	public Rectangle2F(float x, float y, float w, float h)
	{
		this.x1 = x;
		this.y1 = y;
		this.x2 = x + w;
		this.y2 = y + h;
		this.w = w;
		this.h = h;
	}

	public float getX1()
	{
		return this.x1;
	}

	public float getY1()
	{
		return this.y1;
	}

	public float getX2()
	{
		return this.x2;
	}

	public float getY2()
	{
		return this.y2;
	}

	public float getWidth()
	{
		return this.w;
	}

	public float getHeight()
	{
		return this.h;
	}

	public static List<Rectangle2F> scaleAll(List<Rectangle2F> origin, float x, float y)
	{
		if(origin == null)
		{
			return Collections.emptyList();
		}
		ArrayList<Rectangle2F> n = new ArrayList<Rectangle2F>();
		for(Rectangle2F r : origin)
		{
			n.add(r.scale(x, y));
		}
		return n;
	}

	public Rectangle2F scale(float x, float y)
	{
		return new Rectangle2F(this.x1 * x, this.y1 * y, this.w * x, this.h * y);
	}

	public boolean contains(float x, float y)
	{
		return x >= this.x1 && y >= this.y1 && x < this.x2 && y < this.y2;
	}

	public String toString()
	{
		return new ToStringBuilder(this).append("x1", this.x1).append("y1", this.y1).append("w", this.w).append("h", this.h).toString();
	}
}