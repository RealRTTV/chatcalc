package ca.rttv.chatcalc;

import com.google.common.collect.Streams;

import java.nio.charset.StandardCharsets;

import static ca.rttv.chatcalc.MathematicalFunction.factorial;

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
        int n = 0;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '(') {
                n++;
            } else if (c == ')') {
                n--;
            }
        }
        if (n < 0) {
            return "(".repeat(-n).concat(input);
        } else if (n > 0) {
            return input.concat(")".repeat(n));
        } else {
            return input;
        }
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
            if (bite('%')) x = Math.IEEEremainder(x, term());
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
        double x = meat();

        if (bite('!')) x = factorial(x);
        if (bite('^')) {
            boolean absBefore = abs;
            abs = false;
            x = Math.pow(x, grouping());
            abs = absBefore;
        }

        return x;
    }

    // should not be called at all, except once in `part`, meant for easy return statement usage, not to be called
    private double meat() {
        if (bite('(')) {
            boolean absBefore = abs;
            abs = false;
            double x = expression();
            if (!bite(')')) throw new IllegalArgumentException("Expected closing parenthesis");
            abs = absBefore;
            return x;
        }

        if (!abs && bite('|')) {
            boolean absBefore = abs;
            abs = true;
            double x = Math.abs(expression());
            if (!bite('|')) throw new IllegalArgumentException("Expected closing absolute value character");
            abs = absBefore;
            return x;
        }

        if ((bytes[idx] <= '9' & bytes[idx] >= '0') | bytes[idx] == '.' | bytes[idx] == ',') {
            int start = idx;
            while ((bytes[idx] <= '9' & bytes[idx] >= '0') | bytes[idx] == '.' | bytes[idx] == ',') idx++;
            return Double.parseDouble(new String(bytes, start, idx - start, StandardCharsets.US_ASCII).replace(",", ""));
        }

        if (bytes[idx] >= 'a' & bytes[idx] <= 'z') {
            int start = idx;
            while (bytes[idx] <= 'z' & bytes[idx] >= 'a') idx++;
            if (bytes[idx] == '_') idx++;
            String func = new String(bytes, start, idx - start, StandardCharsets.US_ASCII);

            if (Streams.concat(Config.FUNCTIONS.keySet().stream(), MathematicalFunction.FUNCTIONS.keySet().stream()).noneMatch(func::startsWith)) {
                for (FunctionParameter param : params) {
                    if (func.startsWith(param.name())) {
                        idx -= func.length() - param.name().length();
                        return param.value();
                    }
                }

                for (MathematicalConstant constant : MathematicalConstant.CONSTANTS) {
                    if (func.startsWith(constant.name())) {
                        idx -= func.length() - constant.name().length();
                        return constant.value();
                    }
                }

                for (CustomConstant constant : Config.CONSTANTS.values()) {
                    if (func.startsWith(constant.name())) {
                        idx -= func.length() - constant.name().length();
                        return constant.value();
                    }
                }
            }

            if (func.equals("log_")) {
                boolean absBefore = abs;
                abs = false;
                double base = part();
                if (!bite('(')) throw new IllegalArgumentException("Expected parenthesis for logarithmic function");
                double value = expression();
                if (!bite(')')) throw new IllegalArgumentException("Expected closing parenthesis for logarithmic function");
                abs = absBefore;
                return MathematicalFunction.log(base, value);
            }

            {
                boolean absBefore = abs;
                abs = false;
                int param_count = 1;
                double exponent = bite('^') ? grouping() : 1.0d;
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
                return Math.pow(new MathematicalFunction(func).apply(values), exponent);
            }
        }

        throw new IllegalArgumentException("Expected a valid character for equation, not '" + (char) bytes[idx] + "' (at index " + idx + ")");
    }

    private boolean isStartOfPart(byte c) {
        return (c >= 'a' & c <= 'z') | (c >= '0' & c <= '9') | (c == '(') | (c == '|' & !abs);
    }

    @Override
    public String toString() {
        return new String(bytes, idx, bytes.length - idx - 1);
    }
}
