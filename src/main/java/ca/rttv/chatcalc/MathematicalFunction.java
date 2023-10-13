package ca.rttv.chatcalc;

import java.util.HashMap;
import java.util.Map;
import java.util.function.DoubleUnaryOperator;

public final class MathematicalFunction {
    private static final Map<String, DoubleUnaryOperator> functions;

    static {
        functions = new HashMap<>();
        functions.put("sqrt", Math::sqrt);
        functions.put("cbrt", Math::cbrt);
        functions.put("sin", val -> Math.sin(Config.convertIfRadians(val)));
        functions.put("asin", val -> Math.asin(Config.convertIfRadians(val)));
        functions.put("arcsin", val -> Math.asin(Config.convertIfRadians(val)));
        functions.put("cos", val -> Math.cos(Config.convertIfRadians(val)));
        functions.put("acos", val -> Math.acos(Config.convertIfRadians(val)));
        functions.put("arccos", val -> Math.acos(Config.convertIfRadians(val)));
        functions.put("tan", val -> Math.tan(Config.convertIfRadians(val)));
        functions.put("atan", val -> Math.atan(Config.convertIfRadians(val)));
        functions.put("arctan", val -> Math.atan(Config.convertIfRadians(val)));
        functions.put("sec", val -> 1 / Math.cos(Config.convertIfRadians(val)));
        functions.put("csc", val -> 1 / Math.sin(Config.convertIfRadians(val)));
        functions.put("cot", val -> 1 / Math.tan(Config.convertIfRadians(val)));
        functions.put("floor", Math::floor);
        functions.put("ceil", Math::ceil);
        functions.put("round", x -> Math.floor(x + 0.5d));
        functions.put("abs", Math::abs);
        functions.put("log", Math::log10);
        functions.put("ln", Math::log);
    }

    public final String func;

    @Override
    public String toString() {
        return func;
    }

    public MathematicalFunction(String value) {
        func = value;
    }

    public double apply(double... values) {
        if (functions.containsKey(func)) {
            if (values.length != 1) {
                throw new IllegalArgumentException();
            }
            return functions.get(func).applyAsDouble(values[0]);
        } else {
            return Config.func(func, values);
        }
    }

    public static double log(double base, double value) {
        return Math.log(value) / Math.log(base);
    }
}