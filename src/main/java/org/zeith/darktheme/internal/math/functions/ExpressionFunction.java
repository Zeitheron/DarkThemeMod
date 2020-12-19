package org.zeith.darktheme.internal.math.functions;

public abstract class ExpressionFunction
{
	public final String functionName;

	public ExpressionFunction(String funcName)
	{
		this.functionName = funcName;
	}

	public boolean accepts(String functionName, double x)
	{
		return this.functionName.equalsIgnoreCase(functionName);
	}

	public double apply(String functionName, double x)
	{
		return this.apply(x);
	}

	@Deprecated
	public double apply(double x)
	{
		return x;
	}
}