package org.zeith.darktheme.internal.cmds;

import org.zeith.darktheme.DarkThemeMod;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;

public class CommandReloadStyles
		extends CommandBase
{
	@Override
	public String getName()
	{
		return "darktheme:reload_styles";
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
		sender.sendMessage(new TextComponentString("Reloading DarkScripts..."));
		DarkThemeMod.loadStyles(true);
		sender.sendMessage(new TextComponentString("DarkScripts reloaded!"));
	}
}

