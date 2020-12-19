package org.zeith.darktheme.internal.ds;

import net.minecraft.util.ResourceLocation;
import org.zeith.darktheme.internal.MD5;
import org.zeith.darktheme.internal.MapMath;
import org.zeith.darktheme.internal.ScriptBrowser;
import org.zeith.darktheme.internal.data.*;
import org.zeith.darktheme.internal.math.ExpressionEvaluator;
import org.zeith.darktheme.internal.math.functions.ExpressionFunction;
import org.zeith.darktheme.json.JSONArray;
import org.zeith.darktheme.json.JSONTokener;

import javax.annotation.Nullable;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

public class DarkScript
{
	private ModifyContext context = new ModifyContext(-1L);
	final Supplier<List<DarkLine>> code;
	File optFile;
	String md5;
	boolean loaded;
	boolean verified;
	public static final char VAR_GET = '$';

	public DarkScript(File code)
	{
		this.optFile = code;
		this.code = () ->
		{
			try
			{
				String extention = code.getName();
				if(extention.endsWith(".disabled"))
				{
					extention = extention.substring(0, extention.length() - 9);
				}
				extention = extention.substring(extention.lastIndexOf(46));
				this.md5 = MD5.getMD5Checksum(this.optFile);
				if(!extention.equalsIgnoreCase(".cds"))
				{
					List<String> list = Files.readAllLines(code.toPath());
					return DarkScript.parse(list.toArray(new String[list.size()]));
				}
				try(GZIPInputStream input = new GZIPInputStream(new FileInputStream(this.optFile)))
				{
					List<DarkLine> list = DarkCompiler.decompile(input);
					return list;
				}
			} catch(IOException ioe)
			{
				throw new RuntimeException(ioe);
			}
		};
	}

	public DarkScript(Supplier<List<String>> code)
	{
		this.code = () ->
		{
			List<String> list = code.get();
			return DarkScript.parse(list.toArray(new String[list.size()]));
		};
	}

	public DarkScript(String... code)
	{
		this.code = () -> DarkScript.parse(code);
	}

	@Nullable
	public Boolean isVerified()
	{
		UUID bid = this.context.getBrowserId();
		if(bid == null)
		{
			return null;
		}
		ScriptBrowser.FetchableScript fs = ScriptBrowser.getByBid(bid);
		if(fs != null)
		{
			return Objects.equals(fs.md5, this.md5);
		}
		return null;
	}

	public DarkScript parse(Consumer<ModifyContext> undoCtx) throws RuntimeException
	{
		return this.parse(false, null, undoCtx);
	}

	public DarkScript parse(boolean load, Consumer<ModifyContext> applyCtx, Consumer<ModifyContext> undoCtx) throws RuntimeException
	{
		if(undoCtx != null)
		{
			this.unload(undoCtx);
		}
		this.context = DarkScript.createContext(this.code.get());
		if(this.context.id == null && this.optFile != null)
		{
			this.context.id = this.optFile.getAbsolutePath();
		}
		if(this.context.name == null && this.optFile != null)
		{
			this.context.name = this.optFile.getName();
		}
		if(this.context.description == null)
		{
			this.context.description = "No description provided";
		}
		if(this.context.authors == null)
		{
			this.context.authors = new StringArrayList(Arrays.asList("Unknown"));
		}
		if(load && applyCtx != null)
		{
			this.load(applyCtx);
		}
		return this;
	}

	public boolean unload(Consumer<ModifyContext> undoCtx)
	{
		if(this.context != null && this.loaded)
		{
			undoCtx.accept(this.context);
			this.loaded = false;
			return true;
		}
		return false;
	}

	public boolean load(Consumer<ModifyContext> applyCtx)
	{
		if(!this.loaded)
		{
			applyCtx.accept(this.context);
			this.loaded = true;
			return true;
		}
		return false;
	}

	public boolean isLoaded()
	{
		return this.loaded;
	}

	public File getOptFile()
	{
		return this.optFile;
	}

	public long getVersion()
	{
		return this.getContext().getVersion();
	}

	public ModifyContext getContext()
	{
		return this.context;
	}

