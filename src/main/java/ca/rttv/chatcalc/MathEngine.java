package ca.rttv.chatcalc;

import ca.rttv.chatcalc.tokens.*;

import java.util.ArrayList;
import java.util.List;

public class MathEngine {
    public static double eval(String input) {
        input = input.toLowerCase().replaceAll("pi", "3.141592653589793").replaceAll("\\*\\*", "^").replaceAll("(?!c)e(?!il)", "2.718281828459045").replaceAll(",(?! )", "");
        List<Token> tokens = new ArrayList<>(input.length()); // array of custom objects which do different things each
        Class<? extends Token> currentType = null;
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (isNumber(c) && currentType != FunctionToken.class) { // this exclusion is so log_ custom bases work
                sb.append(c);
                currentType = NumberToken.class;
                continue;
            }

            if (isOperator(c)) {
                if (sb.length() > 0) { // this segment on every one of these is to finish a number token
                    tokens.add(makeToken(currentType, sb.toString()));
                }
                sb = new StringBuilder().append(c); // this resets it after the number token has been done
                currentType = OperatorToken.class;
                tokens.add(makeToken(currentType, sb.toString()));
                sb = new StringBuilder();
                continue;
            }

            if (isBracket(c)) {
                if (sb.length() > 0) { // this segment on every one of these is to finish a number token
                    tokens.add(makeToken(currentType, sb.toString()));
                }
                sb = new StringBuilder().append(c); // this resets it after the number token has been done
                currentType = BracketToken.class;
                tokens.add(makeToken(currentType, sb.toString()));
                sb = new StringBuilder();
                continue;
            }

            if (isAbs(c)) {
                if (sb.length() > 0) { // this segment on every one of these is to finish a number token
                    tokens.add(makeToken(currentType, sb.toString()));
                }
                sb = new StringBuilder().append(c); // this resets it after the number token has been done
                currentType = AbsToken.class;
                tokens.add(makeToken(currentType, sb.toString()));
                sb = new StringBuilder();
                continue;
            }

            sb.append(c); // if something is not a member of another type, it just assumes its an unknown function
            currentType = FunctionToken.class;
        }

        if (sb.length() > 0) { // this segment on every one of these is to finish a number token
            tokens.add(makeToken(currentType, sb.toString()));
        }

        return eval(tokens, false);
    }

    private static boolean isAbs(char c) {
        return c == 124;
    }

    @SuppressWarnings("SuspiciousListRemoveInLoop")
    private static double eval(List<Token> tokens, boolean abs) { // recursive evaluation with bracket support!!!
        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if (token instanceof BracketToken bracketToken) {
                tokens.remove(i);

                if (!bracketToken.isOpen) {
                    return ((NumberToken) tokens.get(0)).value;
                }

                eval(tokens.subList(i, tokens.size()), abs);
            } else if (token instanceof OperatorToken operatorToken) {
                Token before = i == 0 ? null : tokens.get(i - 1);
                if (before instanceof NumberToken) {
                    double prev = ((NumberToken) before).value;
                    double next = eval(tokens.subList(i + 1, tokens.size()), abs);
                    tokens.set(i - 1, new NumberToken(operatorToken.apply(prev, next)));
                    tokens.remove(i);
                    tokens.remove(i);
                } else if (operatorToken.value == 45) {
                    double next = eval(tokens.subList(i + 1, tokens.size()), abs);
                    tokens.set(i, new NumberToken(-next));
                    tokens.remove(i + 1);
                }
            } else if (token instanceof FunctionToken functionToken) {
                double input = eval(tokens.subList(i + 1, tokens.size()), abs);
                double num = functionToken.func.length() > 3 && functionToken.func.charAt(3) == 95 ? functionToken.log(Double.parseDouble(functionToken.func.substring(4)), input) : functionToken.apply(input);
                tokens.set(i, new NumberToken(num));
            } else if (token instanceof AbsToken absToken) {
                if (abs) {
                    tokens.remove(i);
                    return ((NumberToken) tokens.get(0)).value;
                }

                tokens.set(i, new NumberToken(absToken.apply(eval(tokens.subList(i + 1, tokens.size()), true))));
            }
        }

        return ((NumberToken) tokens.get(0)).value;
    }

    private static boolean isBracket(char c) {
        return c == 40 || c == 41;
    }

    private static <T extends Token> Token makeToken(Class<T> clazz, String param) {
        if (clazz == null) return null;
        return switch (clazz.getName()) {
            case "ca.rttv.chatcalc.tokens.NumberToken" -> new NumberToken(Double.parseDouble(param));
            case "ca.rttv.chatcalc.tokens.OperatorToken" -> new OperatorToken(param.charAt(0));
            case "ca.rttv.chatcalc.tokens.BracketToken" -> new BracketToken(param.charAt(0));
            case "ca.rttv.chatcalc.tokens.FunctionToken" -> new FunctionToken(param);
            case "ca.rttv.chatcalc.tokens.AbsToken" -> new AbsToken();
            default -> null;
        };
    }

    private static boolean isOperator(char c) {
        return c == 42 || c == 43 || c == 45 || c == 47 || c == 94 || c == 37;
    }

    private static boolean isNumber(char c) {
        return c >= 48 && c <= 57 || c == 46;
    }
}
