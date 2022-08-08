package ca.rttv.chatcalc.tokens;

import ca.rttv.chatcalc.EventHandler;
import net.minecraft.text.LiteralTextContent;
import net.minecraft.text.TextContent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.DoubleUnaryOperator;

public final class FunctionToken implements Token {
    private static final Map<String, DoubleUnaryOperator> functions;

    static {
        functions = new HashMap<>();
        functions.put("sqrt", Math::sqrt);
        functions.put("cbrt", Math::cbrt);
        functions.put("sin", val -> Math.sin(EventHandler.convertIfRadians(val)));
        functions.put("asin", val -> Math.asin(EventHandler.convertIfRadians(val)));
        functions.put("arcsin", val -> Math.asin(EventHandler.convertIfRadians(val)));
        functions.put("cos", val -> Math.cos(EventHandler.convertIfRadians(val)));
        functions.put("acos", val -> Math.acos(EventHandler.convertIfRadians(val)));
        functions.put("arccos", val -> Math.acos(EventHandler.convertIfRadians(val)));
        functions.put("tan", val -> Math.tan(EventHandler.convertIfRadians(val)));
        functions.put("atan", val -> Math.atan(EventHandler.convertIfRadians(val)));
        functions.put("arctan", val -> Math.atan(EventHandler.convertIfRadians(val)));
        functions.put("sec", val -> 1 / Math.cos(EventHandler.convertIfRadians(val)));
        functions.put("csc", val -> 1 / Math.sin(EventHandler.convertIfRadians(val)));
        functions.put("cot", val -> 1 / Math.tan(EventHandler.convertIfRadians(val)));
        functions.put("floor", Math::floor);
        functions.put("ceil", Math::ceil);
        functions.put("abs", Math::abs);
        functions.put("log", Math::log10);
        functions.put("ln", Math::log);
    }

    public final String func;

    @Override
    public String toString() {
        return "\033[0;33m" + func;
    }

    public FunctionToken(String value) {
        func = value;
    }

    public double apply(double value) {
        if (func.startsWith("log_")) {
            return log(Double.parseDouble(func.substring(4)), value);
        }
        return functions.get(func).applyAsDouble(value);
    }

    public static double log(double base, double value) {
        return Math.log10(value) / Math.log10(base);
    }

    @Override
    public TextContent getText() {
        return new LiteralTextContent("§e" + func);
    }
}