package org.zeith.darktheme.internal;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class FinalFieldHelper
{
	private static Field modifiersField;
	private static Object reflectionFactory;
	private static Method newFieldAccessor;
	private static Method fieldAccessorSet;

	public static boolean setStaticFinalField(Class<?> cls, String var, Object val)
	{
		try
		{
			return FinalFieldHelper.setStaticFinalField(cls.getDeclaredField(var), val);
		} catch(Throwable err)
		{
			err.printStackTrace();
			return false;
		}
	}

	public static boolean setStaticFinalField(Field f, Object val)
	{
		try
		{
			if(Modifier.isStatic(f.getModifiers()))
			{
				return FinalFieldHelper.setFinalField(f, null, val);
			}
			return false;
		} catch(Throwable err)
		{
			err.printStackTrace();
			return false;
		}
	}

	public static boolean setFinalField(Field f, @Nullable Object instance, Object thing) throws ReflectiveOperationException
	{
		if(Modifier.isFinal(f.getModifiers()))
		{
			FinalFieldHelper.makeWritable(f);
			Object fieldAccessor = newFieldAccessor.invoke(reflectionFactory, f, false);
			fieldAccessorSet.invoke(fieldAccessor, instance, thing);
			return true;
		}
		return false;
	}

	private static Field makeWritable(Field f) throws ReflectiveOperationException
	{
		f.setAccessible(true);
		if(modifiersField == null)
		{
			Method getReflectionFactory = Class.forName("sun.reflect.ReflectionFactory").getDeclaredMethod("getReflectionFactory");
			reflectionFactory = getReflectionFactory.invoke(null);
			newFieldAccessor = Class.forName("sun.reflect.ReflectionFactory").getDeclaredMethod("newFieldAccessor", Field.class, Boolean.TYPE);
			fieldAccessorSet = Class.forName("sun.reflect.FieldAccessor").getDeclaredMethod("set", Object.class, Object.class);
			modifiersField = Field.class.getDeclaredField("modifiers");
			modifiersField.setAccessible(true);
		}
		modifiersField.setInt(f, f.getModifiers() & 0xFFFFFFEF);
		return f;
	}
}