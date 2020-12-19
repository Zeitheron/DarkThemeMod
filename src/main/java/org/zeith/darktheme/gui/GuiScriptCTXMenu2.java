package org.zeith.darktheme.gui;

import org.zeith.darktheme.gui.util.RenderUtil;
import org.zeith.darktheme.internal.ScriptBrowser;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.init.SoundEvents;

import java.io.IOException;

public class GuiScriptCTXMenu2
		extends GuiScreen
{
	final GuiScriptBrowser parent;
	final float mx;
	final float my;
	int wX;
	int wY;
	int wWidth;
	int wHeight;
	ScriptBrowser.FetchableScript manipul;

	public GuiScriptCTXMenu2(GuiScriptBrowser parent, float x, float y, ScriptBrowser.FetchableScript manipul)
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
		this.wHeight = 12;
		this.wX = (int) (this.mx * (float) this.width) + 2;
		this.wY = (int) (this.my * (float) this.height) + 2;
		boolean hov = mouseX >= this.wX && mouseX < this.wX + this.wWidth && mouseY >= this.wY && mouseY < this.wY + this.wHeight;
		RenderUtil.drawVerticalGradientRect(this.wX, this.wY, this.wWidth, this.wHeight, -872415232, -872415232);
		String text = "Download";
		this.drawString(this.fontRenderer, "Download", this.wX + (this.wWidth - this.fontRenderer.getStringWidth(text)) / 2, this.wY + 2, hov ? -858993664 : -1);
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
			this.mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0f));
			this.mc.displayGuiScreen(this.parent);
			new Thread(() ->
			{
				this.manipul.download(true);
				GuiScriptList gtl = this.parent.parentScreen = new GuiScriptList(this.parent.parentScreen.parentScreen);
				gtl.scroll = this.parent.scroll;
				gtl.prevScroll = this.parent.prevScroll;
				gtl.scrollDelta = this.parent.scrollDelta;
			}).start();
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