	public static ModifyContext createContext(List<DarkLine> lines) throws RuntimeException
	{
		boolean includeSources = false;
		int icn = -1;
		ModifyContext ctx = null;
		for(DarkLine pln : lines)
		{
			TextureRectangle data;
			Point pt;
			Predicate pred;
			if(pln == null || pln.function.ignoreDeserialize())
			{
				if(pln == null || pln.function != LineFunction.INCLUDE_SOURCES) continue;
				if(icn == -1)
				{
					includeSources = true;
					icn = pln.lineNumber;
					continue;
				}
				throw new RuntimeException("Line #" + pln.lineNumber + ": duplicate 'include sources' instruction.");
			}
			int i = pln.lineNumber;
			if(pln.function == LineFunction.VERSION)
			{
				ctx = new ModifyContext((Long) pln.data);
				ctx.lines.add(pln);
				continue;
			}
			if(ctx == null)
			{
				throw new RuntimeException("The first line of script must start with version!");
			}
			ctx.lines.add(pln);
			if(pln.function == LineFunction.EXCLUDE)
			{
				pred = (Predicate) pln.data;
				if(pred == null)
				{
					throw new RuntimeException("Line #" + i + ": unrecognized predicate for \"" + pln.context + "\"");
				}
				ctx.exclusions.add(pred);
				continue;
			}
			if(pln.function == LineFunction.INCLUDE)
			{
				pred = (Predicate) pln.data;
				if(pred == null)
				{
					throw new RuntimeException("Line #" + i + ": unrecognized predicate for \"" + pln.context + "\"");
				}
				ctx.inclusions.add(pred);
				continue;
			}
			if(pln.function == LineFunction.FORCE_RELOAD)
			{
				pred = (Predicate) pln.data;
				if(pred == null)
				{
					throw new RuntimeException("Line #" + i + ": unrecognized predicate for \"" + pln.context + "\"");
				}
				ctx.forceReloads.add(pred);
				continue;
			}
			if(pln.function == LineFunction.COLOR)
			{
				pt = (Point) pln.data;
				ctx.colors.put(pt.x, pt.y);
				continue;
			}
			if(pln.function == LineFunction.TEXT_COLOR)
			{
				pt = (Point) pln.data;
				ctx.textColors.put(pt.x, pt.y);
				continue;
			}
			if(pln.function == LineFunction.AREA_EXCLUDE)
			{
				data = (TextureRectangle) pln.data;
				MapMath.add(ctx.areaExclude, data.getTex(), data.getRect());
				continue;
			}
			if(pln.function == LineFunction.AREA_INCLUDE)
			{
				data = (TextureRectangle) pln.data;
				MapMath.add(ctx.areaInclude, data.getTex(), data.getRect());
				continue;
			}
			if(pln.function == LineFunction.ID)
			{
				if(ctx.id != null)
				{
					throw new RuntimeException("Line #" + i + ": duplicate 'id' instruction.");
				}
				ctx.id = (String) pln.data;
				continue;
			}
			if(pln.function == LineFunction.NAME)
			{
				if(ctx.name != null)
				{
					throw new RuntimeException("Line #" + i + ": duplicate 'name' instruction.");
				}
				ctx.name = (String) pln.data;
				continue;
			}
			if(pln.function == LineFunction.MOD_DEPENDENCY)
			{
				if(ctx.dependencies != null)
				{
					throw new RuntimeException("Line #" + i + ": duplicate 'require mod' instruction.");
				}
				ctx.dependencies = (StringArrayList) pln.data;
				continue;
			}
			if(pln.function == LineFunction.AUTHORS)
			{
				if(ctx.authors != null)
				{
					throw new RuntimeException("Line #" + i + ": duplicate 'authors' instruction.");
				}
				ctx.authors = (StringArrayList) pln.data;
				continue;
			}
			if(pln.function == LineFunction.ICON)
			{
				if(ctx.icon != null)
				{
					throw new RuntimeException("Line #" + i + ": duplicate 'icon' instruction.");
				}
				ctx.icon = (String) pln.data;
				continue;
			}
			if(pln.function == LineFunction.DESCRIPTION)
			{
				if(ctx.description != null)
				{
					throw new RuntimeException("Line #" + i + ": duplicate 'icon' instruction.");
				}
				ctx.description = (String) pln.data;
				continue;
			}
			if(pln.function == LineFunction.BROWSER_ID)
			{
				if(ctx.browserId != null)
				{
					throw new RuntimeException("Line #" + i + ": duplicate 'bid' instruction.");
				}
				ctx.browserId = (UUID) pln.data;
				continue;
			}
			if(pln.function == LineFunction.TEXT_GUI)
			{
				Pattern pat = makePattern((String) pln.data);
				ctx.textReplaceClasses.add(s -> pat.matcher(s).matches());
				continue;
			}
			if(pln.function == LineFunction.PARTIAL_TEX_COL)
			{
				PartialTexColor ptc = (PartialTexColor) pln.data;
				MapMath.add(ctx.partials, ptc.path, ptc);
				continue;
			}
			if(pln.function == LineFunction.TEXT_COLOR_EXCLUDE)
			{
				Pattern pat = makePattern((String) pln.data);
				ctx.textBlacklistClasses.add(s -> pat.matcher(s).matches());
				continue;
			}
			if(pln.function == LineFunction.FILL_COLOR)
			{
				ColoredRectangle cr = (ColoredRectangle) pln.data;
				MapMath.add(ctx.fills, cr.tex, cr);
				continue;
			}
			if(pln.function == LineFunction.RENDER_TEX)
			{
				RenderRectangle rr = (RenderRectangle) pln.data;
				MapMath.add(ctx.renders, rr.dstTex, rr);
				continue;
			}
			if(pln.function != LineFunction.EXCLUDE_SPRITE) continue;
			ctx.spritesExclude.add((TxMapSprite) pln.data);
		}
		if(includeSources)
		{
			ctx.lines.add(new DarkLine(null, LineFunction.INCLUDE_SOURCES, icn));
		}
		return ctx;
	}

