package org.zeith.darktheme.api;

import net.minecraft.util.ResourceLocation;

import java.util.function.Predicate;

public class DarkThemeAPI
		implements IDarkAPI
{
	private static final IDarkAPI api = new DarkThemeAPI();

	public static IDarkAPI getApi()
	{
		return api;
	}

	@Override
	public void excludeTextures(Predicate<ResourceLocation> textures)
	{
	}

	@Override
	public void color(int srcARGB, int dstARGB)
	{
	}

	@Override
	public int texColor(int color)
	{
		return color;
	}
}