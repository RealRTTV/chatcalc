package ca.rttv.chatcalc;

import java.util.Optional;

public record CustomFunction(String name, String eval, String[] params) {
    public static Optional<CustomFunction> fromString(String text) {
        int equalsIdx = text.indexOf('=');
        if (equalsIdx == -1) {
            return Optional.empty();
        }
        String lhs = text.substring(0, equalsIdx);
        String rhs = text.substring(equalsIdx + 1);
        if (!ChatCalc.FUNCTION.matcher(lhs).matches()) {
            return Optional.empty();
        }
        int functionNameEnd = lhs.indexOf('(');
        String functionName = lhs.substring(0, functionNameEnd);
        int paramsEnd = lhs.substring(functionNameEnd).indexOf(')') + functionNameEnd;
        String[] params = lhs.substring(functionNameEnd + 1, paramsEnd).split(ChatCalc.SEPARATOR);
        return Optional.of(new CustomFunction(functionName, rhs, params));
    }

    @Override
    public String toString() {
        return name + '(' + String.join(ChatCalc.SEPARATOR, params) + ")=" + eval;
    }
}
