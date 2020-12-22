package org.zeith.darktheme.compat.creativecore;

import com.creativemd.creativecore.common.gui.client.style.ColoredDisplayStyle;
import com.creativemd.creativecore.common.gui.client.style.Style;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.zeith.darktheme.api.event.StylesReloadEvent;
import org.zeith.darktheme.compat.DarkCompat;
import org.zeith.darktheme.compat.DoDC;

import java.awt.*;

@DoDC("creativecore")
public class DarkCreativeCore
		extends DarkCompat
{
	public static Style liteStyle, liteStyleNoHighlight;

	@Override
	public void preInit()
	{
		liteStyle = Style.liteStyle;
		liteStyleNoHighlight = Style.liteStyleNoHighlight;

		MinecraftForge.EVENT_BUS.register(this);
	}

	@SubscribeEvent
	public void reloadStyles(StylesReloadEvent.Post e)
	{
		Color border = new Color(0, 0, 0);
		Color background = new Color(90, 90, 90);
		Color mouseOverBackground = new Color(100, 100, 100);
		Color face = new Color(198, 198, 198);
		Color disableEffect = new Color(0, 0, 0, 100);

		border = e.context.getColor(border);
		background = e.context.getColor(background);
		mouseOverBackground = e.context.getColor(mouseOverBackground);
		face = e.context.getColor(face);
		disableEffect = e.context.getColor(disableEffect);

		Style.liteStyle = new Style("defaultDark", new ColoredDisplayStyle(border.getRGB()), new ColoredDisplayStyle(background.getRGB()), new ColoredDisplayStyle(mouseOverBackground.getRGB()), new ColoredDisplayStyle(face.getRGB()), new ColoredDisplayStyle(disableEffect.getRGB()));
		Style.liteStyleNoHighlight = new Style("defaultDark", new ColoredDisplayStyle(border.getRGB()), new ColoredDisplayStyle(background.getRGB()), new ColoredDisplayStyle(background.getRGB()), new ColoredDisplayStyle(face.getRGB()), new ColoredDisplayStyle(disableEffect.getRGB()));
	}
}