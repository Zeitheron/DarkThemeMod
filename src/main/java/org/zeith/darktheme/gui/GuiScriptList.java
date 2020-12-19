package org.zeith.darktheme.gui;

import org.zeith.darktheme.DarkThemeMod;
import org.zeith.darktheme.gui.util.RenderUtil;
import org.zeith.darktheme.gui.util.Scissors;
import org.zeith.darktheme.internal.ScriptBrowser;
import org.zeith.darktheme.internal.URLTexBinder;
import org.zeith.darktheme.internal.ds.DarkScript;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class GuiScriptList
		extends GuiScreen
{
	GuiScreen parentScreen;
	List<DarkScript> scripts;
	List<DarkScript> scriptsAll;
	int scroll;
	int prevScroll;
	int scrollDelta;
	String showDwnFromBrowser;
	GuiTextField search;

	public GuiScriptList(GuiScreen parentScreen)
	{
		this.parentScreen = parentScreen;
	}

	public void updateSearch()
	{
		String query = this.search != null ? this.search.getText().trim() : "";
		ArrayList<DarkScript> scripts = new ArrayList<>(this.scriptsAll);
		if(!query.isEmpty())
		{
			for(String word : query.toLowerCase().split(" "))
			{
				scripts.removeIf(ds ->
				{
					DarkScript.ModifyContext ctx = ds.getContext();
					return !ctx.getName().toLowerCase().contains(word) && !ctx.getDescription().toLowerCase().contains(word);
				});
			}
		}
		this.scripts = scripts;
	}

	@Override
	public void initGui()
	{
		this.buttonList.clear();
		this.scriptsAll = new ArrayList<>(DarkThemeMod.styles);
		this.scriptsAll.sort(Comparator.comparing(a -> a.getContext().getName()));

		Keyboard.enableRepeatEvents(true);
		Mouse.getDWheel();

		DarkScript dds = null;
		for(DarkScript def : this.scriptsAll)
		{
			if(def.getOptFile() == null || !def.getOptFile().getName().equalsIgnoreCase("style.ds") && !def.getOptFile().getName().equalsIgnoreCase("style.ds.disabled"))
				continue;
			dds = def;
			break;
		}
		int gind = -1;
		if(dds != null)
		{
			this.scriptsAll.remove(dds);
			this.scriptsAll.add(++gind, dds);
		}
		if(DarkThemeMod.resourcePackScript != null)
		{
			this.scriptsAll.remove(DarkThemeMod.resourcePackScript);
			this.scriptsAll.add(++gind, DarkThemeMod.resourcePackScript);
		}
		this.updateSearch();
		String[] texts = new String[]{
				I18n.format("gui.back"),
				"Browse...",
				I18n.format("selectServer.refresh"),
				"Open Folder"
		};
		int bw = (this.width - 32 - texts.length * 2) / texts.length;
		for(int i = 0; i < texts.length; ++i)
		{
			this.addButton(new GuiButton(i, 16 + i * (bw + 2), this.height - 22, bw, 20, texts[i]));
		}
		String text = this.search != null ? this.search.getText() : "";
		this.search = new GuiTextField(0, this.fontRenderer, this.width / 2 + 2, 2, this.width / 2 - 4, 18);
		this.search.setText(text);
		this.addButton(new GuiButton(100, 1, 1, this.width / 4 - 2, 20, "Disable all"));
		this.addButton(new GuiButton(101, this.width / 4 + 1, 1, this.width / 4 - 2, 20, "Enable all"));
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
			Boolean valid;
			float y = (float) (32 + i * 42) - cscroll;
			if(y <= -62.0f) continue;
			if(y >= (float) (this.height - 24)) break;
			DarkScript ds;
			try
			{
				ds = this.scripts.get(i);
			} catch(Throwable err)
			{
				break;
			}
			boolean hover = mouseX >= 16 && mouseX < this.width - 32 && (float) mouseY >= y && (float) mouseY < y + 40.0f && mouseY < this.height - 24 && mouseY >= 22;
			RenderUtil.drawVerticalGradientRect(16.0f, y, this.width - 32, 40.0f, (hover ? 238 : 136) << 24 | 0, 0);
			RenderUtil.drawVerticalGradientRect(8.0f, y, 8.0f, 40.0f, 0, (hover ? 238 : 136) << 24 | 0);
			DarkScript.ModifyContext ctx = ds.getContext();
			this.fontRenderer.drawString((ds.isLoaded() ? TextFormatting.DARK_GREEN : TextFormatting.DARK_RED).toString() + ctx.getName(), 60.0f, y + 2.0f, -1, true);
			int j = -1;
			for(String ln : (ctx.getDescription() + "").split("\n", 2))
			{
				this.fontRenderer.drawString(ln, 60.0f, y - 1.0f + (float) this.fontRenderer.FONT_HEIGHT * 1.5f + (float) (++j * this.fontRenderer.FONT_HEIGHT), -10066330, true);
			}
			GlStateManager.pushMatrix();
			GlStateManager.translate(60.0f, y + 38.0f, 0.0f);
			GlStateManager.scale(0.5f, 0.5f, 0.5f);
			File of = ds.getOptFile();
			this.fontRenderer.drawString("Author" + (ctx.getAuthors().size() > 1 ? "s" : "") + ": " + GuiScriptBrowser.comma.join(ctx.getAuthors()) + "; File: \"" + (of != null ? of.getName() : "???") + "\"; Version: " + ctx.getVersion(), 0.0f, (float) (-this.fontRenderer.FONT_HEIGHT), -12303292, true);
			GlStateManager.popMatrix();
			GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
			if(ctx.getIcon() != null && !ctx.getIcon().isEmpty() && URLTexBinder.bindURL(ctx.getIcon(), 144))
			{
				RenderUtil.drawFullRectangleFit(18.0, y + 2.0f, 36.0, 36.0);
			}
			if((valid = ds.isVerified()) == null) continue;
			boolean hov = mouseX >= 18 && (float) mouseY >= y + 2.0f && mouseX < 54 && (float) mouseY < y + 40.0f && mouseY < this.height - 24 && mouseY >= 22;
			this.mc.getTextureManager().bindTexture(new ResourceLocation("forge", "textures/gui/version_check_icons.png"));
			GlStateManager.pushMatrix();
			GlStateManager.translate(46.0f, y + 30.0f, 0.0f);
			GlStateManager.scale(0.25f, 0.0625f, 1.0f);
			GlStateManager.color(1.0f, valid ? 1.0f : 0.0f, valid ? 1.0f : 0.0f, 1.0f);
			RenderUtil.drawTexturedModalRect(0.0, 0.0, 96.0, hov ? 128.0 : 0.0, 32.0, 128.0);
			GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
			GlStateManager.popMatrix();
			if(!hov) continue;
			this.showDwnFromBrowser = valid ? "Downloaded from Style Browser" : "Corrupted, downloaded from Style Browser\nClick to fix";
		}
		Scissors.end();
		RenderUtil.drawHorizontalGradientRect(0.0f, 22.0f, this.width, 32.0f, -1442840576, 0);
		RenderUtil.drawHorizontalGradientRect(0.0f, this.height - 24 - 32, this.width, 32.0f, 0, -1442840576);
		RenderUtil.drawVerticalGradientRect(0.0f, 22.0f, 32.0f, this.height - 24, -1442840576, 0);
		RenderUtil.drawVerticalGradientRect(this.width - 32, 22.0f, 32.0f, this.height - 24, 0, -1442840576);
		this.search.drawTextBox();
		super.drawScreen(mouseX, mouseY, partialTicks);
		if(this.showDwnFromBrowser != null)
		{
			this.drawHoveringText(Arrays.asList(this.showDwnFromBrowser.split("\n")), mouseX, mouseY);
			this.showDwnFromBrowser = null;
		}
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
		{
			this.scrollDelta += 20;
		}
		if(keyCode == 200)
		{
			this.scrollDelta -= 20;
		}
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
			Boolean valid;
			float y = (float) (32 + i * 42) - cscroll;
			if(y <= -62.0f) continue;
			if(y >= (float) (this.height - 24)) break;
			DarkScript ds = this.scripts.get(i);
			if(mouseX >= 18 && (float) mouseY >= y + 2.0f && mouseX < 54 && (float) mouseY < y + 40.0f && mouseY < this.height - 24 && mouseY >= 22 && (valid = ds.isVerified()) != null && !valid.booleanValue())
			{
				this.mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0f));
				ScriptBrowser.FetchableScript fs = ScriptBrowser.getByBid(ds.getContext().getBrowserId());
				if(fs == null) continue;
				fs.download(true);
				this.initGui();
				continue;
			}
			if(mouseX < 16 || mouseX >= this.width - 32 || !((float) mouseY >= y) || !((float) mouseY < y + 40.0f) || mouseY >= this.height - 24 || mouseY < 22)
				continue;
			this.mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0f));
			this.mc.displayGuiScreen(new GuiScriptCTXMenu(this, (float) mouseX / (float) this.width, (float) mouseY / (float) this.height, ds));
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
			this.mc.displayGuiScreen(new GuiScriptBrowser(this));
		} else if(button.id == 2)
		{
			button.enabled = false;
			new Thread(() ->
			{
				DarkThemeMod.loadStyles(true);
				this.mc.addScheduledTask(this::initGui);
				button.enabled = true;
			}).start();
		} else if(button.id == 3)
		{
			OpenGlHelper.openFile(DarkThemeMod.darkScripts);
		} else if(button.id == 100)
		{
			button.enabled = false;
			new Thread(() ->
			{
				for(DarkScript manipul : this.scripts)
				{
					if(manipul.getOptFile() == null || !manipul.getOptFile().isFile()) continue;
					String path = manipul.getOptFile().getAbsolutePath();
					if(!path.endsWith(".disabled"))
					{
						path = path + ".disabled";
					}
					manipul.getOptFile().renameTo(new File(path));
				}
				DarkThemeMod.loadStyles(true);
				this.mc.addScheduledTask(this::initGui);
				button.enabled = true;
			}, "DarkThemeMod-DISABLEALL").start();
		} else if(button.id == 101)
		{
			button.enabled = false;
			new Thread(() ->
			{
				for(DarkScript manipul : this.scripts)
				{
					if(manipul.getOptFile() == null || !manipul.getOptFile().isFile()) continue;
					String path = manipul.getOptFile().getAbsolutePath();
					if(path.endsWith(".disabled"))
						path = path.substring(0, path.length() - 9);
					manipul.getOptFile().renameTo(new File(path));
				}
				DarkThemeMod.loadStyles(true);
				this.mc.addScheduledTask(this::initGui);
				button.enabled = true;
			}, "DarkThemeMod-ENABLEALL").start();
		}
	}

	@Override
	public void updateScreen()
	{
		this.prevScroll = this.scroll;
		int dw = Mouse.getDWheel();
		if(dw != 0)
			this.scrollDelta += -dw / 5;
		this.scroll = Math.max(0, this.scroll + this.scrollDelta);
		this.scrollDelta /= 2;
		while(this.scroll > 0 && 32 + (this.scripts.size() - 1) * 42 - this.scroll < this.height - 74)
			--this.scroll;
	}
}

