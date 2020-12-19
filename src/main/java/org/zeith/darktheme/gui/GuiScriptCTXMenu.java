package org.zeith.darktheme.gui;

import org.zeith.darktheme.DarkThemeMod;
import org.zeith.darktheme.gui.util.RenderUtil;
import org.zeith.darktheme.internal.ds.DarkScript;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.init.SoundEvents;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GuiScriptCTXMenu
		extends GuiScreen
{
	final GuiScriptList parent;
	final float mx;
	final float my;
	int wX;
	int wY;
	int wWidth;
	int wHeight;
	List<String> lines = new ArrayList<String>();
	DarkScript manipul;

	public GuiScriptCTXMenu(GuiScriptList parent, float x, float y, DarkScript manipul)
	{
		this.parent = parent;
		this.mx = x;
		this.my = y;
		this.manipul = manipul;
	}

	@Override
	public void initGui()
	{
		super.initGui();
		this.parent.width = this.width;
		this.parent.height = this.height;
		this.parent.initGui();
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks)
	{
		this.parent.drawScreen(mouseX, mouseY, partialTicks);
		this.wWidth = 80;
		this.lines.clear();
		this.lines.add(this.manipul.isLoaded() ? "Disable" : "Enable");
		if(this.manipul.getOptFile() != null && !this.manipul.getOptFile().getName().equalsIgnoreCase("style.ds") && !this.manipul.getOptFile().getName().equalsIgnoreCase("style.ds.disabled"))
		{
			this.lines.add("Delete");
		}
		this.wHeight = (this.fontRenderer.FONT_HEIGHT + 1) * this.lines.size() + 2;
		this.wX = (int) (this.mx * (float) this.width) + 2;
		this.wY = (int) (this.my * (float) this.height) - 2;
		String t = this.manipul.getContext().getName();
		RenderUtil.drawVerticalGradientRect(this.wX, this.wY, this.wWidth, this.wHeight, -872415232, -872415232);
		int sw = this.fontRenderer.getStringWidth(t);
		RenderUtil.drawVerticalGradientRect(this.wX + (this.wWidth - sw) / 2 - 2, this.wY - this.fontRenderer.FONT_HEIGHT - 2, sw + 4, this.fontRenderer.FONT_HEIGHT + 2, -872415232, -872415232);
		this.drawCenteredString(this.fontRenderer, t, this.wX + this.wWidth / 2, this.wY - this.fontRenderer.FONT_HEIGHT - 1, -1);
		for(int i = 0; i < this.lines.size(); ++i)
		{
			int y = this.wY + 2 + (this.fontRenderer.FONT_HEIGHT + 1) * i;
			this.drawString(this.fontRenderer, this.lines.get(i), this.wX + (this.wWidth - this.fontRenderer.getStringWidth(this.lines.get(i))) / 2, y, mouseX >= this.wX && mouseX < this.wX + this.wWidth && mouseY >= y && mouseY < y + this.fontRenderer.FONT_HEIGHT + 1 ? -858993664 : -1);
		}
		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException
	{
		if(keyCode == 1)
		{
			this.mc.displayGuiScreen(this.parent);
		}
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
	{
		if(mouseX >= this.wX && mouseX < this.wX + this.wWidth && mouseY >= this.wY && mouseY < this.wY + this.wHeight)
		{
			for(int i = 0; i < this.lines.size(); ++i)
			{
				int y = this.wY + 2 + (this.fontRenderer.FONT_HEIGHT + 1) * i;
				if(mouseY < y || mouseY >= y + this.fontRenderer.FONT_HEIGHT + 1) continue;
				if(i == 0)
				{
					if(this.manipul.getOptFile() != null && this.manipul.getOptFile().isFile())
					{
						String path = this.manipul.getOptFile().getAbsolutePath();
						path = !path.endsWith(".disabled") ? path + ".disabled" : path.substring(0, path.length() - 9);
						this.manipul.getOptFile().renameTo(new File(path));
					}
					this.manipul.unload(DarkThemeMod.UNDO_CONTEXT);
					this.mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0f));
					this.mc.displayGuiScreen(this.parent);
					new Thread(() ->
					{
						DarkThemeMod.loadStyles(true);
						GuiScriptList gtl = new GuiScriptList(this.parent.parentScreen);
						this.mc.displayGuiScreen(gtl);
						gtl.scroll = this.parent.scroll;
						gtl.prevScroll = this.parent.prevScroll;
						gtl.scrollDelta = this.parent.scrollDelta;
					}).start();
					continue;
				}
				if(i != 1) continue;
				if(this.manipul.getOptFile() != null && this.manipul.getOptFile().isFile())
				{
					this.manipul.getOptFile().delete();
				}
				this.manipul.unload(DarkThemeMod.UNDO_CONTEXT);
				this.mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0f));
				this.mc.displayGuiScreen(this.parent);
				new Thread(() ->
				{
					DarkThemeMod.loadStyles(true);
					GuiScriptList gtl = new GuiScriptList(this.parent.parentScreen);
					this.mc.displayGuiScreen(gtl);
					gtl.scroll = this.parent.scroll;
					gtl.prevScroll = this.parent.prevScroll;
					gtl.scrollDelta = this.parent.scrollDelta;
				}).start();
			}
		} else
		{
			this.mc.displayGuiScreen(this.parent);
		}
		super.mouseClicked(mouseX, mouseY, mouseButton);
	}

	@Override
	public void updateScreen()
	{
		this.parent.updateScreen();
	}
}

