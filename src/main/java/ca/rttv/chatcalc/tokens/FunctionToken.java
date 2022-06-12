package ca.rttv.chatcalc.tokens;

import java.util.HashMap;
import java.util.Map;

public final class FunctionToken implements Token {
    private static final Map<String, DoubleToDoubleFunction> functions;

    static {
        functions = new HashMap<>();
        functions.put("sqrt", Math::sqrt);
        functions.put("cbrt", Math::cbrt);
        functions.put("sin", Math::sin);
        functions.put("asin", Math::asin);
        functions.put("arcsin", Math::asin);
        functions.put("cos", Math::cos);
        functions.put("acos", Math::acos);
        functions.put("arccos", Math::acos);
        functions.put("tan", Math::tan);
        functions.put("atan", Math::atan);
        functions.put("arctan", Math::atan);
        functions.put("sec", val -> 1 / Math.cos(val));
        functions.put("csc", val -> 1 / Math.sin(val));
        functions.put("cot", val -> 1 / Math.tan(val));
        functions.put("floor", Math::floor);
        functions.put("ceil", Math::ceil);
        functions.put("abs", Math::abs);
        functions.put("log", Math::log10);
        functions.put("ln", Math::log);
    }

    public final String func;

    public FunctionToken(String value) {
        this.func = value;
    }

    public double apply(double value) {
        return functions.get(func).apply(value);
    }

    public double log(double base, double value) {
        return Math.log10(value) / Math.log10(base);
    }
}