	public static Pattern makePattern(String val)
	{
		StringBuilder npat = new StringBuilder(val);
		for(char c : val.toCharArray())
		{
			if(c == '?') npat.append('.');
			else if(c == '*') npat.append(".*");
			else npat.append('[').append(c).append(']');
		}
		return Pattern.compile(npat.toString());
	}

	public static List<DarkLine> parse(String[] lines) throws RuntimeException
	{
		DarkVars vars = new DarkVars();
		ArrayList<DarkLine> dls = new ArrayList<DarkLine>();
		int i = 0;
		for(String line : lines)
		{
			++i;
			DarkLine pln = null;
			try
			{
				pln = DarkScript.handleLine(line, i, vars);
			} catch(RuntimeException e)
			{
				throw new RuntimeException("Line #" + i + ": " + e.getMessage(), e);
			}
			dls.add(pln);
		}
		return dls;
	}

	public static DarkLine handleLine(String line, int ln, DarkVars vars)
	{
		if(line.endsWith(";"))
		{
			line = line.substring(0, line.length() - 1);
		}
		for(String key : vars.keySet())
		{
			while(line.contains('$' + key))
			{
				String b4 = line;
				line = line.replace('$' + key, vars.get(key));
			}
		}
		if(line.startsWith("version "))
		{
			return new DarkLine(Long.parseLong(line.substring(8)), LineFunction.VERSION, ln);
		}
		if(line.equalsIgnoreCase("include sources"))
		{
			return new DarkLine(null, LineFunction.INCLUDE_SOURCES, ln);
		}
		if(line.startsWith("exclude "))
		{
			String toExclude = line.split(" ", 2)[1];
			return new DarkLine(new StringedPredicate<ResourceLocation>(DarkScript.readResourcePredicate(toExclude), toExclude), LineFunction.EXCLUDE, ln).setContext(toExclude);
		}
		if(line.startsWith("force reload "))
		{
			String toExclude = line.split(" ", 3)[2];
			return new DarkLine(new StringedPredicate<ResourceLocation>(DarkScript.readResourcePredicate(toExclude), toExclude), LineFunction.FORCE_RELOAD, ln).setContext(toExclude);
		}
		if(line.startsWith("include "))
		{
			String toInclude = line.split(" ", 2)[1];
			return new DarkLine(new StringedPredicate<ResourceLocation>(DarkScript.readResourcePredicate(toInclude), toInclude), LineFunction.INCLUDE, ln).setContext(toInclude);
		}
		if(line.startsWith("color "))
		{
			String[] sub2 = line.split(" ", 2)[1].split(" -> ");
			if(sub2.length != 2)
			{
				return null;
			}
			for(int i = 0; i < sub2.length; ++i)
			{
				if(sub2[i].startsWith("#"))
				{
					sub2[i] = sub2[i].substring(1);
					continue;
				}
				if(!sub2[i].startsWith("0x")) continue;
				sub2[i] = sub2[i].substring(2);
			}
			int src = Integer.parseInt(sub2[0], 16);
			int dst = Integer.parseInt(sub2[1], 16);
			return new DarkLine(new Point(src, dst), LineFunction.COLOR, ln);
		}
		if(line.startsWith("text "))
		{
			String sln = line.substring(5);
			if(sln.startsWith("color "))
			{
				String[] sub = sln.split(" ", 2)[1].split(" -> ");
				if(sub.length != 2)
				{
					return null;
				}
				for(int i = 0; i < sub.length; ++i)
				{
					if(sub[i].startsWith("#"))
					{
						sub[i] = sub[i].substring(1);
						continue;
					}
					if(!sub[i].startsWith("0x")) continue;
					sub[i] = sub[i].substring(2);
				}
				int src = Integer.parseInt(sub[0], 16);
				int dst = Integer.parseInt(sub[1], 16);
				return new DarkLine(new Point(src, dst), LineFunction.TEXT_COLOR, ln);
			}
			if(sln.startsWith("transform in "))
			{
				return new DarkLine(sln.substring(13), LineFunction.TEXT_GUI, ln);
			}
		} else if(line.startsWith("in \""))
		{
			ResourceLocation tex;
			String[] args;
			String tpl = line.substring(3);
			if(tpl.contains("\" exclude rect"))
			{
				args = tpl.split("\" exclude rect");
				tex = new ResourceLocation(new JSONTokener(args[0] + "\"").nextValue().toString());
				String rectString = "rect" + args[1];
				Rectangle2F base = DarkScript.readRectangle(rectString);
				return new DarkLine(new TextureRectangle(tex, base), LineFunction.AREA_EXCLUDE, ln);
			}
			if(tpl.contains("\" include rect"))
			{
				args = tpl.split("\" include rect");
				tex = new ResourceLocation(new JSONTokener(args[0] + "\"").nextValue().toString());
				String rectString = "rect" + args[1];
				Rectangle2F base = DarkScript.readRectangle(rectString);
				return new DarkLine(new TextureRectangle(tex, base), LineFunction.AREA_INCLUDE, ln);
			}
			if(tpl.contains("\" color "))
			{
				args = tpl.split("\" color ");
				tex = new ResourceLocation(new JSONTokener(args[0] + "\"").nextValue().toString());
				String[] sub3 = args[1].split(" -> ");
				if(sub3.length != 2)
				{
					return null;
				}
				for(int i = 0; i < sub3.length; ++i)
				{
					if(sub3[i].startsWith("#"))
					{
						sub3[i] = sub3[i].substring(1);
						continue;
					}
					if(!sub3[i].startsWith("0x")) continue;
					sub3[i] = sub3[i].substring(2);
				}
				int src = Integer.parseInt(sub3[0], 16);
				int dst = Integer.parseInt(sub3[1], 16);
				return new DarkLine(new PartialTexColor(tex, new Rectangle2F(0.0f, 0.0f, 1.0f, 1.0f), new Point(src, dst)), LineFunction.PARTIAL_TEX_COL, ln);
			}
			if(tpl.contains("\" fill color "))
			{
				args = tpl.split("\" fill color ", 2);
				tex = new ResourceLocation(new JSONTokener(args[0] + "\"").nextValue().toString());
				if((args = args[1].split(" within "))[0].startsWith("#"))
				{
					args[0] = args[0].substring(1);
				} else if(args[0].startsWith("0x"))
				{
					args[0] = args[0].substring(2);
				}
				int color = Integer.parseInt(args[0], 16);
				Rectangle2F rect = DarkScript.readRectangle(args[1]);
				return new DarkLine(new ColoredRectangle(tex, rect, color), LineFunction.FILL_COLOR, ln);
			}
		} else if(line.startsWith("in rect"))
		{
			if(line.contains(" of \""))
			{
				String sub2 = line.substring(2);
				String[] subs = sub2.split(" of ", 2);
				Rectangle2F rect = DarkScript.readRectangle(subs[0]);
				subs = subs[1].split("\" color #");
				ResourceLocation tex = new ResourceLocation(new JSONTokener(subs[0] + "\"").nextValue().toString());
				subs = ("#" + subs[1]).split(" -> ");
				if(subs.length != 2)
				{
					return null;
				}
				for(int i = 0; i < subs.length; ++i)
				{
					if(subs[i].startsWith("#"))
					{
						subs[i] = subs[i].substring(1);
						continue;
					}
					if(!subs[i].startsWith("0x")) continue;
					subs[i] = subs[i].substring(2);
				}
				int src = Integer.parseInt(subs[0], 16);
				int dst = Integer.parseInt(subs[1], 16);
				return new DarkLine(new PartialTexColor(tex, rect, new Point(src, dst)), LineFunction.PARTIAL_TEX_COL, ln);
			}
		} else
		{
			if(line.startsWith("in txmap \"") && line.contains("\" exclude sprite \"") && line.endsWith("\""))
			{
				String[] data = line.split("\"");
				return new DarkLine(new TxMapSprite(new ResourceLocation(data[1]), data[3]), LineFunction.EXCLUDE_SPRITE, ln);
			}
			if(line.startsWith("id "))
			{
				return new DarkLine(line.split(" ", 2)[1], LineFunction.ID, ln);
			}
			if(line.startsWith("bid "))
			{
				return new DarkLine(UUID.fromString(line.split(" ", 2)[1]), LineFunction.BROWSER_ID, ln);
			}
			if(line.startsWith("name "))
			{
				return new DarkLine(new JSONTokener(line.split(" ", 2)[1]).nextValue().toString(), LineFunction.NAME, ln);
			}
			if(line.startsWith("authors "))
			{
				ArrayList<String> list = new ArrayList<String>();
				for(Object o : (JSONArray) new JSONTokener(line.split(" ", 2)[1]).nextValue())
				{
					list.add(o.toString());
				}
				return new DarkLine(new StringArrayList(list), LineFunction.AUTHORS, ln);
			}
			if(line.startsWith("require mod"))
			{
				boolean expectMany = false;
				String sub = line.substring(11);
				if(sub.startsWith("s"))
				{
					sub = sub.substring(1);
					expectMany = true;
				}
				Object parsed = new JSONTokener(sub).nextValue();
				ArrayList<String> mods = new ArrayList<String>();
				if(expectMany)
				{
					for(Object v : (JSONArray) parsed)
					{
						mods.add(v.toString());
					}
				} else
				{
					mods.add(parsed.toString());
				}
				return new DarkLine(new StringArrayList(mods), LineFunction.MOD_DEPENDENCY, ln);
			}
			if(line.startsWith("icon "))
			{
				return new DarkLine(new JSONTokener(line.split(" ", 2)[1]).nextValue().toString(), LineFunction.ICON, ln);
			}
			if(line.startsWith("description "))
			{
				return new DarkLine(new JSONTokener(line.split(" ", 2)[1]).nextValue().toString(), LineFunction.DESCRIPTION, ln);
			}
			if(line.startsWith("#"))
			{
				return new DarkLine(line.substring(1).trim(), LineFunction.COMMENT, ln);
			}
			if(line.trim().isEmpty())
			{
				return new DarkLine(null, LineFunction.WHITESPACE, ln);
			}
			if(line.startsWith("prevent text transform in "))
			{
				return new DarkLine(line.substring(26), LineFunction.TEXT_COLOR_EXCLUDE, ln);
			}
			if(line.startsWith("const "))
			{
				String[] dat = line.substring(6).split(": ", 2);
				vars.put(dat[0], dat[1]);
			} else if(line.startsWith("from "))
			{
				String dat = line.substring(5);
				boolean aa = false;
				boolean fq = false;
				while(dat.contains("gfx:render=fancy"))
				{
					fq = true;
					dat = dat.replace("gfx:render=fancy", "");
				}
				while(dat.contains("gfx:render=fast"))
				{
					if(fq)
					{
						throw new RuntimeException("gfx:render already set to 'fancy'!");
					}
					dat = dat.replace("gfx:render=fast", "");
				}
				while(dat.contains("gfx:aa=on"))
				{
					aa = true;
					dat = dat.replace("gfx:aa=on", "");
				}
				while(dat.contains("gfx:aa=off"))
				{
					if(aa)
					{
						throw new RuntimeException("gfx:aa already set to 'on'!");
					}
					dat = dat.replace("gfx:aa=off", "");
				}
				if(dat.startsWith("rect") && line.contains(") of \""))
				{
					Rectangle2F within;
					ResourceLocation dstTex;
					String[] sub4 = dat.split(" of \"", 2);
					Rectangle2F rect = DarkScript.readRectangle(sub4[0]);
					sub4[1] = "\"" + sub4[1];
					sub4 = sub4[1].split(" render to ", 2);
					ResourceLocation srcTex = new ResourceLocation(new JSONTokener(sub4[0]).nextValue().toString());
					if(sub4[1].contains(" within "))
					{
						sub4 = sub4[1].split(" within ");
						dstTex = new ResourceLocation(new JSONTokener(sub4[0]).nextValue().toString());
						within = DarkScript.readRectangle(sub4[1]);
					} else
					{
						dstTex = new ResourceLocation(new JSONTokener(sub4[1]).nextValue().toString());
						within = new Rectangle2F(0.0f, 0.0f, 1.0f, 1.0f);
					}
					return new DarkLine(new RenderRectangle(srcTex, dstTex, rect, within, RenderRectangle.create(aa, fq)), LineFunction.RENDER_TEX, ln);
				}
			}
		}
		return null;
	}

