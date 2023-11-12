package ca.rttv.chatcalc;

import java.util.Optional;

public record CallableFunction(String name, String rest, String[] params) {
    public static Optional<CallableFunction> fromString(String str) {
        int functionNameEnd = str.indexOf('(');
        if (functionNameEnd <= 0) {
            return Optional.empty();
        }
        String functionName = str.substring(0, functionNameEnd);
        int paramsEnd = str.substring(functionNameEnd).indexOf(')') + functionNameEnd;
        if (!(functionName.matches("[A-Za-z]+") && paramsEnd > 0 && str.substring(paramsEnd + 1).startsWith("=") && str.length() > paramsEnd + 2)) {
            return Optional.empty();
        }
        String[] params = str.substring(functionNameEnd + 1, paramsEnd).split(ChatCalc.SEPARATOR);
        for (String param : params) {
            if (!param.matches("[A-Za-z]")) {
                return Optional.empty();
            }
        }
        String rest = str.substring(paramsEnd + 2);
        return Optional.of(new CallableFunction(functionName, rest, params));
    }

    @Override
    public String toString() {
        return name + '(' + String.join(ChatCalc.SEPARATOR, params) + ")=" + rest;
    }
}
