package ca.rttv.chatcalc;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class NibbleMathEngine implements MathEngine {
    byte[] bytes;
    int idx;
    FunctionParameter[] params;
    double x, y, z;

    @Override
    public double eval(String input, FunctionParameter[] paramaters) {
        bytes = fixParenthesis(input).concat("\0").getBytes(StandardCharsets.US_ASCII); // we shouldn't encounter unicode in our math
        idx = 0;
        params = paramaters;
        //noinspection DataFlowIssue -- player != null when the chat != null
        Vec3d pos = MinecraftClient.getInstance().player.getPos();
        x = pos.x;
        y = pos.y;
        z = pos.z;
        double result = expression(false);
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

    private double expression(boolean abs) {
        double x = modulo(abs);
        while (true) {
            if (bite('+')) x += modulo(abs);
            else if (bite('-')) x -= modulo(abs);
            else return x;
        }
    }

    private double modulo(boolean abs) {
        double x = term(abs);
        while (true) {
            if (bite('%')) x %= term(abs);
            else return x;
        }
    }

    private double term(boolean abs) {
        double x = grouping(abs);
        while (true) {
            if (bite('*')) x *= grouping(abs);
            else if (bite('/')) x /= grouping(abs);
            else if (bytes[idx] <= '9' & bytes[idx] >= '0') x *= expression(abs);
            else if (!abs & bytes[idx] == '|') x *= Math.abs(expression(false)); // simplify to false
            else return x;
        }
    }

    private double grouping(boolean abs) {
        double x = part(abs);
        while (true) {
            if (bytes[idx] == '(') x *= expression(abs);
            else return x;
        }
    }

    private double part(boolean abs) {
        long sign = 0L;
        while (bytes[idx] == '+' | bytes[idx] == '-') {
            if (bytes[idx++] == '-') {
                sign ^= 0x8000_0000_0000_0000L;
            }
        }

        double x = 1.0;

        a:
        {
            if (bite('(')) {
                x = expression(false);
                if (!bite(')')) throw new IllegalArgumentException("Expected closing parenthesis");
            } else if (!abs && bite('|')) {
                x = Math.abs(expression(true));
                if (!bite('|')) throw new IllegalArgumentException("Expected closing absolute value character");
            } else if ((bytes[idx] <= '9' & bytes[idx] >= '0') | bytes[idx] == '.' | bytes[idx] == ',') {
                int start = idx;
                while ((bytes[idx] <= '9' & bytes[idx] >= '0') | bytes[idx] == '.' | bytes[idx] == ',') idx++;
                x = Double.parseDouble(new String(bytes, start, idx - start, StandardCharsets.US_ASCII).replace(",", ""));
            } else if (bytes[idx] <= 'z' & bytes[idx] >= 'a') {
                int start = idx;
                while (bytes[idx] <= 'z' & bytes[idx] >= 'a' | bytes[idx] == '_') idx++;
                String func = new String(bytes, start, idx - start, StandardCharsets.US_ASCII);
                double u = 1.0;
                b:
                while (true) {
                    if (func.startsWith("pi")) {
                        x *= u;
                        u = Math.PI;
                        func = func.substring(2);
                        continue;
                    }
                    if (func.startsWith("e")) {
                        x *= u;
                        u = Math.E;
                        func = func.substring(1);
                        continue;
                    }
                    if (func.startsWith("x")) {
                        x *= u;
                        u = this.x;
                        func = func.substring(1);
                        continue;
                    }
                    if (func.startsWith("y")) {
                        x *= u;
                        u = this.y;
                        func = func.substring(1);
                        continue;
                    }
                    if (func.startsWith("z")) {
                        x *= u;
                        u = this.z;
                        func = func.substring(1);
                        continue;
                    }
                    for (FunctionParameter param : params) {
                        if (func.startsWith(param.name())) {
                            x *= u;
                            u = param.value();
                            func = func.substring(param.name().length());
                            continue b;
                        }
                    }
                    if (func.isEmpty()) {
                        if (bite('^')) u = Math.pow(u, part(false));
                        x *= u;
                        break a;
                    }
                    break;
                }
                if (func.equals("log_")) {
                    double base = part(false);
                    if (!bite('(')) throw new IllegalArgumentException("Expected parenthesis for logarithmic function");
                    double value = expression(false);
                    if (!bite(')'))
                        throw new IllegalArgumentException("Expected closing parenthesis for logarithmic function");
                    x *= MathematicalFunction.log(base, value);
                    break a;
                }
                int param_count = 1;
                double exponent = 1.0;
                if (bite('^')) exponent = part(false);
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
                    values[value_count++] = expression(false);
                    if (bite(')')) break;
                    if (!bite(';')) throw new AssertionError("Expected that a semicolon exists between the parameters");
                }
                x *= Math.pow(new MathematicalFunction(func).apply(values), exponent);
            } else
                throw new IllegalArgumentException("Expected a valid character for equation, not " + (char) bytes[idx]);
        }

        if (bite('^')) x = Math.pow(x, part(false));

        return Double.longBitsToDouble(Double.doubleToLongBits(x) | sign);
    }
}
