package org.zeith.darktheme.api.event;

import net.minecraftforge.fml.common.eventhandler.Event;

import java.awt.*;
import java.util.function.Function;

public class StylesReloadEvent
		extends Event
{
	public static class Post
			extends StylesReloadEvent
	{
		public final StylesContext context;

		public Post(StylesContext context)
		{
			this.context = context;
		}
	}

	public static class Pre
			extends StylesReloadEvent
	{
	}

	public static class StylesContext
	{
		public final Function<Integer, Integer> colorHandler;

		public StylesContext(Function<Integer, Integer> colorHandler)
		{
			this.colorHandler = colorHandler;
		}

		public int getColor(int rgb)
		{
			return colorHandler.apply(rgb);
		}

		public Color getColor(Color rgb)
		{
			return new Color(colorHandler.apply(rgb.getRGB()));
		}
	}
}