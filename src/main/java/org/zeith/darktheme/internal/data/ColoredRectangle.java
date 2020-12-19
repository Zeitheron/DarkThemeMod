package org.zeith.darktheme.internal.data;

import net.minecraft.util.ResourceLocation;

public class ColoredRectangle
{
	public final ResourceLocation tex;
	public final Rectangle2F rect;
	public final int color;

	public ColoredRectangle(ResourceLocation tex, Rectangle2F rect, int color)
	{
		this.tex = tex;
		this.rect = rect;
		this.color = color;
	}
}