	static Rectangle2F readRectangle(String str)
	{
		block12:
		{
			if((str = str.trim()).startsWith("rect") && str.endsWith(")"))
			{
				int div = 1;
				String subDiv = str.substring(4);
				if(!(subDiv = subDiv.substring(0, subDiv.indexOf(40))).isEmpty())
				{
					if(subDiv.toLowerCase().startsWith("x"))
					{
						div = Integer.parseInt(subDiv.substring(1));
						str = str.replace("rect" + subDiv, "rect");
					} else
					{
						throw new RuntimeException("Unable to decode " + subDiv + " in " + str);
					}
				}
				try
				{
					String[] tokens = str.substring(5, str.length() - 1).split(",");
					float x = (float) ExpressionEvaluator.evaluateDouble(tokens[0], new ExpressionFunction[0]) / (float) div;
					float y = (float) ExpressionEvaluator.evaluateDouble(tokens[1], new ExpressionFunction[0]) / (float) div;
					float w = (float) ExpressionEvaluator.evaluateDouble(tokens[2], new ExpressionFunction[0]) / (float) div;
					float h = (float) ExpressionEvaluator.evaluateDouble(tokens[3], new ExpressionFunction[0]) / (float) div;
					if(x < 0.0f || x > 1.0f)
					{
						throw new RuntimeException("X is out of [0;1] bounds in " + str + "!");
					}
					if(y < 0.0f || y > 1.0f)
					{
						throw new RuntimeException("Y is out of [0;1] bounds in " + str + "!");
					}
					if(w <= 0.0f || w > 1.0f)
					{
						throw new RuntimeException("Width is out of (0;1) bounds in " + str + "!");
					}
					if(h <= 0.0f || h > 1.0f)
					{
						throw new RuntimeException("Height is out of (0;1) bounds in " + str + "!");
					}
					if(x + w < 0.0f || x + w > 1.0f)
					{
						throw new RuntimeException("X+W is out of [0;1] bounds in " + str + "!");
					}
					if(y + h < 0.0f || y + h > 1.0f)
					{
						throw new RuntimeException("Y+H is out of [0;1] bounds in " + str + "!");
					}
					return new Rectangle2F(x, y, w, h);
				} catch(Throwable err)
				{
					if(!(err instanceof RuntimeException)) break block12;
					throw (RuntimeException) err;
				}
			}
		}
		throw new RuntimeException("Unable to parse " + str);
	}

