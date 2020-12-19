package org.zeith.darktheme.internal.data;

import net.minecraft.util.ResourceLocation;

public class TxMapSprite
{
	public final ResourceLocation textureMap;
	public final String spriteName;

	public TxMapSprite(ResourceLocation txm, String name)
	{
		this.textureMap = txm;
		this.spriteName = name;
	}
}