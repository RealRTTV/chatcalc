package ca.rttv.chatcalc;

import net.minecraft.util.math.MathHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.function.DoubleUnaryOperator;

public final class MathematicalFunction {
    public static final Map<String, DoubleUnaryOperator> FUNCTIONS;

    static {
        FUNCTIONS = new HashMap<>();
        FUNCTIONS.put("sqrt", Math::sqrt);
        FUNCTIONS.put("cbrt", Math::cbrt);

        FUNCTIONS.put("sin", val -> Math.sin(Config.convertToRadians(val)));
        FUNCTIONS.put("cos", val -> Math.cos(Config.convertToRadians(val)));
        FUNCTIONS.put("tan", val -> Math.tan(Config.convertToRadians(val)));
        FUNCTIONS.put("csc", val -> 1 / Math.sin(Config.convertToRadians(val)));
        FUNCTIONS.put("sec", val -> 1 / Math.cos(Config.convertToRadians(val)));
        FUNCTIONS.put("cot", val -> 1 / Math.tan(Config.convertToRadians(val)));

        FUNCTIONS.put("arcsin", val -> Config.convertFromRadians(Math.asin(val)));
        FUNCTIONS.put("asin", val -> Config.convertFromRadians(Math.asin(val)));

        FUNCTIONS.put("acos", val -> Config.convertFromRadians(Math.acos(val)));
        FUNCTIONS.put("arccos", val -> Config.convertFromRadians(Math.acos(val)));

        FUNCTIONS.put("atan", val -> Config.convertFromRadians(Math.atan(val)));
        FUNCTIONS.put("arctan", val -> Config.convertFromRadians(Math.atan(val)));

        FUNCTIONS.put("arccsc", val -> Config.convertFromRadians(Math.asin(1 / val)));
        FUNCTIONS.put("acsc", val -> Config.convertFromRadians(Math.asin(1/ val)));

        FUNCTIONS.put("arcsec", val -> Config.convertFromRadians(Math.acos(1 / val)));
        FUNCTIONS.put("asec", val -> Config.convertFromRadians(Math.acos(1/ val)));

        FUNCTIONS.put("arccot", val -> Config.convertFromRadians(Math.atan(1 / val)));
        FUNCTIONS.put("acot", val -> Config.convertFromRadians(Math.atan(1/ val)));

        FUNCTIONS.put("floor", Math::floor);
        FUNCTIONS.put("ceil", Math::ceil);
        FUNCTIONS.put("round", x -> Math.floor(x + 0.5d));
        FUNCTIONS.put("abs", Math::abs);
        FUNCTIONS.put("log", Math::log10);
        FUNCTIONS.put("ln", Math::log);
        FUNCTIONS.put("exp", Math::exp);

        FUNCTIONS.put("min", DoubleUnaryOperator.identity());
        FUNCTIONS.put("max", DoubleUnaryOperator.identity());
        FUNCTIONS.put("clamp", DoubleUnaryOperator.identity());
        FUNCTIONS.put("cmp", DoubleUnaryOperator.identity());
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
        if (func.equals("cmp")) {
            if (values.length < 2 || values.length > 3) {
                throw new IllegalArgumentException();
            }
            double epsilon = values.length == 2 ? 0.0 : values[2];
            double a = values[0];
            double b = values[1];
            if (Math.abs(a - b) <= epsilon) {
                return 0.0;
            } else if (a < b) {
                return -1.0;
            } else if (a > b) {
                return 1.0;
            } else {
                return 0.0;
            }
        }

        if (func.equals("min")) {
            if (values.length == 0) {
                throw new IllegalArgumentException();
            }
            double min = values[0];
            for (double value : values) {
                min = Math.min(min, value);
            }
            return min;
        }

        if (func.equals("max")) {
            if (values.length == 0) {
                throw new IllegalArgumentException();
            }
            double max = values[0];
            for (double value : values) {
                max = Math.max(max, value);
            }
            return max;
        }

        if (func.equals("clamp")) {
            if (values.length != 3) {
                throw new IllegalArgumentException();
            }
            return MathHelper.clamp(values[0], values[1], values[2]);
        }

        if (FUNCTIONS.containsKey(func)) {
            if (values.length != 1) {
                throw new IllegalArgumentException();
            }

            return FUNCTIONS.get(func).applyAsDouble(values[0]);
        } else {
            return Config.func(func, values);
        }
    }

    public static double log(double base, double value) {
        return Math.log(value) / Math.log(base);
    }
}