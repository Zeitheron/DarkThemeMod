package org.zeith.darktheme.internal.cmds;

import com.google.common.base.Joiner;
import org.zeith.darktheme.DarkThemeMod;
import org.zeith.darktheme.internal.ds.DarkCompiler;
import org.zeith.darktheme.internal.ds.DarkScript;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.zip.GZIPOutputStream;

public class CommandCompile
		extends CommandBase
{
	@Override
	public String getName()
	{
		return "darktheme:compile";
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
		block18:
		{
			File script = new File(DarkThemeMod.darkScripts, Joiner.on(' ').join(args));
			if(script.isFile())
			{
				File output;
				sender.sendMessage(new TextComponentString("Compiling..."));
				String path = script.getAbsolutePath();
				if(path.endsWith(".ds"))
				{
					path = path.substring(0, path.length() - 2) + "cds";
				}
				if(!(output = new File(path)).isFile())
				{
					try(GZIPOutputStream out = new GZIPOutputStream(new FileOutputStream(output)))
					{
						DarkScript ds = new DarkScript(script);
						ds.parse(false, null, null);
						DarkCompiler.compile(ds, out);
						sender.sendMessage(new TextComponentString(TextFormatting.DARK_GREEN + "Compilation succeeded!"));
						break block18;
					} catch(IOException ioe)
					{
						sender.sendMessage(new TextComponentString(TextFormatting.DARK_RED + "Compilation failed!"));
						throw new CommandException(ioe.getMessage());
					}
				}
				sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "Error: target file already present."));
			} else
			{
				throw new CommandException("Unable to find file '" + Joiner.on(' ').join(args) + "'!");
			}
		}
	}

    @Override
	public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos targetPos)
	{
		if(args.length == 1)
		{
			File[] fs = DarkThemeMod.darkScripts.listFiles(f -> f.getName().endsWith(".ds"));
			ArrayList ss = new ArrayList();
			Arrays.stream(fs).map(File::getName).forEach(ss::add);
			return CommandCompile.getListOfStringsMatchingLastWord(args, ss);
		}
		return CommandCompile.getListOfStringsMatchingLastWord(args, Collections.emptyList());
	}
}