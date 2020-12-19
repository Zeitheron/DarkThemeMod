package org.zeith.darktheme.internal.data;

import net.minecraft.util.ResourceLocation;

public class TextureRectangle
{
	final ResourceLocation tex;
	final Rectangle2F rect;

	public TextureRectangle(ResourceLocation texture, Rectangle2F rectangle)
	{
		this.tex = texture;
		this.rect = rectangle;
	}

	public ResourceLocation getTex()
	{
		return this.tex;
	}

	public Rectangle2F getRect()
	{
		return this.rect;
	}
}