	static Predicate<ResourceLocation> readResourcePredicate(String toExclude)
	{
		if(toExclude.startsWith("*:"))
		{
			if(toExclude.endsWith("*"))
			{
				String ctn = toExclude.substring(2, toExclude.length() - 1);
				return texture -> texture.getPath().contains(ctn);
			}
			String ctn = toExclude.substring(2);
			return texture -> texture.getPath().equals(ctn);
		}
		if(toExclude.endsWith(":*"))
		{
			String ctn = toExclude.substring(0, toExclude.length() - 2);
			return texture -> texture.getNamespace().equals(ctn);
		}
		if(toExclude.startsWith("*") && toExclude.endsWith("*"))
		{
			String ctn = toExclude.substring(1, toExclude.length() - 1);
			return texture -> texture.toString().contains(ctn);
		}
		if(toExclude.startsWith("*"))
		{
			String ctn = toExclude.substring(1);
			return texture -> texture.toString().endsWith(ctn);
		}
		if(toExclude.endsWith("*"))
		{
			String ctn = toExclude.substring(0, toExclude.length() - 1);
			return texture -> texture.toString().startsWith(ctn);
		}
		return texture -> texture.toString().equals(toExclude);
	}

	public static class DarkLine
	{
		public final Object data;
		public final LineFunction function;
		public final int lineNumber;
		String context;

