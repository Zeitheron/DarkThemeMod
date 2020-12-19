/*
 * Decompiled with CFR 0.150.
 */
package org.zeith.darktheme.internal.math;

import org.zeith.darktheme.internal.math.functions.ExpressionFunction;
import org.zeith.darktheme.internal.math.functions.FunctionMath;
import java.util.ArrayList;
import java.util.List;

public class ExpressionEvaluator {
    private final String str;
    private int pos = -1;
    private int ch;
    private final List<ExpressionFunction> functions = new ArrayList<ExpressionFunction>();

    public ExpressionEvaluator(String str) {
        this.addFunction(FunctionMath.inst);
        str = str.replaceAll("PI", "3.141592653589793");
        this.str = str = str.replaceAll("E", "2.718281828459045");
    }

    private void nextChar() {
        this.ch = ++this.pos < this.str.length() ? (int)this.str.charAt(this.pos) : -1;
    }

    private boolean eat(int charToEat) {
        while (this.ch == 32) {
            this.nextChar();
        }
        if (this.ch == charToEat) {
            this.nextChar();
            return true;
        }
        return false;
    }

    public final double parse() {
        this.pos = -1;
        this.nextChar();
        double x = this.parseExpression();
        if (this.pos < this.str.length()) {
            throw new RuntimeException("Unexpected: " + (char)this.ch);
        }
        return x;
    }

    private double parseExpression() {
        double x = this.parseTerm();
        while (true) {
            if (this.eat(43)) {
                x += this.parseTerm();
                continue;
            }
            if (!this.eat(45)) break;
            x -= this.parseTerm();
        }
        return x;
    }

    private double parseTerm() {
        double x = this.parseFactor();
        while (true) {
            if (this.eat(42)) {
                x *= this.parseFactor();
                continue;
            }
            if (this.eat(47) || this.eat(58)) {
                x /= this.parseFactor();
                continue;
            }
            if (this.eat(37)) {
                x %= this.parseFactor();
                continue;
            }
            if (!this.eat(94)) break;
            x = Math.pow(x, this.parseFactor());
        }
        return x;
    }

    private double parseFactor() {
        double x;
        if (this.eat(43)) {
            return this.parseFactor();
        }
        if (this.eat(45)) {
            return -this.parseFactor();
        }
        int startPos = this.pos;
        if (this.eat(40)) {
            x = this.parseExpression();
            this.eat(41);
        } else if (this.ch >= 48 && this.ch <= 57 || this.ch == 46) {
            while (this.ch >= 48 && this.ch <= 57 || this.ch == 46) {
                this.nextChar();
            }
            x = Double.parseDouble(this.str.substring(startPos, this.pos));
        } else if (this.ch >= 97 && this.ch <= 122) {
            while (this.ch >= 97 && this.ch <= 122) {
                this.nextChar();
            }
            String func = this.str.substring(startPos, this.pos).toLowerCase();
            x = this.parseFactor();
            boolean funcFound = false;
            for (ExpressionFunction f : this.functions) {
                if (!f.accepts(func, x)) continue;
                x = f.apply(func, x);
                funcFound = true;
                break;
            }
            if (!funcFound) {
                throw new RuntimeException("Unknown function: " + func);
            }
        } else {
            throw new RuntimeException("Unexpected: " + (char)this.ch);
        }
        return x;
    }

    public void addFunction(ExpressionFunction func) {
        if (this.functions.contains(func)) {
            return;
        }
        this.functions.add(func);
    }

    public static String evaluate(String expression, ExpressionFunction ... functions) {
        double result = ExpressionEvaluator.evaluateDouble(expression, functions);
        if (result == Math.floor(result)) {
            return (int)result + "";
        }
        return result + "";
    }

    public static double evaluateDouble(String expression, ExpressionFunction ... functions) {
        try {
            return Double.parseDouble(expression);
        }
        catch (Throwable throwable) {
            ExpressionEvaluator eval = new ExpressionEvaluator(expression);
            for (ExpressionFunction func : functions) {
                eval.addFunction(func);
            }
            return eval.parse();
        }
    }
}

