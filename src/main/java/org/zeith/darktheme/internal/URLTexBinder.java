package org.zeith.darktheme.internal;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class URLTexBinder
{
	static final List<String> urls = new ArrayList<String>();
	static final List<String> loadedUrls = new ArrayList<String>();

	public static boolean bindURL(String url)
	{
		return URLTexBinder.bindURL(url, null);
	}

	public static boolean bindURL(String url, Integer maxTexSize)
	{
		if(!urls.contains(url))
		{
			new Thread(() ->
			{
				BufferedImage image = null;
				for(int attempt = 0; attempt < 3; ++attempt)
				{
					try(InputStream input = HttpUtil.openCached(url);)
					{
						int ms;
						image = ImageIO.read(input);
						if(image == null || maxTexSize == null || (ms = Math.max(image.getWidth(), image.getHeight())) <= maxTexSize)
							break;
						float val = maxTexSize.floatValue() / (float) ms;
						int nw = Math.round((float) image.getWidth() * val);
						int nh = Math.round((float) image.getHeight() * val);
						BufferedImage nimg = new BufferedImage(nw, nh, 2);
						Graphics2D g = nimg.createGraphics();
						g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
						g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
						g.drawImage(image, 0, 0, nw, nh, null);
						g.dispose();
						image = nimg;
						break;
					} catch(IOException e)
					{
						System.out.println("Attempt " + (attempt + 1) + "/3 of downloading " + url + " failed.");
						continue;
					}
				}
				if(image == null)
				{
					image = new BufferedImage(2, 2, 2);
					image.setRGB(0, 0, -5111553);
					image.setRGB(1, 1, -5111553);
					image.setRGB(0, 1, -16777216);
					image.setRGB(1, 0, -16777216);
				}
				ResourceLocation path = new ResourceLocation("darktheme", "textures/cache/" + MD5.encrypt(url) + ".png");
				TexTransformer.FixedCachedTexture obj = new TexTransformer.FixedCachedTexture(path, null, image);
				Minecraft.getMinecraft().addScheduledTask(() ->
				{
					obj.loadTexture(Minecraft.getMinecraft().getResourceManager());
					Minecraft.getMinecraft().getTextureManager().mapTextureObjects.put(path, obj);
					loadedUrls.add(url);
					if(!urls.contains(url))
					{
						urls.add(url);
					}
				});
			}).start();
			if(!urls.contains(url))
			{
				urls.add(url);
			}
			return false;
		}
		if(loadedUrls.contains(url))
		{
			Minecraft.getMinecraft().getTextureManager().bindTexture(new ResourceLocation("darktheme", "textures/cache/" + MD5.encrypt(url) + ".png"));
			return true;
		}
		return false;
	}

	public static void loadTexture(ResourceLocation path, BufferedImage image, Consumer<TexTransformer.FixedCachedTexture> whenDone)
	{
		TexTransformer.FixedCachedTexture obj = new TexTransformer.FixedCachedTexture(path, null, image);
		Minecraft.getMinecraft().addScheduledTask(() ->
		{
			obj.loadTexture(Minecraft.getMinecraft().getResourceManager());
			Minecraft.getMinecraft().getTextureManager().mapTextureObjects.put(path, obj);
			if(whenDone != null)
			{
				whenDone.accept(obj);
			}
		});
	}
}

