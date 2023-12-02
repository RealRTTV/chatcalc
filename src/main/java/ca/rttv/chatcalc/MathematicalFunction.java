package ca.rttv.chatcalc;

import com.google.common.math.DoubleMath;
import net.minecraft.util.math.MathHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;
import java.util.stream.DoubleStream;

public final class MathematicalFunction {
    public static final Map<String, Function<double[], OptionalDouble>> FUNCTIONS;

    static {
        FUNCTIONS = new HashMap<>();

        FUNCTIONS.put("sqrt", simple(Math::sqrt));
        FUNCTIONS.put("cbrt", simple(Math::cbrt));

        FUNCTIONS.put("sin", simple(val -> Math.sin(Config.convertToRadians(val))));
        FUNCTIONS.put("cos", simple(val -> Math.cos(Config.convertToRadians(val))));
        FUNCTIONS.put("tan", simple(val -> Math.tan(Config.convertToRadians(val))));
        FUNCTIONS.put("csc", simple(val -> 1 / Math.sin(Config.convertToRadians(val))));
        FUNCTIONS.put("sec", simple(val -> 1 / Math.cos(Config.convertToRadians(val))));
        FUNCTIONS.put("cot", simple(val -> 1 / Math.tan(Config.convertToRadians(val))));

        FUNCTIONS.put("arcsin", simple(val -> Config.convertFromRadians(Math.asin(val))));
        FUNCTIONS.put("asin", FUNCTIONS.get("arcsin"));

        FUNCTIONS.put("arccos", simple(val -> Config.convertFromRadians(Math.acos(val))));
        FUNCTIONS.put("acos", FUNCTIONS.get("arccos"));

        FUNCTIONS.put("arctan", simple(val -> Config.convertFromRadians(Math.atan(val))));
        FUNCTIONS.put("atan", FUNCTIONS.get("arctan"));

        FUNCTIONS.put("arccsc", simple(val -> Config.convertFromRadians(Math.asin(1 / val))));
        FUNCTIONS.put("acsc", FUNCTIONS.get("arccsc"));

        FUNCTIONS.put("arcsec", simple(val -> Config.convertFromRadians(Math.acos(1 / val))));
        FUNCTIONS.put("asec", FUNCTIONS.get("arcsec"));

        FUNCTIONS.put("arccot", simple(val -> Config.convertFromRadians(Math.atan(1 / val))));
        FUNCTIONS.put("acot", FUNCTIONS.get("arccot"));

        FUNCTIONS.put("floor", simple(Math::floor));
        FUNCTIONS.put("ceil", simple(Math::ceil));
        FUNCTIONS.put("round", simple(x -> Math.floor(x + 0.5d)));
        FUNCTIONS.put("abs", simple(Math::abs));
        FUNCTIONS.put("log", simple(Math::log10));
        FUNCTIONS.put("ln", simple(Math::log));
        FUNCTIONS.put("exp", simple(Math::exp));

        FUNCTIONS.put("sgn", simple(x -> Double.isNaN(x) || x + 0.0 == 0.0 ? 0.0 : (x >= 0.0 ? 1.0 : -1.0)));
        FUNCTIONS.put("min", values -> DoubleStream.of(values).min());
        FUNCTIONS.put("max", values -> DoubleStream.of(values).max());
        FUNCTIONS.put("gcf", values -> DoubleStream.of(values).reduce(MathematicalFunction::gcf));
        FUNCTIONS.put("lcm", values -> DoubleStream.of(values).reduce(MathematicalFunction::lcm));
        FUNCTIONS.put("clamp", values -> values.length == 3 ? OptionalDouble.of(MathHelper.clamp(values[0], values[1], values[2])) : OptionalDouble.empty());
        FUNCTIONS.put("cmp", values -> (values.length >= 2 && values.length <= 3) ? OptionalDouble.of((Math.abs(values[0] - values[1]) <= (values.length == 2 ? 0.0 : values[2])) ? 0.0d : (values[0] < values[1] ? -1.0d : (values[0] > values[1] ? 1.0d : 0.0d))) : OptionalDouble.empty());
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
        return Optional.ofNullable(FUNCTIONS.get(func)).map(function -> function.apply(values).orElseThrow(IllegalArgumentException::new)).orElseGet(() -> Config.func(func, values));
    }

    public static double gcf(double a, double b) {
        if (b > a) {
            double t = a;
            a = b;
            b = t;
        }

        while (b != 0.0d) {
            double t = b;
            b = mod(a, b);
            a = t;
        }

        return a;
    }

    public static double lcm(double a, double b) {
        return (a * b) / gcf(a, b);
    }

    public static double log(double base, double value) {
        return Math.log(value) / Math.log(base);
    }

    public static double mod(double a, double b) {
        return a % b + (((Double.doubleToLongBits(a) >>> 63) ^ (Double.doubleToLongBits(b) >>> 63)) > 0 ? b : 0);
    }

    public static double factorial(double x) {
        return x % 1.0d == 0.0d & x >= 1.0d
                ? DoubleMath.factorial((int) x)
                : Math.sqrt(2.0 * Math.PI * x)
                * Math.pow(x / Math.E, x)
                * (1.0d
                + 1.0d / (12.0d * x)
                + 1.0d / (288.0d * x * x)
                - 139.0d / (51840.0d * x * x * x)
                - 571.0d / (2488320.0d * x * x * x * x)
                + 163879.0d / (209018880.0d * x * x * x * x * x)
                + 5246819.0d / (75246796800.0d * x * x * x * x * x * x)
                + -534703531.0d / (902961561600.0d * x * x * x * x * x * x * x));
    }

    private static Function<double[], OptionalDouble> simple(DoubleUnaryOperator simple) {
        return values -> values.length == 1 ? OptionalDouble.of(simple.applyAsDouble(values[0])) : OptionalDouble.empty();
    }
}