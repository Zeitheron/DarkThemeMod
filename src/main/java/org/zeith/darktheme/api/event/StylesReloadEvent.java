package org.zeith.darktheme.api.event;

import net.minecraftforge.fml.common.eventhandler.Event;

public class StylesReloadEvent
		extends Event
{
	public static class Post
			extends StylesReloadEvent
	{
	}

	public static class Pre
			extends StylesReloadEvent
	{
	}
}