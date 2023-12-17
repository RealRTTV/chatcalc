package ca.rttv.chatcalc;

import com.google.common.collect.Streams;
import com.mojang.datafixers.util.Pair;
import net.minecraft.util.math.MathHelper;

import java.nio.charset.StandardCharsets;

import static ca.rttv.chatcalc.MathematicalFunction.factorial;
import static ca.rttv.chatcalc.MathematicalFunction.mod;

public class NibbleMathEngine implements MathEngine {
    byte[] bytes;
    int idx;
    FunctionParameter[] params;
    boolean abs;

    @Override
    public double eval(String input, FunctionParameter[] paramaters) {
        bytes = fixParenthesis(input).concat("\0").getBytes(StandardCharsets.US_ASCII); // we shouldn't encounter unicode in our math
        idx = 0;
        abs = false;
        params = paramaters;
        double result = expression();
        if (idx + 1 != bytes.length) {
            throw new IllegalArgumentException("Evaluation had unexpected remaining characters");
        }
        return result;
    }

    private String fixParenthesis(String input) {
        int openingMissing = 0;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == ')') {
                openingMissing++;
            } else if (c == '(') {
                break;
            }
        }

        int closingMissing = 0;
        for (int i = input.length() - 1; i >= 0; i--) {
            char c = input.charAt(i);
            if (c == '(') {
                closingMissing++;
            } else if (c == ')') {
                break;
            }
        }
        input = "(".repeat(openingMissing) + input + ")".repeat(closingMissing);

        int opening = 0;
        int closing = 0;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '(') {
                opening++;
            } else if (c == ')') {
                closing++;
            }
        }

        return "(".repeat(Math.max(0, closing - opening)) + input + ")".repeat(Math.max(0, opening - closing));
    }

    private boolean bite(char bite) { // pun intended
        if (bytes[idx] == bite) {
            idx++;
            return true;
        } else {
            return false;
        }
    }

    private double expression() {
        double x = modulo();
        while (true) {
            if (bite('+')) x += modulo();
            else if (bite('-')) x -= modulo();
            else return x;
        }
    }

    private double modulo() {
        double x = term();
        while (true) {
            if (bite('%')) x = mod(x, term());
            else return x;
        }
    }

    private double term() {
        double x = grouping();
        while (true) {
            if (bite('*')) x *= grouping();
            else if (bite('/')) x /= grouping();
            else return x;
        }
    }

    private double grouping() {
        long sign = 0L;
        while (bytes[idx] == '+' | bytes[idx] == '-') {
            if (bytes[idx++] == '-') {
                sign ^= 0x8000_0000_0000_0000L;
            }
        }

        double x = part();
        while (isStartOfPart(bytes[idx])) {
            x *= part();
        }

        return Double.longBitsToDouble(Double.doubleToLongBits(x) ^ sign);
    }

    private double part() {
        double x;
        a: {
            if (bite('(')) {
                boolean absBefore = abs;
                abs = false;
                x = expression();
                if (!bite(')')) throw new IllegalArgumentException("Expected closing parenthesis");
                abs = absBefore;
                break a;
            }

            if (!abs && bite('|')) {
                boolean absBefore = abs;
                abs = true;
                x = Math.abs(expression());
                if (!bite('|')) throw new IllegalArgumentException("Expected closing absolute value character");
                abs = absBefore;
                break a;
            }

            if ((bytes[idx] <= '9' & bytes[idx] >= '0') | bytes[idx] == '.' | bytes[idx] == ',') {
                int start = idx;
                while ((bytes[idx] <= '9' & bytes[idx] >= '0') | bytes[idx] == '.' | bytes[idx] == ',') idx++;
                x = Double.parseDouble(new String(bytes, start, idx - start, StandardCharsets.US_ASCII).replace(",", ""));
                break a;
            }

            if ((bytes[idx] >= 'a' & bytes[idx] <= 'z') | (bytes[idx] >= 'A' & bytes[idx] <= 'Z')) {
                int start = idx;
                while ((bytes[idx] >= 'a' & bytes[idx] <= 'z') | (bytes[idx] >= 'A' & bytes[idx] <= 'Z')) idx++;
                if (bytes[idx] == '_') idx++;
                String str = new String(bytes, start, idx - start, StandardCharsets.US_ASCII);

                if (Streams.concat(Config.FUNCTIONS.keySet().stream().map(Pair::getFirst), MathematicalFunction.FUNCTIONS.keySet().stream()).noneMatch(str::startsWith)) {
                    for (FunctionParameter param : params) {
                        if (str.startsWith(param.name())) {
                            idx -= str.length() - param.name().length();
                            x = param.value();
                            break a;
                        }
                    }

                    for (MathematicalConstant constant : MathematicalConstant.CONSTANTS) {
                        if (str.startsWith(constant.name())) {
                            idx -= str.length() - constant.name().length();
                            x = constant.value();
                            break a;
                        }
                    }

                    for (CustomConstant constant : Config.CONSTANTS.values()) {
                        if (str.startsWith(constant.name())) {
                            idx -= str.length() - constant.name().length();
                            x = constant.get();
                            break a;
                        }
                    }
                }

                if (str.equals("sum")) {
                    boolean absBefore = abs;
                    abs = false;
                    if (!bite('(')) throw new IllegalArgumentException("Expected parenthesis for summation");
                    start = idx;
                    while ((bytes[idx] >= 'a' & bytes[idx] <= 'z') | (bytes[idx] >= 'A' & bytes[idx] <= 'Z')) idx++;
                    String param = new String(bytes, start, idx - start, StandardCharsets.US_ASCII);
                    if (!bite('=')) throw new IllegalArgumentException("Expected starting value for parameter in summation");
                    int lowerBound = MathHelper.floor(expression());
                    if (!bite(';')) throw new IllegalArgumentException("Expected multiple parameters in summation");
                    int upperBound = MathHelper.floor(expression());
                    if (!bite(';')) throw new IllegalArgumentException("Expected multiple parameters in summation");
                    start = idx;
                    for (int parenthesis = 0; parenthesis >= 0; idx++) {
                        byte c = bytes[idx];
                        if (c == '(') {
                            parenthesis++;
                        } else if (c == ')') {
                            parenthesis--;
                        } else if (c == '\0') {
                            throw new IllegalArgumentException("Expected closing parenthesis in summation");
                        }
                    }
                    String expression = new String(bytes, start, idx - 1 - start, StandardCharsets.US_ASCII);
                    double sum = 0.0;
                    FunctionParameter[] summationParams = new FunctionParameter[params.length + 1];
                    System.arraycopy(params, 0, summationParams, 0, params.length);
                    for (int i = lowerBound; i <= upperBound; i++) {
                        summationParams[params.length] = new FunctionParameter(param, i);
                        sum += Config.makeEngine().eval(expression, summationParams);
                    }
                    abs = absBefore;
                    x = sum;
                    break a;
                }

                if (str.equals("prod")) {
                    boolean absBefore = abs;
                    abs = false;
                    if (!bite('(')) throw new IllegalArgumentException("Expected parenthesis for product");
                    start = idx;
                    while ((bytes[idx] >= 'a' & bytes[idx] <= 'z') | (bytes[idx] >= 'A' & bytes[idx] <= 'Z')) idx++;
                    String param = new String(bytes, start, idx - start, StandardCharsets.US_ASCII);
                    if (!bite('=')) throw new IllegalArgumentException("Expected starting value for parameter in product");
                    int lowerBound = MathHelper.floor(expression());
                    if (!bite(';')) throw new IllegalArgumentException("Expected multiple parameters in product");
                    int upperBound = MathHelper.floor(expression());
                    if (!bite(';')) throw new IllegalArgumentException("Expected multiple parameters in product");
                    start = idx;
                    for (int parenthesis = 0; parenthesis >= 0; idx++) {
                        byte c = bytes[idx];
                        if (c == '(') {
                            parenthesis++;
                        } else if (c == ')') {
                            parenthesis--;
                        } else if (c == '\0') {
                            throw new IllegalArgumentException("Expected closing parenthesis in product");
                        }
                    }
                    String expression = new String(bytes, start, idx - 1 - start, StandardCharsets.US_ASCII);
                    double prod = 1.0;
                    FunctionParameter[] productParams = new FunctionParameter[params.length + 1];
                    System.arraycopy(params, 0, productParams, 0, params.length);
                    for (int i = lowerBound; i <= upperBound; i++) {
                        productParams[params.length] = new FunctionParameter(param, i);
                        prod *= Config.makeEngine().eval(expression, productParams);
                    }
                    abs = absBefore;
                    x = prod;
                    break a;
                }

                if (str.equals("log_")) {
                    boolean absBefore = abs;
                    abs = false;
                    // cannot be grouping because `log_2(3)` becomes `log_6`
                    double base = part();
                    if (!bite('(')) throw new IllegalArgumentException("Expected parenthesis for logarithmic function");
                    double value = expression();
                    if (!bite(')'))
                        throw new IllegalArgumentException("Expected closing parenthesis for logarithmic function");
                    abs = absBefore;
                    x = MathematicalFunction.log(base, value);
                    break a;
                }

                {
                    boolean absBefore = abs;
                    abs = false;
                    int param_count = 1;
                    // cannot be grouping because `sqrt^2(3)` becomes `sqrt^6`
                    double exponent = bite('^') ? part() : 1.0d;
                    if (!bite('(')) throw new IllegalArgumentException("Expected parenthesis for function");
                    int depth = 0;
                    int before = idx;
                    while (bytes[idx] != '\0') {
                        if (bytes[idx] == ChatCalc.SEPARATOR_CHAR & depth == 0) param_count++;
                        else if (bytes[idx] == ')') {
                            if (depth-- == 0) break;
                        } else if (bytes[idx] == '(') depth++;
                        idx++;
                    }
                    idx = before;
                    double[] values = new double[param_count];
                    int value_count = 0;
                    while (true) {
                        if (bite('\0')) throw new IllegalArgumentException("Expected closing parenthesis for function");
                        values[value_count++] = expression();
                        if (bite(')')) break;
                        if (!bite(';'))
                            throw new IllegalArgumentException("Expected that a semicolon exists between the parameters");
                    }
                    abs = absBefore;
                    x = Math.pow(new MathematicalFunction(str).apply(values), exponent);
                    break a;
                }
            }

            throw new IllegalArgumentException("Expected a valid character for equation, not '" + (char) bytes[idx] + "' (at index " + idx + ")");
        };

        if (bite('!')) x = factorial(x);
        if (bite('^')) {
            boolean absBefore = abs;
            abs = false;
            x = Math.pow(x, grouping());
            abs = absBefore;
        }

        return x;
    }

    private boolean isStartOfPart(byte c) {
        return (c >= 'a' & c <= 'z') | (c >= '0' & c <= '9') | (c == '(') | (c == '|' & !abs);
    }

    @Override
    public String toString() {
        return new String(bytes, idx, bytes.length - idx - 1);
    }
}
