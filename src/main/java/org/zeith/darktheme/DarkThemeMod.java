package org.zeith.darktheme;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ProgressManager;
import net.minecraftforge.fml.common.discovery.ASMDataTable;
import net.minecraftforge.fml.common.event.FMLConstructionEvent;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zeith.darktheme.api.DarkThemeAPI;
import org.zeith.darktheme.api.IDarkAPI;
import org.zeith.darktheme.api.IFixedTxMap;
import org.zeith.darktheme.api.event.StylesReloadEvent;
import org.zeith.darktheme.compat.DarkCompat;
import org.zeith.darktheme.compat.DoDC;
import org.zeith.darktheme.internal.*;
import org.zeith.darktheme.internal.cmds.CommandCompile;
import org.zeith.darktheme.internal.cmds.CommandExportSprite;
import org.zeith.darktheme.internal.cmds.CommandExportTexture;
import org.zeith.darktheme.internal.cmds.CommandReloadStyles;
import org.zeith.darktheme.internal.data.*;
import org.zeith.darktheme.internal.ds.DarkCompiler;
import org.zeith.darktheme.internal.ds.DarkScript;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.Field;
import java.net.UnknownHostException;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Mod(modid = "darktheme", name = "Dark Theme Mod", version = "13r", certificateFingerprint = "9f5e2a811a8332a842b34f6967b7db0ac4f24856", updateJSON = "http://dccg.herokuapp.com/api/fmluc/323440", clientSideOnly = true, guiFactory = "org.zeith.darktheme.gui.CfgFactory")
public class DarkThemeMod
		implements IDarkAPI
{
	public static final List<String> ERRORS = new ArrayList<>();
	public static final int ERR_CHAT_ID = -811225;
	public static final Logger LOG = LogManager.getLogger();
	public static final List<Predicate<ResourceLocation>> BLACKLIST = new ArrayList<>();
	public static final List<Predicate<ResourceLocation>> INCLUDES = new ArrayList<>();
	public static final List<Predicate<ResourceLocation>> FORCERELOADS = new ArrayList<>();
	public static final Map<ResourceLocation, List<Rectangle2F>> AREA_EXCLUDE = new HashMap<>();
	public static final Map<ResourceLocation, List<Rectangle2F>> AREA_INCLUDE = new HashMap<>();
	public static final Map<ResourceLocation, List<ColoredRectangle>> TEXTURE_FILLS = new HashMap<>();
	public static final Map<ResourceLocation, List<RenderRectangle>> TEXTURE_RENDERS = new HashMap<>();
	public static final Map<Integer, Integer> COLOR_MAP = new HashMap<>();
	public static final Map<Integer, Integer> TEXT_COLOR_MAP = new HashMap<>();
	public static final List<Predicate<String>> GUI_CLASS_WHITELIST = new ArrayList<>();
	public static final List<Predicate<String>> GUI_CLASS_BLACKLIST = new ArrayList<>();
	public static final List<DarkScript> styles = new ArrayList<>();
	public static final List<UUID> styleBIDS = new ArrayList<>();
	public static final Map<ResourceLocation, List<TxMapSprite>> TXMAP_EXCLUDES = new HashMap<>();
	public static final Map<ResourceLocation, List<PartialTexColor>> PARTIALS = new HashMap<>();
	public static final PrintStream LOG_FILE = System.out;
	public static final List<Class<? extends GuiScreen>> DISCOVERED_GUIS = new ArrayList<>();
	public static boolean reloadMarker;
	public static File modConfigDir;
	public static File darkScripts;
	public static final List<DarkCompat> COMPATS = new ArrayList<>();
	public static DarkScript resourcePackScript;
	boolean fontsOverriden;
	public static final Consumer<DarkScript.ModifyContext> APPLY_CONTEXT;
	public static final Consumer<DarkScript.ModifyContext> UNDO_CONTEXT;

	private static DarkThemeMod INSTANCE;

	{
		INSTANCE = this;
	}

	@Mod.EventHandler
	public void ctr(FMLConstructionEvent e)
	{
		FinalFieldHelper.setStaticFinalField(DarkThemeAPI.class, "api", this);
		DarkCompiler.forwardCompat = true;
		File configFile = new File(Minecraft.getMinecraft().gameDir, "config/splash.properties");
		if(configFile.isFile())
		{
			try(RandomAccessFile raf = new RandomAccessFile(configFile, "rw"))
			{
				raf.seek(0L);
				while(true)
				{
					long prevPos = raf.getFilePointer();
					String ln = raf.readLine();
					if(ln == null)
					{
						break;
					}
					if(ln.equalsIgnoreCase("background=0xFFFFFF"))
					{
						raf.seek(prevPos + 13L);
						raf.write("484C52".getBytes());
						LOG.info("Darkened 'background' in loading screen. Will work after restart.");
					}
					if(!ln.equalsIgnoreCase("barBackground=0xFFFFFF")) continue;
					raf.seek(prevPos + 16L);
					raf.write("43474C".getBytes());
					LOG.info("Darkened 'barBackground' in loading screen. Will work after restart.");
				}
			} catch(IOException iOException)
			{
				// empty catch block
			}
		}
	}

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent e)
	{
		String remoteDefScriptStr;
		Long ioVersion;
		Long ourVersion;
		File def;
		ProgressManager.ProgressBar bar;
		block54:
		{
			bar = ProgressManager.push("Loading Dark Theme Mod...", 4);
			for(ASMDataTable.ASMData data : e.getAsmData().getAll(DoDC.class.getName()))
			{
				String mod = data.getAnnotationInfo().get("value") + "";
				try
				{
					Class<?> c;
					if(!Loader.isModLoaded(mod) || !DarkCompat.class.isAssignableFrom(c = Class.forName(data.getClassName())))
						continue;
					DarkCompat dc = (DarkCompat) c.getDeclaredConstructor(new Class[0]).newInstance();
					COMPATS.add(dc);
					dc.preInit();
				} catch(ReflectiveOperationException e1)
				{
					e1.printStackTrace();
				}
			}
			MinecraftForge.EVENT_BUS.register(this);
			modConfigDir = new File(e.getModConfigurationDirectory(), "DarkThemeMod");
			if(!modConfigDir.isDirectory())
				modConfigDir.mkdirs();
			darkScripts = new File(modConfigDir, "styles");
			if(!darkScripts.isDirectory())
				darkScripts.mkdirs();
			CacheStore.cache = new File(modConfigDir, "cache");
			def = new File(darkScripts, "style.ds");
			File defDis = new File(darkScripts, "style.ds.disabled");
			if(!def.isFile() && defDis.isFile())
			{
				def = defDis;
			}
			try
			{
				FinalFieldHelper.setStaticFinalField(DarkThemeMod.class, "LOG_FILE", new PrintStream(new File(modConfigDir, "logs.txt")));
			} catch(FileNotFoundException e1)
			{
				e1.printStackTrace();
			}
			ourVersion = null;
			ioVersion = null;

			DarkScript remoteDefScript;
			try
			{
				try(InputStream srcIn = HttpUtil.open("https://zeitheron.github.io/DarkScriptThemes/default.ds");
					FileOutputStream srcOut = new FileOutputStream(def))
				{
					int r;
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					byte[] buf = new byte[1024];
					while((r = srcIn.read(buf)) > 0)
					{
						srcOut.write(buf, 0, r);
						baos.write(buf, 0, r);
					}
					remoteDefScriptStr = baos.toString();
					remoteDefScript = new DarkScript(remoteDefScriptStr.replaceAll("\r", "").split("\n")).parse(false, null, null);
				} catch(Throwable err)
				{
					throw err;
				}
			} catch(IOException ioe)
			{
				remoteDefScript = null;
				remoteDefScriptStr = "";
				if(ioe instanceof UnknownHostException)
				{
					try(InputStream srcIn = Minecraft.getMinecraft().getResourceManager().getResource(new ResourceLocation("darktheme", "default.ds")).getInputStream();
						FileOutputStream srcOut = new FileOutputStream(def))
					{
						int r;
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						byte[] buf = new byte[1024];
						while((r = srcIn.read(buf)) > 0)
						{
							srcOut.write(buf, 0, r);
							baos.write(buf, 0, r);
						}
						remoteDefScriptStr = baos.toString();
						remoteDefScript = new DarkScript(remoteDefScriptStr.replaceAll("\r", "").split("\n")).parse(false, null, null);
					} catch(Throwable err)
					{
					}
				} else throw new RuntimeException(ioe);
			}
			bar.step("Validating DarkScripts...");
			if(!darkScripts.isDirectory() && remoteDefScript != null)
			{
				darkScripts.mkdirs();
				ourVersion = remoteDefScript.getVersion();
			}
			bar.step("Extracting our version...");
			if(ourVersion == null)
			{
				try
				{
					ioVersion = new DarkScript(def).parse(false, null, null).getVersion();
				} catch(RuntimeException e2)
				{
					if(e2.getCause() != null) break block54;
					ERRORS.add(def.getName() + ": " + e2.getMessage());
				}
			}
		}
		bar.step("Comparing DarkScript versions...");
		if(ourVersion == null || ioVersion == null || ourVersion > ioVersion)
		{
			def.renameTo(new File(def.getAbsolutePath() + ".old"));
			try
			{
				try(FileOutputStream srcOut = new FileOutputStream(def))
				{
					srcOut.write(remoteDefScriptStr.getBytes());
				} catch(Throwable throwable3)
				{
					throw throwable3;
				}
			} catch(IOException ioe)
			{
				throw new RuntimeException(ioe);
			}
		}
		TexTransformer.addTransformer(TexTransformer.ITextureTransformer.create(DarkThemeMod::processTexture, (img, tex) ->
		{
			LOG_FILE.println("Processing texture: " + tex);
			DarkThemeMod.handle(img, tex);
		}));
		bar.step("Loading DarkScripts...");
		new Thread(() ->
		{
			ScriptBrowser.reload();
			block0:
			for(ScriptBrowser.FetchableScript f : ScriptBrowser.stylesList)
			{
				for(String dep : f.dependencies)
				{
					if(Loader.isModLoaded(dep)) continue;
					continue block0;
				}
				f.download(false);
			}
			DarkThemeMod.loadStyles(false);
		}, "DarkThemePreInit").start();
		((IReloadableResourceManager) Minecraft.getMinecraft().getResourceManager()).registerReloadListener(mgr ->
		{
			DarkThemeMod.loadStyles(false);
			TexTransformer.reloadTextures();
		});
		ClientCommandHandler.instance.registerCommand(new CommandReloadStyles());
		ClientCommandHandler.instance.registerCommand(new CommandCompile());
		ClientCommandHandler.instance.registerCommand(new CommandExportTexture());
		ClientCommandHandler.instance.registerCommand(new CommandExportSprite());
		ProgressManager.pop(bar);
	}

	@Mod.EventHandler
	public void init(FMLInitializationEvent e)
	{
		TextureManager tex = Minecraft.getMinecraft().getTextureManager();
		Field f = TextureManager.class.getDeclaredFields()[2];
		try
		{
			CallableMap<ResourceLocation, ITextureObject> newMap = new CallableMap<>(tex.mapTextureObjects, (res, tobj) -> TexTransformer.handle(res, tobj, tex));
			FinalFieldHelper.setFinalField(f, tex, newMap);
		} catch(ReflectiveOperationException e1)
		{
			e1.printStackTrace();
		}
		COMPATS.forEach(DarkCompat::init);
	}

	public static int handleAlpha(int a, int b)
	{
		return ColorHelper.packARGB(ColorHelper.getAlpha(a), ColorHelper.getRed(b), ColorHelper.getGreen(b), ColorHelper.getBlue(b));
	}

	public static int stripAlpha(int c)
	{
		return ColorHelper.packRGB(ColorHelper.getRed(c), ColorHelper.getGreen(c), ColorHelper.getBlue(c));
	}

	public static void loadStyles(boolean forceReload)
	{
		if(!styles.isEmpty())
		{
			LOG.info("Unloading " + styles.size() + " styles...");
			while(!styles.isEmpty())
			{
				UNDO_CONTEXT.accept(styles.remove(0).getContext());
			}
		}
		styleBIDS.clear();
		MinecraftForge.EVENT_BUS.post(new StylesReloadEvent.Pre());
		LOG.info("Resolving .ds files...");

		File[] fs = darkScripts.listFiles(f -> f.isFile() && (f.getName().endsWith(".ds") || f.getName().endsWith(".ds.disabled") || f.getName().endsWith(".cds") || f.getName().endsWith(".cds.disabled")));

		LOG.info("Located " + fs.length + " DarkScript files... parsing...");
		for(File sub : fs)
		{
			try
			{
				DarkScript ds = new DarkScript(sub).parse(!sub.getName().endsWith(".disabled"), APPLY_CONTEXT, UNDO_CONTEXT);
				if(ds == null)
				{
					throw new RuntimeException("Unable to parse " + sub.getName());
				}
				styles.add(ds);
				if(ds.getContext().getBrowserId() == null) continue;
				styleBIDS.add(ds.getContext().getBrowserId());
			} catch(RuntimeException e)
			{
				e.printStackTrace();
				ERRORS.add(sub.getName() + ": " + e.getMessage());
			}
		}
		try
		{
			resourcePackScript = null;
			IResource res = Minecraft.getMinecraft().getResourceManager().getResource(new ResourceLocation("darktheme", "style.ds"));
			try(InputStream in = res.getInputStream())
			{
				int r;
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				byte[] buf = new byte[1024];
				while((r = in.read(buf)) > 0)
				{
					baos.write(buf, 0, r);
				}
				resourcePackScript = new DarkScript(baos.toString().replaceAll("\r", "").split("\n")).parse(true, APPLY_CONTEXT, UNDO_CONTEXT);
			}
			LOG.info("Loaded \"/assets/darktheme/style.ds\" from current resource pack!");
		} catch(Throwable e)
		{
			if(e instanceof RuntimeException)
			{
				ERRORS.add("Resource pack: " + e.getMessage());
			}
			LOG.warn("Warning: unable to find \"/assets/darktheme/style.ds\"! You may put it in your resourcepacks to improve compatability.");
		}
		if(resourcePackScript != null)
		{
			styles.add(resourcePackScript);
		}
		if(forceReload)
		{
			reloadMarker = true;
		}

		StylesReloadEvent.StylesContext ctx = new StylesReloadEvent.StylesContext(DarkThemeMod.INSTANCE::texColor);

		MinecraftForge.EVENT_BUS.post(new StylesReloadEvent.Post(ctx));
		LOG.info("Styles reloaded.");
	}

	public static void fixGlTexture(int glId, ResourceLocation path, @Nullable BufferedImage image, Rectangle2F... excludes)
	{
		if(image == null)
		{
			image = GLImageManager.toBufferedImage(glId);
		}
		DarkThemeMod.handle(image, path, excludes);
		TextureUtil.uploadTextureImageSub(glId, image, 0, 0, false, false);
	}

	@SubscribeEvent
	@SideOnly(value = Side.CLIENT)
	public void clientTick(TickEvent.ClientTickEvent cte)
	{
		if(reloadMarker)
		{
			reloadMarker = false;
			TexTransformer.reloadTextures();
		}
		if(!this.fontsOverriden)
		{
			Minecraft.getMinecraft().fontRenderer = new GUIAwareFontRenderer(Minecraft.getMinecraft().gameSettings, Minecraft.getMinecraft().fontRenderer);
			Minecraft.getMinecraft().standardGalacticFontRenderer = new GUIAwareFontRenderer(Minecraft.getMinecraft().gameSettings, Minecraft.getMinecraft().standardGalacticFontRenderer);
			this.fontsOverriden = true;
		}
		if(!ERRORS.isEmpty() && Minecraft.getMinecraft().player != null)
			Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new TextComponentString(ERRORS.remove(0)));
	}

	@Override
	public void excludeTextures(Predicate<ResourceLocation> textures)
	{
		BLACKLIST.add(textures);
	}

	@Override
	public void color(int srcARGB, int dstARGB)
	{
		COLOR_MAP.put(srcARGB, dstARGB);
	}

	@Override
	public int texColor(int color)
	{
		int orgb = DarkThemeMod.stripAlpha(color);
		int nrgb = COLOR_MAP.getOrDefault(orgb, orgb);
		if(nrgb != orgb)
			return DarkThemeMod.handleAlpha(color, nrgb);
		return color;
	}

	private static void applyRenders(BufferedImage src, ResourceLocation texture)
	{
		if(TEXTURE_FILLS.containsKey(texture) || TEXTURE_RENDERS.containsKey(texture))
		{
			Graphics2D gfx = src.createGraphics();
			if(TEXTURE_FILLS.containsKey(texture))
			{
				for(ColoredRectangle cr : TEXTURE_FILLS.get(texture))
				{
					Rectangle2F rect = cr.rect.scale(src.getWidth(), src.getHeight());
					gfx.setColor(new Color(cr.color));
					gfx.fillRect(Math.round(rect.getX1()), Math.round(rect.getY1()), Math.round(rect.getWidth()), Math.round(rect.getHeight()));
				}
			}
			if(TEXTURE_RENDERS.containsKey(texture))
			{
				for(RenderRectangle rr : TEXTURE_RENDERS.get(texture))
				{
					BufferedImage toRender = null;
					gfx.setRenderingHint(RenderingHints.KEY_ANTIALIASING, rr.doAA() ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);
					gfx.setRenderingHint(RenderingHints.KEY_RENDERING, rr.renderFancy() ? RenderingHints.VALUE_RENDER_QUALITY : RenderingHints.VALUE_RENDER_SPEED);
					try(IResource res = Minecraft.getMinecraft().getResourceManager().getResource(rr.srcTex);
						InputStream in = res.getInputStream())
					{
						toRender = ImageIO.read(in);
					} catch(IOException ioe)
					{
						ERRORS.add("Error while handling \"" + texture + "\": Unable to load \"" + rr.srcTex + "\"!");
						ioe.printStackTrace();
					}
					if(toRender != null)
					{
						Rectangle2F srcr = rr.srcRect.scale(toRender.getWidth(), toRender.getHeight());
						toRender = toRender.getSubimage(Math.round(srcr.getX1()), Math.round(srcr.getY1()), Math.round(srcr.getWidth()), Math.round(srcr.getHeight()));
						srcr = rr.dstRect.scale(src.getWidth(), src.getHeight());
						gfx.drawImage(toRender, Math.round(srcr.getX1()), Math.round(srcr.getY1()), Math.round(srcr.getWidth()), Math.round(srcr.getHeight()), null);
						continue;
					}
					ERRORS.add("Error while handling \"" + texture + "\": Invalid image at \"" + rr.srcTex + "\"!");
				}
			}
			gfx.dispose();
		}
	}

	public static void handle(BufferedImage src, ResourceLocation texture, Rectangle2F... excludes)
	{
		GuiScreen gs;
		for(Predicate<ResourceLocation> rl : BLACKLIST)
		{
			if(!rl.test(texture)) continue;
			DarkThemeMod.applyRenders(src, texture);
			return;
		}
		List<Rectangle2F> includeArea = Rectangle2F.scaleAll(AREA_INCLUDE.get(texture), src.getWidth(), src.getHeight());
		ArrayList<Rectangle2F> excludeArea = new ArrayList<Rectangle2F>(Rectangle2F.scaleAll(AREA_EXCLUDE.get(texture), src.getWidth(), src.getHeight()));
		excludeArea.addAll(Rectangle2F.scaleAll(Arrays.asList(excludes), src.getWidth(), src.getHeight()));
		boolean handle = !excludeArea.isEmpty();
		boolean whitelist = !includeArea.isEmpty();
		boolean affected = false;
		for(int x = 0; x < src.getWidth(); ++x)
		{
			block2:
			for(int y = 0; y < src.getHeight(); ++y)
			{
				if(handle)
				{
					for(Rectangle2F r : excludeArea)
					{
						if(!r.contains(x, y)) continue;
						continue block2;
					}
				}

				if(whitelist)
				{
					boolean let = false;
					for(Rectangle2F r2 : includeArea)
					{
						if(!r2.contains(x, y)) continue;
						let = true;
						break;
					}
					if(!let) continue;
				}

				int _or = src.getRGB(x, y);
				int orgb = DarkThemeMod.stripAlpha(_or);
				int nrgb = COLOR_MAP.getOrDefault(orgb, orgb);
				if(PARTIALS.containsKey(texture))
				{
					for(PartialTexColor ptc : PARTIALS.get(texture))
					{
						if(orgb != DarkThemeMod.stripAlpha(ptc.color.x) || !ptc.rect.scale(src.getWidth(), src.getHeight()).contains(x, y))
							continue;
						nrgb = ptc.color.y;
						break;
					}
				}
				if(nrgb == orgb) continue;
				src.setRGB(x, y, DarkThemeMod.handleAlpha(_or, nrgb));
				affected = true;
			}
		}
		DarkThemeMod.applyRenders(src, texture);
		if((affected || TEXTURE_FILLS.containsKey(texture) || TEXTURE_RENDERS.containsKey(texture)) && (gs = Minecraft.getMinecraft().currentScreen) != null && !DISCOVERED_GUIS.contains(gs.getClass()))
			DISCOVERED_GUIS.add(gs.getClass());
	}

	public static boolean shouldReload(ResourceLocation tex)
	{
		for(Predicate<ResourceLocation> exl : FORCERELOADS)
		{
			if(!exl.test(tex)) continue;
			return true;
		}
		return false;
	}

	public static boolean processTexture(ResourceLocation tex)
	{
		for(Predicate<ResourceLocation> inc : INCLUDES)
		{
			if(inc == null || !inc.test(tex)) continue;
			return true;
		}
		return false;
	}

	private static <T> void safeSetFieldData(Field f, Object instance, T data)
	{
		try
		{
			f.set(instance, data);
		} catch(ClassCastException | ReflectiveOperationException e)
		{
			return;
		}
	}

	private static <T> T safeGetFieldData(Field f, Object instance)
	{
		try
		{
			return (T) f.get(instance);
		} catch(ClassCastException | ReflectiveOperationException e)
		{
			return null;
		}
	}

	private static Field safeGetField(Class<?> cls, String fn)
	{
		try
		{
			Field f = cls.getDeclaredField(fn);
			f.setAccessible(true);
			return f;
		} catch(ReflectiveOperationException reflectiveOperationException)
		{
			return null;
		}
	}

	static
	{
		APPLY_CONTEXT = ctx ->
		{
			List<String> req = ctx.getDependencies();
			if(req != null)
			{
				for(String rm : req)
				{
					if(Loader.isModLoaded(rm)) continue;
					return;
				}
			}
			BLACKLIST.addAll(ctx.getExclusions());
			INCLUDES.addAll(ctx.getInclusions());
			FORCERELOADS.addAll(ctx.getForceReloads());
			COLOR_MAP.putAll(ctx.getColors());
			TEXT_COLOR_MAP.putAll(ctx.getTextColors());
			GUI_CLASS_WHITELIST.addAll(ctx.getTextReplaceClasses());
			GUI_CLASS_BLACKLIST.addAll(ctx.getTextBlacklistClasses());
			MapMath.add(ctx.getAreaExclude(), AREA_EXCLUDE);
			MapMath.add(ctx.getAreaInclude(), AREA_INCLUDE);
			MapMath.add(ctx.getPartials(), PARTIALS);
			MapMath.add(ctx.getFills(), TEXTURE_FILLS);
			MapMath.add(ctx.getRenders(), TEXTURE_RENDERS);
			TextureManager mgr = Minecraft.getMinecraft().getTextureManager();
			if(mgr != null)
				for(TxMapSprite txm : ctx.getSpritesExclude())
				{
					ITextureObject obj;
					MapMath.add(TXMAP_EXCLUDES, txm.textureMap, txm);
					if((obj = mgr.getTexture(txm.textureMap)) instanceof IFixedTxMap)
						((IFixedTxMap) obj).addExclude(txm.spriteName);
				}
		};

		UNDO_CONTEXT = ctx ->
		{
			BLACKLIST.removeAll(ctx.getExclusions());
			INCLUDES.removeAll(ctx.getInclusions());
			FORCERELOADS.removeAll(ctx.getForceReloads());
			ctx.getColors().keySet().forEach(COLOR_MAP::remove);
			ctx.getTextColors().keySet().forEach(TEXT_COLOR_MAP::remove);
			GUI_CLASS_WHITELIST.removeAll(ctx.getTextReplaceClasses());
			GUI_CLASS_BLACKLIST.removeAll(ctx.getTextBlacklistClasses());
			MapMath.remove(ctx.getAreaExclude(), AREA_EXCLUDE);
			MapMath.remove(ctx.getAreaInclude(), AREA_INCLUDE);
			MapMath.remove(ctx.getPartials(), PARTIALS);
			MapMath.remove(ctx.getFills(), TEXTURE_FILLS);
			MapMath.remove(ctx.getRenders(), TEXTURE_RENDERS);
			TextureManager mgr = Minecraft.getMinecraft().getTextureManager();
			for(TxMapSprite txm : ctx.getSpritesExclude())
			{
				ITextureObject obj;
				MapMath.remove(TXMAP_EXCLUDES, txm.textureMap, txm);
				if(!((obj = mgr.getTexture(txm.textureMap)) instanceof IFixedTxMap)) continue;
				((IFixedTxMap) obj).removeExclude(txm.spriteName);
			}
		};
	}
}

