/*
 * Decompiled with CFR 0.150.
 */
package org.zeith.darktheme.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

public class MD5
{
	public static String encrypt(byte[] data)
	{
		MessageDigest messageDigest = null;
		byte[] digest = new byte[]{};
		try
		{
			messageDigest = MessageDigest.getInstance("MD5");
			messageDigest.reset();
			messageDigest.update(data);
			digest = messageDigest.digest();
		} catch(NoSuchAlgorithmException e)
		{
			e.printStackTrace();
		}
		BigInteger bigInt = new BigInteger(1, digest);
		String md5Hex = bigInt.toString(16);
		while(md5Hex.length() < 32)
		{
			md5Hex = "0" + md5Hex;
		}
		return md5Hex;
	}

	public static String encrypt(String line)
	{
		return MD5.encrypt(line.getBytes());
	}

	public static String createFolderMD5(File prime)
	{
		if(!prime.exists())
		{
			return "ERROR";
		}
		ArrayList<File> paths = new ArrayList<File>();
		ArrayList<File> files = new ArrayList<File>();
		paths.add(prime);
		int lastSize = 0;
		while(paths.size() != lastSize)
		{
			lastSize = paths.size();
			for(int i = 0; i < paths.size(); ++i)
			{
				File f = (File) paths.get(i);
				if(f.isDirectory())
				{
					for(File r : f.listFiles())
					{
						if(r.isDirectory() && !paths.contains(r))
						{
							paths.add(r);
							continue;
						}
						if(!r.isFile() || files.contains(r)) continue;
						files.add(r);
					}
					continue;
				}
				if(!f.isFile() || files.contains(f)) continue;
				files.add(f);
			}
		}
		StringBuilder b = new StringBuilder();
		for(File k : files)
		{
			if(k.equals(prime)) continue;
			try
			{
				b.append(String.valueOf(MD5.getMD5Checksum(k)) + ",");
			} catch(Throwable err)
			{
				err.printStackTrace(System.out);
			}
		}
		return MD5.encrypt(b.toString().getBytes());
	}

	public static byte[] createChecksum(File file) throws Exception
	{
		int numRead;
		if(!file.exists())
		{
			MessageDigest messageDigest = null;
			byte[] digest = new byte[]{};
			try
			{
				messageDigest = MessageDigest.getInstance("MD5");
				messageDigest.reset();
				messageDigest.update("0".getBytes());
				digest = messageDigest.digest();
			} catch(NoSuchAlgorithmException e)
			{
				e.printStackTrace();
			}
			return digest;
		}
		FileInputStream fis = new FileInputStream(file);
		byte[] buffer = new byte[1024];
		MessageDigest complete = MessageDigest.getInstance("MD5");
		do
		{
			if((numRead = fis.read(buffer)) <= 0) continue;
			complete.update(buffer, 0, numRead);
		} while(numRead != -1);
		fis.close();
		return complete.digest();
	}

	public static String getMD5Checksum(File filename)
	{
		byte[] b = null;
		try
		{
			b = MD5.createChecksum(filename);
		} catch(Exception e)
		{
			e.printStackTrace(System.out);
		}
		BigInteger bigInt = new BigInteger(1, b);
		String md5Hex = bigInt.toString(16);
		while(md5Hex.length() < 32)
		{
			md5Hex = "0" + md5Hex;
		}
		return md5Hex;
	}

	public static class MD5OutputStream
			extends OutputStream
	{
		MessageDigest complete;

		{
			try
			{
				complete = MessageDigest.getInstance("MD5");
			} catch(NoSuchAlgorithmException e)
			{
				e.printStackTrace();
			}
		}

		byte[] dgs;
		String dgss;

		@Override
		public void write(int b) throws IOException
		{
			this.complete.update((byte) b);
		}

		@Override
		public void close() throws IOException
		{
			this.dgs = this.complete.digest();
			BigInteger bigInt = new BigInteger(1, this.dgs);
			String md5Hex = bigInt.toString(16);
			while(md5Hex.length() < 32)
			{
				md5Hex = "0" + md5Hex;
			}
			this.dgss = md5Hex;
		}

		public String getMD5()
		{
			return this.dgss;
		}

		public byte[] getDigest()
		{
			return this.dgs;
		}
	}
}

