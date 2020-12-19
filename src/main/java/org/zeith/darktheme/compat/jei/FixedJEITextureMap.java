package org.zeith.darktheme.compat.jei;

import org.zeith.darktheme.DarkThemeMod;
import org.zeith.darktheme.api.IFixedTxMap;
import org.zeith.darktheme.internal.GLImageManager;
import org.zeith.darktheme.internal.data.Rectangle2F;
import org.zeith.darktheme.internal.data.TxMapSprite;
import mezz.jei.gui.textures.JeiTextureMap;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FixedJEITextureMap
		extends JeiTextureMap
		implements IFixedTxMap
{
	public static final Set<String> excludeSprites = new HashSet<>();
	public static BufferedImage origin;
	public static boolean dirty;

	public FixedJEITextureMap(String basePathIn)
	{
		super(basePathIn);
	}

	@Override
	public void tick()
	{
		super.tick();
		if(origin == null)
		{
			origin = GLImageManager.toBufferedImage(this.getGlTextureId());
			List<TxMapSprite> sprites = DarkThemeMod.TXMAP_EXCLUDES.get(this.getLocation());
			if(sprites != null && !sprites.isEmpty())
			{
				for(TxMapSprite s : sprites)
				{
					this.addExclude(s.spriteName);
				}
			}
			dirty = true;
		}
		if(dirty)
		{
			ArrayList<Rectangle2F> excludes = new ArrayList<Rectangle2F>();
			for(String e : excludeSprites)
			{
				TextureAtlasSprite tas = this.mapUploadedSprites.get(e);
				if(tas == null) continue;
				excludes.add(new Rectangle2F(tas.getMinU(), tas.getMinV(), tas.getMaxU() - tas.getMinU(), tas.getMaxV() - tas.getMinV()));
			}
			BufferedImage copyOfOrigin = new BufferedImage(origin.getWidth(), origin.getHeight(), 2);
			int[] src = ((DataBufferInt) origin.getRaster().getDataBuffer()).getData();
			int[] dst = ((DataBufferInt) copyOfOrigin.getRaster().getDataBuffer()).getData();
			System.arraycopy(src, 0, dst, 0, src.length);
			DarkThemeMod.fixGlTexture(this.getGlTextureId(), this.getLocation(), copyOfOrigin, excludes.toArray(new Rectangle2F[excludes.size()]));
			dirty = false;
		}
	}

	@Override
	public void addExclude(String spriteName)
	{
		excludeSprites.add(spriteName);
		dirty = true;
	}

	@Override
	public void removeExclude(String spriteName)
	{
		excludeSprites.remove(spriteName);
		dirty = true;
	}
}