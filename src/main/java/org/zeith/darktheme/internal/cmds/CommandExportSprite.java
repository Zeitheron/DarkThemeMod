package org.zeith.darktheme.internal.cmds;

import org.zeith.darktheme.DarkThemeMod;
import org.zeith.darktheme.internal.GLImageManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
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
import java.util.stream.Collectors;

public class CommandExportSprite
		extends CommandBase
{
    @Override
	public String getName()
	{
		return "darktheme:exporttxms";
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
		TextureAtlasSprite tas;
		boolean all;
		TextureMap txm;
		ITextureObject obj;
		File export;
		ResourceLocation loc;
		File exports = new File(DarkThemeMod.modConfigDir, "exports");
		if(!exports.isDirectory())
		{
			exports.mkdirs();
		}
		if(args.length > 1)
		{
			loc = new ResourceLocation(args[0]);
			export = new File(exports, loc.getNamespace() + "." + loc.getPath().replace('/', '_') + File.separator + args[1].replaceAll(":", "_") + ".png");
			obj = Minecraft.getMinecraft().getTextureManager().mapTextureObjects.get(loc);
			if(!(obj instanceof TextureMap))
			{
				throw new CommandException("Could not find texture map reference for \"" + loc + "\"!");
			}
			txm = (TextureMap) obj;
			all = args[1].equals("*");
			tas = txm.mapUploadedSprites.get(args[1]);
			if(!all && tas == null)
			{
				throw new CommandException("Could not find sprite reference for \"" + args[1] + "\"!");
			}
		} else
		{
			throw new CommandException("Please specify texture to export (use tab to view all loaded textures)");
		}
		Minecraft.getMinecraft().addScheduledTask(() ->
		{
			BufferedImage image = GLImageManager.toBufferedImage(obj.getGlTextureId());
			if(all)
			{
				for(TextureAtlasSprite s : txm.mapUploadedSprites.values())
				{
					File xp = new File(exports, loc.getNamespace() + "." + loc.getPath().replace('/', '_') + File.separator + s.getIconName().replaceAll(":", "_") + ".png");
					BufferedImage spr = image.getSubimage(Math.round(s.getMinU() * (float) image.getWidth()), Math.round(s.getMinV() * (float) image.getHeight()), Math.round((s.getMaxU() - s.getMinU()) * (float) image.getWidth()), Math.round((s.getMaxV() - s.getMinV()) * (float) image.getHeight()));
					new Thread(() ->
					{
						try
						{
							xp.mkdirs();
							xp.delete();
							xp.createNewFile();
							ImageIO.write(spr, "png", xp);
							sender.sendMessage(new TextComponentString(TextFormatting.DARK_GREEN + "Texture sprite \"" + s.getIconName() + "\" exported and saved!"));
						} catch(IOException e)
						{
							sender.sendMessage(new TextComponentString(TextFormatting.DARK_RED + "Failed to save texture sprite: " + e.getMessage() + "! See logs for more info."));
							e.printStackTrace();
						}
					}, "ExportTexture" + loc.hashCode()).start();
				}
			} else
			{
				BufferedImage spr = image.getSubimage(Math.round(tas.getMinU() * (float) image.getWidth()), Math.round(tas.getMinV() * (float) image.getHeight()), Math.round((tas.getMaxU() - tas.getMinU()) * (float) image.getWidth()), Math.round((tas.getMaxV() - tas.getMinV()) * (float) image.getHeight()));
				new Thread(() ->
				{
					try
					{
						export.mkdirs();
						export.delete();
						export.createNewFile();
						ImageIO.write(spr, "png", export);
						sender.sendMessage(new TextComponentString(TextFormatting.DARK_GREEN + "Texture sprite exported and saved!"));
					} catch(IOException e)
					{
						sender.sendMessage(new TextComponentString(TextFormatting.DARK_RED + "Failed to save texture sprite: " + e.getMessage() + "! See logs for more info."));
						e.printStackTrace();
					}
				}, "ExportTexture" + loc.hashCode()).start();
			}
		});
	}

	@Override
	public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos targetPos)
	{
		if(args.length == 1)
		{
			return CommandExportSprite.getListOfStringsMatchingLastWord(args, Minecraft.getMinecraft().getTextureManager().mapTextureObjects.keySet().stream().filter(path -> Minecraft.getMinecraft().getTextureManager().getTexture(path) instanceof TextureMap).collect(Collectors.toList()));
		}
		if(args.length == 2)
		{
			ITextureObject obj = Minecraft.getMinecraft().getTextureManager().mapTextureObjects.get(new ResourceLocation(args[0]));
			if(!(obj instanceof TextureMap))
			{
				return Collections.emptyList();
			}
			TextureMap txm = (TextureMap) obj;
			return CommandExportSprite.getListOfStringsMatchingLastWord(args, txm.mapUploadedSprites.keySet());
		}
		return Collections.emptyList();
	}
}

