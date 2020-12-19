package org.zeith.darktheme.internal;

import org.zeith.darktheme.internal.io.MultiOutputStream;
import org.zeith.darktheme.internal.io.ReleasableFileInputStream;

import java.io.*;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class CacheStore
{
	private static final Set<File> busy = new HashSet<File>();
	public static File cache;

	public static InputStream readWithCachingURL(String url)
	{
		if(!cache.isDirectory())
			cache.mkdirs();
		PipedInputStream pis = new PipedInputStream();
		PipedOutputStream posTmp = null;
		try
		{
			posTmp = new PipedOutputStream(pis);
		} catch(IOException iOException)
		{
		}
		PipedOutputStream pos = posTmp;
		String md5 = MD5.encrypt(url);
		File target = new File(cache, md5);
		if(target.isFile())
		{
			return CacheStore.$fileReader(target, url, () ->
			{
				File tmp = new File(cache, UUID.randomUUID() + ".tmp");
				MD5.MD5OutputStream md5os = null;
				try
				{
					InputStream http = HttpUtil.open(url);
					try(MD5.MD5OutputStream mos = new MD5.MD5OutputStream();
						MultiOutputStream mlos = new MultiOutputStream(new FileOutputStream(tmp), mos);)
					{
						int r;
						byte[] buf = new byte[1024];
						while((r = http.read(buf)) > 0)
						{
							mlos.write(buf, 0, r);
						}
						md5os = mos;
					} catch(Throwable throwable)
					{
						throw throwable;
					}
				} catch(IOException ioe)
				{
					ioe.printStackTrace();
				}
				if(md5os != null)
				{
					String remoteMD5 = md5os.getMD5();
					if(!remoteMD5.equals(md5))
					{
						while(busy.contains(target))
						{
							synchronized(busy)
							{
								try
								{
									busy.wait();
								} catch(InterruptedException interruptedException)
								{
								}
							}
						}
						target.delete();
						tmp.renameTo(target);
					} else
					{
						tmp.delete();
					}
				}
			});
		}

		new Thread(() ->
		{
			try(InputStream http = HttpUtil.open(url); MultiOutputStream mos = new MultiOutputStream(pos, new FileOutputStream(target)))
			{
				byte[] buf = new byte[1024];
				int r;
				while((r = http.read(buf)) > 0)
				{
					mos.write(buf, 0, r);
				}
			} catch(IOException ioe)
			{
				ioe.printStackTrace();
				target.delete();
			}
		}, "IO-DWN-" + md5).start();

		return pis;
	}

	private static InputStream $fileReader(File file, String url, Runnable handleDwn)
	{
		try
		{
			busy.add(file);
			return new ReleasableFileInputStream(cache, () ->
			{
				busy.remove(file);
				Set<File> set = busy;
				synchronized(set)
				{
					busy.notifyAll();
				}
				handleDwn.run();
			});
		} catch(FileNotFoundException e)
		{
			try
			{
				return HttpUtil.open(url);
			} catch(IOException e1)
			{
				return new ByteArrayInputStream(new byte[0]);
			}
		}
	}
}