		public DarkLine(Object data, LineFunction func, int lineNumber)
		{
			this.data = data;
			this.function = func;
			this.lineNumber = lineNumber;
		}

		DarkLine setContext(String context)
		{
			this.context = context;
			return this;
		}
	}

	public static class ModifyContext
	{
		final List<DarkLine> lines = new ArrayList<DarkLine>();
		public final List<DarkLine> lineView = Collections.unmodifiableList(this.lines);
		final long version;
		final List<Predicate<ResourceLocation>> exclusions = new ArrayList<Predicate<ResourceLocation>>();
		final List<Predicate<ResourceLocation>> inclusions = new ArrayList<Predicate<ResourceLocation>>();
		final List<Predicate<ResourceLocation>> forceReloads = new ArrayList<Predicate<ResourceLocation>>();
		final Map<Integer, Integer> colors = new HashMap<Integer, Integer>();
		final Map<Integer, Integer> textColors = new HashMap<Integer, Integer>();
		final Map<ResourceLocation, List<Rectangle2F>> areaInclude = new HashMap<ResourceLocation, List<Rectangle2F>>();
		final Map<ResourceLocation, List<Rectangle2F>> areaExclude = new HashMap<ResourceLocation, List<Rectangle2F>>();
		final List<Predicate<String>> textReplaceClasses = new ArrayList<>();
		final Map<ResourceLocation, List<PartialTexColor>> partials = new HashMap<ResourceLocation, List<PartialTexColor>>();
		final Map<ResourceLocation, List<ColoredRectangle>> fills = new HashMap<ResourceLocation, List<ColoredRectangle>>();
		final Map<ResourceLocation, List<RenderRectangle>> renders = new HashMap<ResourceLocation, List<RenderRectangle>>();
		final List<Predicate<String>> textBlacklistClasses = new ArrayList<>();
		final List<TxMapSprite> spritesExclude = new ArrayList<TxMapSprite>();
		String id;
		String name;
		String icon;
		String description;
		UUID browserId;
		StringArrayList authors;
		StringArrayList dependencies;

