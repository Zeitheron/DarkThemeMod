package org.zeith.darktheme.internal.ds;

import org.zeith.darktheme.internal.data.*;
import net.minecraft.util.ResourceLocation;
import org.zeith.darktheme.internal.data.*;

import java.awt.*;
import java.io.*;
import java.util.List;
import java.util.*;

public final class DarkCompiler
{
	private static final Map<Class<?>, IDataHandler<?>> DATA = new HashMap<>();
	public static boolean forwardCompat = false;

	private static <T> void handle(IDataHandler<T> handler, Class<? extends T>... classes)
	{
		for(Class<? extends T> c : classes)
			DATA.put(c, handler);
	}

	private static <T> void handle(IDataWriter<T> writer, IDataReader<T> reader, Class<? extends T>... classes)
	{
		DarkCompiler.handle(IDataHandler.create(writer, reader), classes);
	}

	public static List<DarkScript.DarkLine> decompile(InputStream input) throws IOException
	{
		ArrayList<DarkScript.DarkLine> dls = new ArrayList<DarkScript.DarkLine>();
		DataInputStream din = new DataInputStream(input);
		int nodes = din.readInt();
		DarkScript.LineFunction[] lf = DarkScript.LineFunction.values();
		for(int i = 0; i < nodes; ++i)
		{
			IDataHandler<?> handler;
			byte insn = din.readByte();
			if(insn >= lf.length && forwardCompat)
			{
				System.out.println("[FWD-COMPAT] Detected unknown to us instruction: " + insn);
				continue;
			}
			DarkScript.LineFunction func = lf[insn];
			int lnn = din.readInt();
			boolean hasContext = din.readBoolean();
			Object data = null;
			String ctx = null;
			if(hasContext)
			{
				ctx = din.readUTF();
				if(func == DarkScript.LineFunction.EXCLUDE || func == DarkScript.LineFunction.INCLUDE || func == DarkScript.LineFunction.FORCE_RELOAD)
					data = new StringedPredicate<>(DarkScript.readResourcePredicate(ctx), ctx);
			}
			if(func.getDataType() != null && (handler = DATA.get(func.getDataType())) != null)
				data = handler.read(din);
			if(data == null)
				throw new IOException("Unable to decode line #" + lnn);
			dls.add(new DarkScript.DarkLine(data, func, lnn).setContext(ctx));
		}
		return dls;
	}

	public static void compile(DarkScript dark, OutputStream output) throws IOException
	{
		List<DarkScript.DarkLine> ctx = dark.getContext().lines;
		DataOutputStream dos = new DataOutputStream(output);
		dos.writeInt(ctx.size());
		for(DarkScript.DarkLine dl : ctx)
		{
			if(dl.function.ignoreDeserialize()) continue;
			dos.writeByte(dl.function.ordinal());
			dos.writeInt(dl.lineNumber);
			dos.writeBoolean(dl.context != null);
			if(dl.context != null)
			{
				dos.writeUTF(dl.context);
				continue;
			}
			Object write = dl.data;
			IDataHandler handler = DATA.get(dl.function.getDataType());
			if(handler == null) continue;
			handler.write(dos, write);
		}
	}

