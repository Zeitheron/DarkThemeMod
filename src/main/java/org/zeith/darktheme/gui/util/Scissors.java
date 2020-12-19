package org.zeith.darktheme.gui.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.opengl.GL11;

public class Scissors
{
	public static void begin()
	{
		GL11.glEnable(GL11.GL_SCISSOR_TEST);
	}

	public static void scissor(int x, int y, int width, int height)
	{
		int sw = Minecraft.getMinecraft().displayWidth;
		int sh = Minecraft.getMinecraft().displayHeight;
		ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
		float dw = sr.getScaledWidth();
		float dh = sr.getScaledHeight();
		x = Math.round((float) sw * ((float) x / dw));
		y = Math.round((float) sh * ((float) y / dh));
		width = Math.round((float) sw * ((float) width / dw));
		height = Math.round((float) sh * ((float) height / dh));
		GL11.glScissor(x, sh - height - y, width, height);
	}

	public static void end()
	{
		GL11.glDisable(GL11.GL_SCISSOR_TEST);
	}
}

