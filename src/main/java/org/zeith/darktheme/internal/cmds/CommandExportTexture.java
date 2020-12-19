package org.zeith.darktheme.internal.cmds;

import com.google.common.base.Joiner;
import org.zeith.darktheme.DarkThemeMod;
import org.zeith.darktheme.internal.GLImageManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class CommandExportTexture
		extends CommandBase
{
	@Override
	public String getName()
	{
		return "darktheme:exporttex";
	}

	@Override
	public String getUsage(ICommandSender sender)
	{
		return "";
	}

	@Override
	public boolean checkPermission(MinecraftServer server, ICommandSender sender)
	{
		return true;
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
	{
		ITextureObject obj;
		File export;
		ResourceLocation loc;
		File exports = new File(DarkThemeMod.modConfigDir, "exports");
		if(!exports.isDirectory())
		{
			exports.mkdirs();
		}
		if(args.length > 0)
		{
			loc = new ResourceLocation(Joiner.on(' ').join(args));
			export = new File(exports, loc.getNamespace() + "." + loc.getPath().replace('/', '_') + ".png");
			obj = Minecraft.getMinecraft().getTextureManager().mapTextureObjects.get(loc);
			if(obj == null)
			{
				throw new CommandException("Could not find texture reference for \"" + loc + "\"!");
			}
		} else
		{
			throw new CommandException("Please specify texture to export (use tab to view all loaded textures)");
		}
		Minecraft.getMinecraft().addScheduledTask(() ->
		{
			BufferedImage image = GLImageManager.toBufferedImage(obj.getGlTextureId());
			new Thread(() ->
			{
				try
				{
					export.createNewFile();
					ImageIO.write(image, "png", export);
					sender.sendMessage(new TextComponentString(TextFormatting.DARK_GREEN + "Texture exported and saved!"));
				} catch(IOException e)
				{
					sender.sendMessage(new TextComponentString(TextFormatting.DARK_RED + "Failed to save texture: " + e.getMessage() + "! See logs for more info."));
					e.printStackTrace();
				}
			}, "ExportTexture" + loc.hashCode()).start();
		});
	}

    @Override
	public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos targetPos)
	{
		if(args.length == 1)
		{
			return CommandExportTexture.getListOfStringsMatchingLastWord(args, Minecraft.getMinecraft().getTextureManager().mapTextureObjects.keySet());
		}
		return CommandExportTexture.getListOfStringsMatchingLastWord(args, Collections.emptyList());
	}
}

