package org.zeith.darktheme.internal;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ColorHelper
{
	public static void glColor1ia(int argb)
	{
		GlStateManager.color((float) ColorHelper.getRed(argb), (float) ColorHelper.getGreen(argb), (float) ColorHelper.getBlue(argb), (float) ColorHelper.getAlpha(argb));
	}

	public static void glColor1i(int argb)
	{
		GlStateManager.color((float) ColorHelper.getRed(argb), (float) ColorHelper.getGreen(argb), (float) ColorHelper.getBlue(argb));
	}

	public static int multiply(int argb, float multi)
	{
		return ColorHelper.packARGB(ColorHelper.getAlpha(argb) * multi, ColorHelper.getRed(argb) * multi, ColorHelper.getGreen(argb) * multi, ColorHelper.getBlue(argb) * multi);
	}

	public static int packARGB(float a, float r, float g, float b)
	{
		return (int) (a * 255.0f) << 24 | (int) (r * 255.0f) << 16 | (int) (g * 255.0f) << 8 | (int) (b * 255.0f);
	}

	public static int packRGB(float r, float g, float b)
	{
		return (int) (r * 255.0f) << 16 | (int) (g * 255.0f) << 8 | (int) (b * 255.0f);
	}

	public static int packARGB(int a, int r, int g, int b)
	{
		return a << 24 | r << 16 | b << 8 | b;
	}

	public static int packRGB(int r, int g, int b)
	{
		return r << 16 | b << 8 | b;
	}

	public static float getAlpha(int rgb)
	{
		return (float) (rgb >> 24 & 0xFF) / 255.0f;
	}

	public static float getRed(int rgb)
	{
		return (float) (rgb >> 16 & 0xFF) / 255.0f;
	}

	public static float getGreen(int rgb)
	{
		return (float) (rgb >> 8 & 0xFF) / 255.0f;
	}

	public static float getBlue(int rgb)
	{
		return (float) (rgb >> 0 & 0xFF) / 255.0f;
	}

	public static float getBrightnessF(int rgb)
	{
		return ColorHelper.getRed(rgb) * ColorHelper.getGreen(rgb) * ColorHelper.getBlue(rgb);
	}

	public static int getBrightnessRGB(int rgb)
	{
		int bri = (int) (ColorHelper.getBrightnessF(rgb) * 255.0f);
		return bri << 16 | bri << 8 | bri;
	}

	@SideOnly(value = Side.CLIENT)
	public static void gl(int rgba)
	{
		GlStateManager.color((float) ColorHelper.getRed(rgba), (float) ColorHelper.getGreen(rgba), (float) ColorHelper.getBlue(rgba), (float) ColorHelper.getAlpha(rgba));
	}

	public static int interpolateSine(int a, int b, float progress)
	{
		return ColorHelper.interpolate(a, b, progress <= 0.0f ? 0.0f : (progress >= 1.0f ? 1.0f : (float) Math.sin(Math.toRadians(progress * 90.0f))));
	}

	public static int interpolate(int a, int b, float progress)
	{
		float rs = ColorHelper.getRed(a) * (1.0f - progress) + ColorHelper.getRed(b) * progress;
		float gs = ColorHelper.getGreen(a) * (1.0f - progress) + ColorHelper.getGreen(b) * progress;
		float bs = ColorHelper.getBlue(a) * (1.0f - progress) + ColorHelper.getBlue(b) * progress;
		float as = ColorHelper.getAlpha(a) * (1.0f - progress) + ColorHelper.getAlpha(b) * progress;
		return ColorHelper.packARGB(as, rs, gs, bs);
	}
}