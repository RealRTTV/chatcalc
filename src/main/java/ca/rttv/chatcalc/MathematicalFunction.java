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

        functions.put("sin", val -> Math.sin(Config.convertFromDegrees(val)));
        functions.put("cos", val -> Math.cos(Config.convertFromDegrees(val)));
        functions.put("tan", val -> Math.tan(Config.convertFromDegrees(val)));
        functions.put("csc", val -> 1 / Math.sin(Config.convertFromDegrees(val)));
        functions.put("sec", val -> 1 / Math.cos(Config.convertFromDegrees(val)));
        functions.put("cot", val -> 1 / Math.tan(Config.convertFromDegrees(val)));

        functions.put("arcsin", val -> Config.convertFromRadians(Math.asin(val)));
        functions.put("asin", val -> Config.convertFromRadians(Math.asin(val)));

        functions.put("acos", val -> Config.convertFromRadians(Math.acos(val)));
        functions.put("arccos", val -> Config.convertFromRadians(Math.acos(val)));

        functions.put("atan", val -> Config.convertFromRadians(Math.atan(val)));
        functions.put("arctan", val -> Config.convertFromRadians(Math.atan(val)));

        functions.put("arccsc", val -> Config.convertFromRadians(Math.asin(1 / val)));
        functions.put("acsc", val -> Config.convertFromRadians(Math.asin(1/ val)));

        functions.put("arcsec", val -> Config.convertFromRadians(Math.acos(1 / val)));
        functions.put("asec", val -> Config.convertFromRadians(Math.acos(1/ val)));

        functions.put("arccot", val -> Config.convertFromRadians(Math.atan(1 / val)));
        functions.put("acot", val -> Config.convertFromRadians(Math.atan(1/ val)));

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