	static
	{
		DarkCompiler.handle((out, obj) -> out.writeLong(obj), DataInputStream::readLong, Long.TYPE, Long.class);
		DarkCompiler.handle((out, obj) -> out.writeInt(obj), DataInputStream::readInt, Integer.TYPE, Integer.class);
		DarkCompiler.handle((out, obj) -> out.writeUTF(obj), DataInput::readUTF, String.class);
		DarkCompiler.handle((out, obj) ->
		{
			out.writeUTF(obj.getTex().toString());
			out.writeFloat(obj.getRect().getX1());
			out.writeFloat(obj.getRect().getY1());
			out.writeFloat(obj.getRect().getWidth());
			out.writeFloat(obj.getRect().getHeight());
		}, (DataInputStream in) ->
		{
			ResourceLocation rl = new ResourceLocation(in.readUTF());
			float x = in.readFloat();
			float y = in.readFloat();
			float w = in.readFloat();
			float h = in.readFloat();
			Rectangle2F rect = new Rectangle2F(x, y, w, h);
			return new TextureRectangle(rl, rect);
		}, TextureRectangle.class);
		DarkCompiler.handle((out, obj) ->
		{
			out.writeInt(obj.x);
			out.writeInt(obj.y);
		}, (DataInputStream in) ->
		{
			int x = in.readInt();
			int y = in.readInt();
			return new Point(x, y);
		}, Point.class);
		DarkCompiler.handle((out, obj) ->
		{
			out.writeLong(obj.getMostSignificantBits());
			out.writeLong(obj.getLeastSignificantBits());
		}, (DataInputStream in) ->
		{
			long a = in.readLong();
			long b = in.readLong();
			return new UUID(a, b);
		}, UUID.class);
		DarkCompiler.handle((out, obj) ->
		{
			out.writeShort(obj.size());
			for(String a : obj)
			{
				out.writeUTF(a);
			}
		}, (DataInputStream in) ->
		{
			short acount = in.readShort();
			StringArrayList l = new StringArrayList();
			for(short j = 0; j < acount; j = (short) (j + 1))
			{
				l.add(in.readUTF());
			}
			return l;
		}, StringArrayList.class);
		DarkCompiler.handle((out, obj) ->
		{
			out.writeUTF(obj.path.toString());
			out.writeFloat(obj.rect.getX1());
			out.writeFloat(obj.rect.getY1());
			out.writeFloat(obj.rect.getWidth());
			out.writeFloat(obj.rect.getHeight());
			out.writeInt(obj.color.x);
			out.writeInt(obj.color.y);
		}, (DataInputStream in) ->
		{
			ResourceLocation loc = new ResourceLocation(in.readUTF());
			float x = in.readFloat();
			float y = in.readFloat();
			float w = in.readFloat();
			float h = in.readFloat();
			Rectangle2F rect = new Rectangle2F(x, y, w, h);
			int xi = in.readInt();
			int yi = in.readInt();
			return new PartialTexColor(loc, rect, new Point(xi, yi));
		}, PartialTexColor.class);
		DarkCompiler.handle((out, obj) ->
		{
			out.writeUTF(obj.tex.toString());
			out.writeFloat(obj.rect.getX1());
			out.writeFloat(obj.rect.getY1());
			out.writeFloat(obj.rect.getWidth());
			out.writeFloat(obj.rect.getHeight());
			out.writeInt(obj.color);
		}, (DataInputStream in) ->
		{
			ResourceLocation loc = new ResourceLocation(in.readUTF());
			float x = in.readFloat();
			float y = in.readFloat();
			float w = in.readFloat();
			float h = in.readFloat();
			Rectangle2F rect = new Rectangle2F(x, y, w, h);
			int xi = in.readInt();
			return new ColoredRectangle(loc, rect, xi);
		}, ColoredRectangle.class);
		DarkCompiler.handle((out, obj) ->
		{
			out.writeUTF(obj.srcTex.toString());
			out.writeUTF(obj.dstTex.toString());
			out.writeFloat(obj.srcRect.getX1());
			out.writeFloat(obj.srcRect.getY1());
			out.writeFloat(obj.srcRect.getWidth());
			out.writeFloat(obj.srcRect.getHeight());
			out.writeFloat(obj.dstRect.getX1());
			out.writeFloat(obj.dstRect.getY1());
			out.writeFloat(obj.dstRect.getWidth());
			out.writeFloat(obj.dstRect.getHeight());
			out.writeByte(obj.renderMeta.getData());
		}, (DataInputStream in) ->
		{
			ResourceLocation srct = new ResourceLocation(in.readUTF());
			ResourceLocation dstt = new ResourceLocation(in.readUTF());
			float x = in.readFloat();
			float y = in.readFloat();
			float w = in.readFloat();
			float h = in.readFloat();
			Rectangle2F srcr = new Rectangle2F(x, y, w, h);
			x = in.readFloat();
			y = in.readFloat();
			w = in.readFloat();
			h = in.readFloat();
			byte data = in.readByte();
			Rectangle2F dstr = new Rectangle2F(x, y, w, h);
			return new RenderRectangle(srct, dstt, srcr, dstr, data);
		}, RenderRectangle.class);
		DarkCompiler.handle((out, obj) ->
		{
			out.writeUTF(obj.textureMap.toString());
			out.writeUTF(obj.spriteName);
		}, (DataInputStream in) ->
		{
			String mp = in.readUTF();
			String name = in.readUTF();
			return new TxMapSprite(new ResourceLocation(mp), name);
		}, TxMapSprite.class);
	}

	public interface IDataWriter<TYPE>
	{
		void write(DataOutputStream var1, TYPE var2) throws IOException;
	}

	public interface IDataReader<TYPE>
	{
		TYPE read(DataInputStream var1) throws IOException;
	}

	public interface IDataHandler<TYPE>
			extends IDataReader<TYPE>,
			IDataWriter<TYPE>
	{
		static <T> IDataHandler<T> create(final IDataWriter<T> writer, final IDataReader<T> reader)
		{
			return new IDataHandler<T>()
			{
				@Override
				public void write(DataOutputStream out, T data) throws IOException
				{
					writer.write(out, data);
				}

				@Override
				public T read(DataInputStream in) throws IOException
				{
					return reader.read(in);
				}
			};
		}
	}
}

