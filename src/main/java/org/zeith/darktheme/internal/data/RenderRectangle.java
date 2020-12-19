package org.zeith.darktheme.internal.data;

import net.minecraft.util.ResourceLocation;

public class RenderRectangle
{
	public final ResourceLocation srcTex;
	public final ResourceLocation dstTex;
	public final Rectangle2F srcRect;
	public final Rectangle2F dstRect;
	public final ByteStore renderMeta;

	public RenderRectangle(ResourceLocation srct, ResourceLocation dstt, Rectangle2F srcr, Rectangle2F dstr, byte renderMeta)
	{
		this.srcTex = srct;
		this.dstTex = dstt;
		this.srcRect = srcr;
		this.dstRect = dstr;
		this.renderMeta = new ByteStore(renderMeta);
	}

	public boolean doAA()
	{
		return this.renderMeta.get(0);
	}

	public boolean renderFancy()
	{
		return this.renderMeta.get(1);
	}

	public static byte create(boolean aa, boolean fancy)
	{
		ByteStore bs = new ByteStore((byte) 0);
		bs.set(0, aa);
		bs.set(1, fancy);
		return bs.getData();
	}
}