package org.zeith.darktheme.internal.math.functions;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Set;

public class FunctionMath
		extends ExpressionFunction
{
	private static final SecureRandom rand = new SecureRandom((System.currentTimeMillis() + System.nanoTime() + "").getBytes());
	public static final FunctionMath inst = new FunctionMath();
	private final Set<String> allowedFuncs = new HashSet<>();

	public FunctionMath()
	{
		super("Math");
		for(Method m : Math.class.getMethods())
		{
			if(!Modifier.isStatic(m.getModifiers()) || !Modifier.isPublic(m.getModifiers()) || m.getParameterTypes().length != 1 || m.getParameterTypes()[0] != Double.TYPE && m.getParameterTypes()[0] != Double.class)
				continue;
			this.allowedFuncs.add(m.getName());
		}
	}

	@Override
	public boolean accepts(String functionName, double x)
	{
		return this.allowedFuncs.contains(functionName = functionName.toLowerCase()) || functionName.equals("rand");
	}

	@Override
	public double apply(String functionName, double x)
	{
		if((functionName = functionName.toLowerCase()).equals("rand"))
			return (double) rand.nextInt(Integer.MAX_VALUE) / 2.147483647E9 * x;
		if(functionName.equals("sin") || functionName.equals("cos") || functionName.equals("tan"))
			x = Math.toRadians(x);
		try
		{
			return (Double) Math.class.getMethod(functionName, Double.TYPE).invoke(null, x);
		} catch(Throwable throwable)
		{
			return x;
		}
	}
}