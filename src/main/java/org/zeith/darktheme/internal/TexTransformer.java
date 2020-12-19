package org.zeith.darktheme.internal;

import org.zeith.darktheme.DarkThemeMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

@Mod.EventBusSubscriber(value = { Side.CLIENT })
public class TexTransformer
{
	static final List<ITextureTransformer> TRANSFORMERS = new ArrayList<>();
	static final List<ResourceLocation> forceLoad = new ArrayList<>();

	public static List<ITextureTransformer> getTransformers()
	{
		return TRANSFORMERS;
	}

	public static void addTransformer(ITextureTransformer trans)
	{
		if(!TRANSFORMERS.contains(trans))
		{
			TRANSFORMERS.add(trans);
		}
	}

	@SubscribeEvent
	public static void textureMapStitch(TextureStitchEvent.Pre e)
	{
		Minecraft.getMinecraft().addScheduledTask(() ->
		{
			Minecraft.getMinecraft().getTextureManager().mapTextureObjects.keySet().removeIf(texture -> texture.getNamespace().equals("darktheme") && texture.getPath().startsWith("textures/cache/"));
			URLTexBinder.urls.clear();
			URLTexBinder.loadedUrls.clear();
		});
	}

	public static void reloadTextures()
	{
		Minecraft.getMinecraft().addScheduledTask(() ->
		{
			forceLoad.clear();
			TextureManager mgr = Minecraft.getMinecraft().getTextureManager();
			mgr.mapTextureObjects.keySet().removeIf(tex ->
			{
				ITextureObject t = mgr.mapTextureObjects.get(tex);
				if(!(!DarkThemeMod.processTexture(tex) && !(t instanceof IFixedTex) || tex.getNamespace().equals("darktheme") && tex.getPath().startsWith("textures/cache/")))
				{
					if(DarkThemeMod.shouldReload(tex))
					{
						forceLoad.add(tex);
					}
					TextureUtil.deleteTexture(t.getGlTextureId());
					return true;
				}
				return false;
			});
			for(ResourceLocation fl : forceLoad)
			{
				mgr.bindTexture(fl);
			}
			DarkThemeMod.DISCOVERED_GUIS.clear();
			FontRenderer fr = Minecraft.getMinecraft().fontRenderer;
			if(fr instanceof GUIAwareFontRenderer)
			{
				((GUIAwareFontRenderer) fr).colorReplacements.clear();
				GUIAwareFontRenderer.handle.clear();
			}
		});
	}

	public static ITextureObject handle(ResourceLocation ntp, ITextureObject tex, TextureManager mgr)
	{
		if(!(tex instanceof IFixedTex))
		{
			int fixer = 0;
			for(ITextureTransformer texTrans : TRANSFORMERS)
			{
				if(!texTrans.affects(ntp)) continue;
				++fixer;
			}
			if(fixer > 0)
			{
				BufferedImage before = GLImageManager.toBufferedImage(tex.getGlTextureId());
				mgr.deleteTexture(ntp);
				FixedCachedTexture tex2 = new FixedCachedTexture(ntp, ITextureTransformer.merge(TRANSFORMERS), before);
				try
				{
					TextureUtil.deleteTexture(tex.getGlTextureId());
					tex2.loadTexture(Minecraft.getMinecraft().getResourceManager());
					tex = tex2;
				} catch(Throwable e)
				{
					e.printStackTrace();
				}
			}
		}
		return tex;
	}

	public static ITextureObject handle(ResourceLocation ntp, BufferedImage before, TextureManager mgr)
	{
		int fixer = 0;
		for(ITextureTransformer texTrans : TRANSFORMERS)
		{
			if(!texTrans.affects(ntp)) continue;
			++fixer;
		}
		if(fixer > 0)
		{
			FixedCachedTexture tex2 = new FixedCachedTexture(ntp, ITextureTransformer.merge(TRANSFORMERS), before);
			try
			{
				tex2.loadTexture(Minecraft.getMinecraft().getResourceManager());
			} catch(Throwable e)
			{
				e.printStackTrace();
			}
			return tex2;
		}
		return null;
	}

	public static class FixedCachedTexture
			extends AbstractTexture
			implements IFixedTex
	{
		final ResourceLocation texture;
		final ITextureTransformer proc;
		BufferedImage image;
		BufferedImage finalImage;

		public FixedCachedTexture(ResourceLocation texture, ITextureTransformer proc, BufferedImage image)
		{
			if(image == null)
			{
				image = new BufferedImage(16, 16, 2);
				Graphics2D g = image.createGraphics();
				g.setColor(Color.BLACK);
				g.fillRect(0, 0, 8, 8);
				g.fillRect(8, 8, 8, 8);
				g.setColor(Color.MAGENTA);
				g.fillRect(8, 0, 8, 8);
				g.fillRect(0, 8, 8, 8);
				g.dispose();
			}
			this.image = image;
			this.texture = texture;
			this.proc = proc;
		}

		public void loadTexture(IResourceManager rm)
		{
			this.finalImage = this.proc != null ? this.proc.handle(this.image, this.texture) : this.image;
			TextureUtil.uploadTextureImageAllocate(this.getGlTextureId(), this.finalImage, false, false);
		}

		@Override
		public BufferedImage getOriginal()
		{
			return this.image;
		}

		@Override
		public BufferedImage getFinal()
		{
			return this.finalImage;
		}

		@Override
		public ResourceLocation getPath()
		{
			return this.texture;
		}
	}

	public interface IFixedTex
	{
		BufferedImage getOriginal();

		BufferedImage getFinal();

		ResourceLocation getPath();
	}

	public interface ITextureTransformer
	{
		boolean affects(ResourceLocation var1);

		void transform(BufferedImage var1, ResourceLocation var2);

		default BufferedImage handle(BufferedImage src, ResourceLocation texture)
		{
			if(this.affects(texture))
			{
				BufferedImage dst = new BufferedImage(src.getWidth(), src.getHeight(), 2);
				Graphics2D g = dst.createGraphics();
				g.drawImage(src, 0, 0, dst.getWidth(), dst.getHeight(), null);
				g.dispose();
				this.transform(dst, texture);
				return dst;
			}
			return src;
		}

		static ITextureTransformer merge(Iterable<ITextureTransformer> all)
		{
			return ITextureTransformer.create(texture ->
			{
				for(ITextureTransformer child : all)
				{
					if(!child.affects(texture)) continue;
					return true;
				}
				return false;
			}, (src, texture) ->
			{
				for(ITextureTransformer child : all)
				{
					if(!child.affects(texture)) continue;
					child.transform(src, texture);
				}
			});
		}

		static ITextureTransformer create(final Predicate<ResourceLocation> validator, final BiConsumer<BufferedImage, ResourceLocation> transformer)
		{
			return new ITextureTransformer()
			{

				@Override
				public void transform(BufferedImage src, ResourceLocation texture)
				{
					transformer.accept(src, texture);
				}

				@Override
				public boolean affects(ResourceLocation texture)
				{
					return validator.test(texture);
				}
			};
		}
	}
}

