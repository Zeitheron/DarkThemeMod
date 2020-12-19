package org.zeith.darktheme.gui.util;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

@SideOnly(value = Side.CLIENT)
public class RenderUtil
{
	public static double zLevel = 0.0;

	public static void drawFullRectangleFit(double x, double y, double width, double height)
	{
		int w = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
		int h = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);

		float ws = 1, hs = 1;

		if(w > h)
			hs = h / (float) w;
		if(h > w)
			ws = w / (float) h;

		double nw = width * ws;
		double nh = height * hs;

		drawFullTexturedModalRect(x + (width - nw) / 2, y + (height - nh) / 2, nw, nh);
	}

	public static void drawTexturedModalRect(double x, double y, double texX, double texY, double width, double height)
	{
		float n = 0.00390625F;
		Tessellator tess = Tessellator.getInstance();
		BufferBuilder vb = tess.getBuffer();
		vb.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
		vb.pos(x, y + height, zLevel).tex(texX * n, (texY + height) * n).endVertex();
		vb.pos(x + width, y + height, zLevel).tex((texX + width) * n, (texY + height) * n).endVertex();
		vb.pos(x + width, y, zLevel).tex((texX + width) * n, texY * n).endVertex();
		vb.pos(x, y, zLevel).tex(texX * n, texY * n).endVertex();
		tess.draw();
	}

	public static void drawFullTexturedModalRect(double x, double y, double width, double height)
	{
		float n = 0.00390625F;
		Tessellator tess = Tessellator.getInstance();
		BufferBuilder vb = tess.getBuffer();
		vb.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);

		vb.pos(x, y + height, zLevel).tex(0, 1).endVertex();
		vb.pos(x + width, y + height, zLevel).tex(1, 1).endVertex();
		vb.pos(x + width, y, zLevel).tex(1, 0).endVertex();
		vb.pos(x, y, zLevel).tex(0, 0).endVertex();

		tess.draw();
	}

	public static void drawHorizontalGradientRect(float left, float top, float width, float height, int startColor, int endColor)
	{
		float f = (float) (startColor >> 24 & 0xFF) / 255.0f;
		float f1 = (float) (startColor >> 16 & 0xFF) / 255.0f;
		float f2 = (float) (startColor >> 8 & 0xFF) / 255.0f;
		float f3 = (float) (startColor & 0xFF) / 255.0f;
		float f4 = (float) (endColor >> 24 & 0xFF) / 255.0f;
		float f5 = (float) (endColor >> 16 & 0xFF) / 255.0f;
		float f6 = (float) (endColor >> 8 & 0xFF) / 255.0f;
		float f7 = (float) (endColor & 0xFF) / 255.0f;
		float right = left + width;
		float bottom = top + height;
		GlStateManager.disableTexture2D();
		GlStateManager.enableBlend();
		GlStateManager.disableAlpha();
		GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
		GlStateManager.shadeModel(7425);
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder bufferbuilder = tessellator.getBuffer();
		bufferbuilder.begin(7, DefaultVertexFormats.POSITION_COLOR);
		bufferbuilder.pos(right, top, 0.0).color(f1, f2, f3, f).endVertex();
		bufferbuilder.pos(left, top, 0.0).color(f1, f2, f3, f).endVertex();
		bufferbuilder.pos(left, bottom, 0.0).color(f5, f6, f7, f4).endVertex();
		bufferbuilder.pos(right, bottom, 0.0).color(f5, f6, f7, f4).endVertex();
		tessellator.draw();
		GlStateManager.shadeModel(7424);
		GlStateManager.disableBlend();
		GlStateManager.enableAlpha();
		GlStateManager.enableTexture2D();
	}

	public static void drawVerticalGradientRect(float left, float top, float width, float height, int startColor, int endColor)
	{
		float f = (float) (startColor >> 24 & 0xFF) / 255.0f;
		float f1 = (float) (startColor >> 16 & 0xFF) / 255.0f;
		float f2 = (float) (startColor >> 8 & 0xFF) / 255.0f;
		float f3 = (float) (startColor & 0xFF) / 255.0f;
		float f4 = (float) (endColor >> 24 & 0xFF) / 255.0f;
		float f5 = (float) (endColor >> 16 & 0xFF) / 255.0f;
		float f6 = (float) (endColor >> 8 & 0xFF) / 255.0f;
		float f7 = (float) (endColor & 0xFF) / 255.0f;
		float right = left + width;
		float bottom = top + height;
		GlStateManager.disableTexture2D();
		GlStateManager.enableBlend();
		GlStateManager.disableAlpha();
		GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
		GlStateManager.shadeModel(7425);
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder bufferbuilder = tessellator.getBuffer();
		bufferbuilder.begin(7, DefaultVertexFormats.POSITION_COLOR);
		bufferbuilder.pos(right, top, 0.0).color(f5, f6, f7, f4).endVertex();
		bufferbuilder.pos(left, top, 0.0).color(f1, f2, f3, f).endVertex();
		bufferbuilder.pos(left, bottom, 0.0).color(f1, f2, f3, f).endVertex();
		bufferbuilder.pos(right, bottom, 0.0).color(f5, f6, f7, f4).endVertex();
		tessellator.draw();
		GlStateManager.shadeModel(7424);
		GlStateManager.disableBlend();
		GlStateManager.enableAlpha();
		GlStateManager.enableTexture2D();
	}
}

