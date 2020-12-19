package org.zeith.darktheme.compat.jei;

import org.zeith.darktheme.DarkThemeMod;
import org.zeith.darktheme.api.event.StylesReloadEvent;
import org.zeith.darktheme.compat.DarkCompat;
import org.zeith.darktheme.compat.DoDC;
import org.zeith.darktheme.internal.FinalFieldHelper;
import org.zeith.darktheme.internal.data.TxMapSprite;
import mezz.jei.JustEnoughItems;
import mezz.jei.startup.ProxyCommonClient;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.lang.reflect.Field;
import java.util.List;

@DoDC(value = "jei")
public class DarkJEI
		extends DarkCompat
{
	@Override
	public void preInit()
	{
		DarkThemeMod.LOG.info("Starting JEI compat...");
		MinecraftForge.EVENT_BUS.register((Object) this);
		try
		{
			Object textureMap = null;
			for(Field f : ProxyCommonClient.class.getDeclaredFields())
			{
				if(!f.getName().equals("textureMap")) continue;
				FinalFieldHelper.setFinalField(f, JustEnoughItems.getProxy(), new FixedJEITextureMap("textures"));
				return;
			}
		} catch(ReflectiveOperationException e)
		{
			System.err.println("Failed JEI integration:");
			e.printStackTrace();
		}
	}

	@SubscribeEvent
	public void reset(StylesReloadEvent.Post e)
	{
		FixedJEITextureMap.dirty = true;
		FixedJEITextureMap.excludeSprites.clear();
		List<TxMapSprite> sprites = DarkThemeMod.TXMAP_EXCLUDES.get(new ResourceLocation("jei:textures"));
		if(sprites != null && !sprites.isEmpty())
		{
			for(TxMapSprite s : sprites)
			{
				FixedJEITextureMap.excludeSprites.add(s.spriteName);
			}
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void stitch(TextureStitchEvent.Pre e)
	{
		FixedJEITextureMap.origin = null;
	}
}