		public ModifyContext(long version)
		{
			this.version = version;
		}

		public ModifyContext copy()
		{
			ModifyContext c = new ModifyContext(this.version);
			c.lines.addAll(this.lines);
			c.exclusions.addAll(this.exclusions);
			c.inclusions.addAll(this.inclusions);
			c.forceReloads.addAll(this.forceReloads);
			c.colors.putAll(this.colors);
			c.areaInclude.putAll(this.areaInclude);
			c.areaExclude.putAll(this.areaExclude);
			c.partials.putAll(this.partials);
			c.fills.putAll(this.fills);
			c.renders.putAll(this.renders);
			c.spritesExclude.addAll(this.spritesExclude);
			c.id = this.id;
			c.name = this.name;
			c.icon = this.icon;
			c.description = this.description;
			if(this.authors != null)
			{
				c.authors = new StringArrayList(this.authors);
			}
			if(this.dependencies != null)
			{
				c.dependencies = new StringArrayList(this.dependencies);
			}
			c.browserId = this.browserId;
			c.textReplaceClasses.addAll(this.textReplaceClasses);
			c.textBlacklistClasses.addAll(this.textBlacklistClasses);
			return c;
		}

		public List<Predicate<ResourceLocation>> getForceReloads()
		{
			return this.forceReloads;
		}

