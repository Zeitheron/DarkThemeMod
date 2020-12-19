package org.zeith.darktheme.gui;

import com.google.common.base.Joiner;
import org.zeith.darktheme.gui.util.RenderUtil;
import org.zeith.darktheme.gui.util.Scissors;
import org.zeith.darktheme.internal.ScriptBrowser;
import org.zeith.darktheme.internal.URLTexBinder;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.text.TextFormatting;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class GuiScriptBrowser
		extends GuiScreen
{
	GuiScriptList parentScreen;
	List<ScriptBrowser.FetchableScript> scripts;
	List<ScriptBrowser.FetchableScript> scriptsAll = ScriptBrowser.stylesList;
	int scroll;
	int prevScroll;
	int scrollDelta;
	GuiTextField search;
	static final Joiner comma = Joiner.on(", ");

	public GuiScriptBrowser(GuiScriptList parentScreen)
	{
		this.parentScreen = parentScreen;
	}

	public void updateSearch()
	{
		ArrayList<ScriptBrowser.FetchableScript> scripts = new ArrayList<>(this.scriptsAll);
		String query = this.search != null ? this.search.getText().trim() : "";
		if(!query.isEmpty())
			for(String word : query.toLowerCase().split(" "))
				scripts.removeIf(ds -> !ds.name.toLowerCase().contains(word) && !ds.description.toLowerCase().contains(word));
		this.scripts = scripts;
	}

	@Override
	public void initGui()
	{
		this.updateSearch();
		this.buttonList.clear();
		Keyboard.enableRepeatEvents(true);
		Mouse.getDWheel();
		String[] texts = new String[]{
				I18n.format("gui.back"),
				I18n.format("selectServer.refresh"),
				"Submit script..."
		};
		int bw = (this.width - 32 - texts.length * 2) / texts.length;
		for(int i = 0; i < texts.length; ++i)
		{
			this.addButton(new GuiButton(i, 16 + i * (bw + 2), this.height - 22, bw, 20, texts[i]));
		}
		String text = this.search != null ? this.search.getText() : "";
		this.search = new GuiTextField(0, this.fontRenderer, 2, 2, this.width - 4, 18);
		this.search.setText(text);
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks)
	{
		this.drawBackground(0);
		float cscroll = (float) this.prevScroll + (float) (this.scroll - this.prevScroll) * this.mc.getRenderPartialTicks();
		Scissors.begin();
		Scissors.scissor(0, 22, this.width, this.height - 48);
		for(int i = 0; i < this.scripts.size(); ++i)
		{
			float y = (float) (32 + i * 42) - cscroll;
			if(y <= -62.0f) continue;
			if(y >= (float) (this.height - 24)) break;
			boolean hover = mouseX >= 16 && mouseX < this.width - 32 && (float) mouseY >= y && (float) mouseY < y + 40.0f && mouseY < this.height - 24;
			RenderUtil.drawVerticalGradientRect(16.0f, y, this.width - 32, 40.0f, (hover ? 238 : 136) << 24, 0);
			RenderUtil.drawVerticalGradientRect(8.0f, y, 8.0f, 40.0f, 0, (hover ? 238 : 136) << 24);
			ScriptBrowser.FetchableScript ds = this.scripts.get(i);
			this.fontRenderer.drawString((ds.installed() ? TextFormatting.GREEN : TextFormatting.WHITE).toString() + ds.name, 60.0f, y + 2.0f, -1, true);
			int j = -1;
			for(String ln : ds.description.split("\n", 2))
			{
				this.fontRenderer.drawString(ln, 60.0f, y - 1.0f + (float) this.fontRenderer.FONT_HEIGHT * 1.5f + (float) (++j * this.fontRenderer.FONT_HEIGHT), -10066330, true);
			}
			GlStateManager.pushMatrix();
			GlStateManager.translate(60.0f, y + 38.0f, 0.0f);
			GlStateManager.scale(0.5f, 0.5f, 0.5f);
			this.fontRenderer.drawString("Author" + (ds.authors.size() > 1 ? "s" : "") + ": " + comma.join(ds.authors) + "; File: \"" + ds.file + "\"", 0.0f, (float) (-this.fontRenderer.FONT_HEIGHT), -12303292, true);
			GlStateManager.popMatrix();
			GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
			if(ds.icon == null || ds.icon.isEmpty() || !URLTexBinder.bindURL(ds.icon, 144)) continue;
			RenderUtil.drawFullRectangleFit(18.0, y + 2.0f, 36.0, 36.0);
		}
		Scissors.end();
		RenderUtil.drawHorizontalGradientRect(0.0f, 22.0f, this.width, 32.0f, -1442840576, 0);
		RenderUtil.drawHorizontalGradientRect(0.0f, this.height - 24 - 32, this.width, 32.0f, 0, -1442840576);
		RenderUtil.drawVerticalGradientRect(0.0f, 22.0f, 32.0f, this.height - 24, -1442840576, 0);
		RenderUtil.drawVerticalGradientRect(this.width - 32, 22.0f, 32.0f, this.height - 24, 0, -1442840576);
		this.search.drawTextBox();
		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException
	{
		if(this.search.textboxKeyTyped(typedChar, keyCode))
		{
			this.updateSearch();
			return;
		}
		if(keyCode == 1)
		{
			this.mc.displayGuiScreen(this.parentScreen);
			Keyboard.enableRepeatEvents(false);
		}
		if(keyCode == 208)
			this.scrollDelta += 20;
		if(keyCode == 200)
			this.scrollDelta -= 20;
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
	{
		if(mouseButton == 1 && mouseX >= this.search.x && mouseX < this.search.x + this.search.width && mouseY >= this.search.y && mouseY < this.search.y + this.search.height)
		{
			this.search.setText("");
			this.updateSearch();
		}
		if(this.search.mouseClicked(mouseX, mouseY, mouseButton))
		{
			return;
		}
		super.mouseClicked(mouseX, mouseY, mouseButton);
		float cscroll = (float) this.prevScroll + (float) (this.scroll - this.prevScroll) * this.mc.getRenderPartialTicks();
		for(int i = 0; i < this.scripts.size(); ++i)
		{
			float y = (float) (32 + i * 42) - cscroll;
			if(y <= -62.0f) continue;
			if(y >= (float) (this.height - 24)) break;
			if(mouseX < 16 || mouseX >= this.width - 32 || !((float) mouseY >= y) || !((float) mouseY < y + 40.0f) || mouseY >= this.height - 24)
				continue;
			this.mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0f));
			this.mc.displayGuiScreen(new GuiScriptCTXMenu2(this, (float) mouseX / (float) this.width, (float) mouseY / (float) this.height, this.scripts.get(i)));
		}
	}

	@Override
	protected void actionPerformed(GuiButton button) throws IOException
	{
		if(button.id == 0)
		{
			this.mc.displayGuiScreen(this.parentScreen);
			Keyboard.enableRepeatEvents(false);
		} else if(button.id == 1)
		{
			button.enabled = false;
			this.scripts = new ArrayList<>(this.scripts);
			new Thread(() ->
			{
				ScriptBrowser.reload();
				this.scripts = ScriptBrowser.stylesList;
				this.updateSearch();
				button.enabled = true;
			}, "DarkThemeModThemeBrowser").start();
		} else if(button.id == 2)
		{
			try
			{
				Desktop.getDesktop().browse(new URL("https://github.com/Zeitheron/DarkScriptThemes/issues/new").toURI());
			} catch(URISyntaxException e)
			{
				e.printStackTrace();
			}
		}
	}

	@Override
	public void updateScreen()
	{
		this.prevScroll = this.scroll;
		int dw = Mouse.getDWheel();
		if(dw != 0)
		{
			this.scrollDelta += -dw / 5;
		}
		this.scroll = Math.max(0, this.scroll + this.scrollDelta);
		this.scrollDelta /= 2;
		while(this.scroll > 0 && 32 + (this.scripts.size() - 1) * 42 - this.scroll < this.height - 74)
		{
			--this.scroll;
		}
	}
}

