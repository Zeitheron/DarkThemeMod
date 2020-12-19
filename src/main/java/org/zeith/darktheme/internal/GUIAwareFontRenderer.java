package org.zeith.darktheme.internal;

import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanArrayMap;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.settings.GameSettings;
import org.zeith.darktheme.DarkThemeMod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class GUIAwareFontRenderer
		extends FontRenderer
{
	static final Map<String, Class<?>> CLASS_MAP = new HashMap();
	public final FontRenderer parent;
	static final Object2BooleanArrayMap<Class> handle = new Object2BooleanArrayMap();
	public final Int2IntMap colorReplacements = new Int2IntArrayMap();

	public GUIAwareFontRenderer(GameSettings gameSettingsIn, FontRenderer parent)
	{
		super(gameSettingsIn, parent.locationFontTexture, parent.renderEngine, parent.unicodeFlag);
		this.parent = parent;
	}

	public int handleColor(int color)
	{
		Class<?> cl;
		int _bef;
		block22:
		{
			_bef = color;
			color = DarkThemeMod.stripAlpha(color);
			cl = null;
			StackTraceElement[] es = Thread.currentThread().getStackTrace();
			block2:
			for(int i = 0; i < es.length; ++i)
			{
				StackTraceElement e = es[i];
				if(!e.getClassName().equals(getClass().getName())) continue;
				for(int j = i; j < es.length; ++j)
				{
					Class<?> cs;
					e = es[j];
					String c = e.getClassName();
					if(!CLASS_MAP.containsKey(c))
					{
						try
						{
							CLASS_MAP.put(c, Class.forName(c));
							DarkThemeMod.LOG_FILE.println("Detected new text stack trace class: " + c);
						} catch(ClassNotFoundException err)
						{
							CLASS_MAP.put(c, null);
						}
					}
					if((cs = CLASS_MAP.get(c)) == null || !Gui.class.isAssignableFrom(cs)) continue;
					cl = cs;
					break block2;
				}
			}

			if(!handle.containsKey(cl) && cl != null)
			{
				if(DarkThemeMod.DISCOVERED_GUIS.contains(cl))
				{
					handle.put(cl, true);
				} else
				{
					for(Predicate<String> val : DarkThemeMod.GUI_CLASS_BLACKLIST)
					{
						if(val.test(cl.getName()))
						{
							handle.put(cl, false);
							return _bef;
						}
					}

					for(Predicate<String> val : DarkThemeMod.GUI_CLASS_WHITELIST)
					{
						if(val.test(cl.getName()))
						{
							handle.put(cl, true);
						}
						break block22;
					}
					handle.put(cl, false);
				}
			}
		}
		if(handle.getBoolean(cl))
		{
			if(this.colorReplacements.containsKey(color))
			{
				color = this.colorReplacements.get(color);
			} else
			{
				int col = DarkThemeMod.TEXT_COLOR_MAP.getOrDefault(color, color);
				DarkThemeMod.LOG_FILE.println("Detected new font color #" + Integer.toHexString(color) + " in " + cl + ". Replaced to #" + Integer.toHexString(col));
				this.colorReplacements.put(color, col);
				color = col;
			}
		}
		return DarkThemeMod.handleAlpha(_bef, color);
	}

	public void drawSplitString(String str, int x, int y, int wrapWidth, int textColor)
	{
		textColor = this.handleColor(textColor);
		this.parent.drawSplitString(str, x, y, wrapWidth, textColor);
	}

	public int drawString(String text, float x, float y, int color, boolean dropShadow)
	{
		color = this.handleColor(color);
		return this.parent.drawString(text, x, y, color, dropShadow);
	}

	public int drawString(String text, int x, int y, int color)
	{
		color = this.handleColor(color);
		return this.parent.drawString(text, x, y, color);
	}

	public int drawStringWithShadow(String text, float x, float y, int color)
	{
		color = this.handleColor(color);
		return this.parent.drawStringWithShadow(text, x, y, color);
	}

	public boolean getBidiFlag()
	{
		return this.parent.getBidiFlag();
	}

	public int getCharWidth(char character)
	{
		return this.parent.getCharWidth(character);
	}

	public int getColorCode(char character)
	{
		return this.parent.getColorCode(character);
	}

	public int getStringWidth(String text)
	{
		return this.parent.getStringWidth(text);
	}

	public boolean getUnicodeFlag()
	{
		return this.parent.getUnicodeFlag();
	}

	public int getWordWrappedHeight(String str, int maxLength)
	{
		return this.parent.getWordWrappedHeight(str, maxLength);
	}

	public List<String> listFormattedStringToWidth(String str, int wrapWidth)
	{
		return this.parent.listFormattedStringToWidth(str, wrapWidth);
	}

	public void onResourceManagerReload(IResourceManager resourceManager)
	{
		this.parent.onResourceManagerReload(resourceManager);
	}

	public void setBidiFlag(boolean bidiFlagIn)
	{
		this.parent.setBidiFlag(bidiFlagIn);
	}

	public void setUnicodeFlag(boolean unicodeFlagIn)
	{
		this.parent.setUnicodeFlag(unicodeFlagIn);
	}

	public String trimStringToWidth(String text, int width)
	{
		return this.parent.trimStringToWidth(text, width);
	}

	public String trimStringToWidth(String text, int width, boolean reverse)
	{
		return this.parent.trimStringToWidth(text, width, reverse);
	}

	public FontRenderer getParent()
	{
		return this.parent;
	}
}

