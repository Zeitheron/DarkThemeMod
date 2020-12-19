package org.zeith.darktheme.api;

import net.minecraft.util.ResourceLocation;

import java.util.function.Predicate;

public interface IDarkAPI
{
	default void excludeTexture(ResourceLocation texture)
	{
		this.excludeTextures(texture::equals);
	}

	void excludeTextures(Predicate<ResourceLocation> var1);

	void color(int var1, int var2);

	int texColor(int var1);
}