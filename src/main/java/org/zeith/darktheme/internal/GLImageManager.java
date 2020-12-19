package org.zeith.darktheme.internal;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

public class GLImageManager
{
	public static void loadTexture(BufferedImage image, int id, boolean mirrorEffect)
	{
		if(image == null)
		{
			return;
		}
		int[] pixels = new int[image.getWidth() * image.getHeight()];
		image.getRGB(0, 0, image.getWidth(), image.getHeight(), pixels, 0, image.getWidth());
		ByteBuffer buffer = BufferUtils.createByteBuffer(image.getWidth() * image.getHeight() * 4);
		for(int y = 0; y < image.getHeight(); ++y)
		{
			for(int x = 0; x < image.getWidth(); ++x)
			{
				int pixel = pixels[y * image.getWidth() + (mirrorEffect ? image.getWidth() - x - 1 : x)];
				buffer.put((byte) (pixel >> 16 & 0xFF));
				buffer.put((byte) (pixel >> 8 & 0xFF));
				buffer.put((byte) (pixel & 0xFF));
				buffer.put((byte) (pixel >> 24 & 0xFF));
			}
		}
		buffer.flip();
		GL11.glBindTexture(3553, id);
		GL11.glTexParameteri(3553, 10242, 33071);
		GL11.glTexParameteri(3553, 10243, 33071);
		GL11.glTexParameteri(3553, 10241, 9728);
		GL11.glTexParameteri(3553, 10240, 9728);
		GL11.glTexImage2D(3553, 0, 32856, image.getWidth(), image.getHeight(), 0, 6408, 5121, buffer);
	}

	public static BufferedImage toBufferedImage(int glTex)
	{
		return GLImageManager.toBufferedImage(GLImageManager.toByteBuffer(glTex), GLImageManager.getWidth(glTex), GLImageManager.getHeight(glTex), GLImageManager.getChannels(glTex));
	}

	public static BufferedImage toBufferedImage(ByteBuffer buffer, int width, int height, int channels)
	{
		BufferedImage image = new BufferedImage(width, height, 2);
		for(int x = 0; x < width; ++x)
		{
			for(int y = 0; y < height; ++y)
			{
				int i = (x + y * width) * channels;
				int r = buffer.get(i) & 0xFF;
				int g = buffer.get(i + 1) & 0xFF;
				int b = buffer.get(i + 2) & 0xFF;
				int a = 255;
				if(channels == 4)
				{
					a = buffer.get(i + 3) & 0xFF;
				}
				image.setRGB(x, y, a << 24 | r << 16 | g << 8 | b);
			}
		}
		return image;
	}

	public static BufferedImage exportSprite(TextureAtlasSprite sprite, BufferedImage textureMap)
	{
		BufferedImage target = new BufferedImage(sprite.getIconWidth(), sprite.getIconHeight(), 2);
		for(int x = 0; x < target.getWidth(); ++x)
		{
			for(int y = 0; y < target.getHeight(); ++y)
			{
				float rx = (float) x / (float) target.getWidth();
				float ry = (float) y / (float) target.getHeight();
				double u = sprite.getMinU() + (sprite.getMaxU() - sprite.getMinU()) * rx;
				double v = sprite.getMinV() + (sprite.getMaxV() - sprite.getMinV()) * ry;
				int tx = (int) (u * (double) textureMap.getWidth());
				int ty = (int) (v * (double) textureMap.getHeight());
				try
				{
					target.setRGB(x, y, textureMap.getRGB(tx, ty));
					continue;
				} catch(ArrayIndexOutOfBoundsException e)
				{
					target.setRGB(x, y, textureMap.getRGB(0, 0));
				}
			}
		}
		return target;
	}

	public static void toGL4(ByteBuffer buffer, int glTex, int width, int height)
	{
		GL11.glTexImage2D(3553, 0, 6408, width, height, 0, 6408, 5121, buffer);
	}

	public static void toGL3(ByteBuffer buffer, int glTex, int width, int height)
	{
		GL11.glTexImage2D(3553, 0, 6407, width, height, 0, 6407, 5121, buffer);
	}

	public static ByteBuffer toByteBuffer(int glTex)
	{
		int format = GLImageManager.getFormat(glTex);
		int width = GLImageManager.getWidth(glTex);
		int height = GLImageManager.getHeight(glTex);
		int channels = GLImageManager.getChannels(format);
		ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * channels);
		GL11.glGetTexImage(3553, 0, format, 5121, buffer);
		return buffer;
	}

	public static int getChannels(int format)
	{
		int channels = 4;
		if(format == 6407)
		{
			channels = 3;
		}
		return channels;
	}

	public static int getFormat(int glTex)
	{
		GL11.glBindTexture(3553, glTex);
		return GL11.glGetTexLevelParameteri(3553, 0, 4099);
	}

	public static int getHeight(int glTex)
	{
		return GL11.glGetTexLevelParameteri(3553, 0, 4097);
	}

	public static int getWidth(int glTex)
	{
		return GL11.glGetTexLevelParameteri(3553, 0, 4096);
	}
}

