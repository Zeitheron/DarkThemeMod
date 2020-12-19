package org.zeith.darktheme.internal.data;

import net.minecraft.util.ResourceLocation;

import java.awt.*;

public class PartialTexColor
{
	public final ResourceLocation path;
	public final Rectangle2F rect;
	public final Point color;

	public PartialTexColor(ResourceLocation path, Rectangle2F rect, Point color)
	{
		this.path = path;
		this.rect = rect;
		this.color = color;
	}
}