		public List<Predicate<ResourceLocation>> getExclusions()
		{
			return this.exclusions;
		}

		public Map<ResourceLocation, List<Rectangle2F>> getAreaInclude()
		{
			return this.areaInclude;
		}

		public List<Predicate<ResourceLocation>> getInclusions()
		{
			return this.inclusions;
		}

		public Map<ResourceLocation, List<Rectangle2F>> getAreaExclude()
		{
			return this.areaExclude;
		}

		public Map<Integer, Integer> getColors()
		{
			return this.colors;
		}

		public Map<Integer, Integer> getTextColors()
		{
			return this.textColors;
		}

		public Map<ResourceLocation, List<ColoredRectangle>> getFills()
		{
			return this.fills;
		}

		public Map<ResourceLocation, List<RenderRectangle>> getRenders()
		{
			return this.renders;
		}

		public List<TxMapSprite> getSpritesExclude()
		{
			return this.spritesExclude;
		}

		public long getVersion()
		{
			return this.version;
		}

		public String getId()
		{
			return this.id;
		}

		public String getName()
		{
			if(this.name == null)
			{
				this.name = "Unnamed";
			}
			return this.name;
		}

		public String getIcon()
		{
			return this.icon;
		}

		public String getDescription()
		{
			return this.description;
		}

		public UUID getBrowserId()
		{
			return this.browserId;
		}

		public List<String> getAuthors()
		{
			return this.authors == null ? Collections.emptyList() : this.authors;
		}

		public List<String> getDependencies()
		{
			return this.dependencies;
		}

		public List<Predicate<String>> getTextReplaceClasses()
		{
			return this.textReplaceClasses;
		}

		public Map<ResourceLocation, List<PartialTexColor>> getPartials()
		{
			return this.partials;
		}

		public List<Predicate<String>> getTextBlacklistClasses()
		{
			return this.textBlacklistClasses;
		}
	}

	public enum LineFunction
	{
		EXCLUDE(null),
		INCLUDE(null),
		COLOR(Point.class),
		VERSION(Long.class),
		COMMENT(null, true),
		WHITESPACE(null, true),
		ID(String.class),
		NAME(String.class),
		AUTHORS(StringArrayList.class),
		ICON(String.class),
		DESCRIPTION(String.class),
		AREA_EXCLUDE(TextureRectangle.class),
		AREA_INCLUDE(TextureRectangle.class),
		INCLUDE_SOURCES(null, true),
		BROWSER_ID(UUID.class),
		MOD_DEPENDENCY(StringArrayList.class),
		TEXT_COLOR(Point.class),
		TEXT_GUI(String.class),
		PARTIAL_TEX_COL(PartialTexColor.class),
		TEXT_COLOR_EXCLUDE(String.class),
		FILL_COLOR(ColoredRectangle.class),
		RENDER_TEX(RenderRectangle.class),
		EXCLUDE_SPRITE(TxMapSprite.class),
		FORCE_RELOAD(null);

		boolean ignore = false;
		final Class<?> dataType;

		LineFunction(Class<?> dataType)
		{
			this.dataType = dataType;
		}

		LineFunction(Class<?> dataType, boolean v)
		{
			this(dataType);
			this.ignore = v;
		}

		public Class<?> getDataType()
		{
			return this.dataType;
		}

		public boolean ignoreDeserialize()
		{
			return this.ignore;
		}
	}
}

