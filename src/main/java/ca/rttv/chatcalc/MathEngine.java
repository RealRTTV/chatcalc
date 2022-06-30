package ca.rttv.chatcalc;

import ca.rttv.chatcalc.tokens.*;
import com.mojang.logging.LogUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.LiteralTextContent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MathEngine {
    public static final Logger LOGGER = LogUtils.getLogger();

    public static double eval(String input) {
        input = input.toLowerCase().replaceAll("pi", "3.141592653589793").replaceAll("\\*\\*", "^").replaceAll("(?!c)e(?!il)", "2.718281828459045").replaceAll(",(?! )", "");
        List<Token> tokens = new ArrayList<>(input.length()); // array of custom objects which do different things each
        Optional<Class<? extends Token>> currentType = Optional.empty();
        StringBuilder sb = new StringBuilder();
        MinecraftClient client = MinecraftClient.getInstance();

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (isNumber(c) && (currentType.isEmpty() || currentType.get() != FunctionToken.class)) { // this exclusion is so log_ custom bases work
                sb.append(c);
                currentType = Optional.of(NumberToken.class);
                continue;
            }

            if (isOperator(c)) {
                if (sb.length() > 0) { // this segment on every one of these is to finish a number token
                    makeToken(currentType, sb.toString()).ifPresent(tokens::add);
                }
                sb = new StringBuilder().append(c); // this resets it after the number token has been done
                currentType = Optional.of(OperatorToken.class);
                makeToken(currentType, sb.toString()).ifPresent(tokens::add);
                sb = new StringBuilder();
                continue;
            }

            if (isBracket(c)) {
                if (sb.length() > 0) { // this segment on every one of these is to finish a number token
                    makeToken(currentType, sb.toString()).ifPresent(tokens::add);
                }
                sb = new StringBuilder().append(c); // this resets it after the number token has been done
                currentType = Optional.of(BracketToken.class);
                makeToken(currentType, sb.toString()).ifPresent(tokens::add);
                sb = new StringBuilder();
                continue;
            }

            if (isAbs(c)) {
                if (sb.length() > 0) { // this segment on every one of these is to finish a number token
                    makeToken(currentType, sb.toString()).ifPresent(tokens::add);
                }
                sb = new StringBuilder().append(c); // this resets it after the number token has been done
                currentType = Optional.of(AbsToken.class);
                makeToken(currentType, sb.toString()).ifPresent(tokens::add);
                sb = new StringBuilder();
                continue;
            }

            sb.append(c); // if something is not a member of another type, it just assumes it's an unknown function
            currentType = Optional.of(FunctionToken.class);
        }

        if (sb.length() > 0) { // this segment on every one of these is to finish a number token
            makeToken(currentType, sb.toString()).ifPresent(tokens::add);
        }

        if (EventHandler.debugTokens() && client.player != null) {
            LOGGER.info(tokens.stream().map(Object::toString).collect(Collectors.joining()));

            MutableText text = MutableText.of(new LiteralTextContent("§r"));
            text.getSiblings().addAll(tokens.stream().map(token -> MutableText.of(token.getText())).toList());
            text.getSiblings().add(Text.literal("§r"));
            client.player.sendMessage(text);
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
                    return ((NumberToken) tokens.get(0)).val();
                }

                eval(tokens.subList(i, tokens.size()), abs);
            } else if (token instanceof OperatorToken operatorToken) {
                Token before = i == 0 ? null : tokens.get(i - 1);
                if (before instanceof NumberToken) {
                    double prev = ((NumberToken) before).val();
                    double next = eval(tokens.subList(i + 1, tokens.size()), abs);
                    tokens.set(i - 1, new NumberToken(operatorToken.apply(prev, next)));
                    tokens.remove(i);
                    tokens.remove(i);
                } else if (operatorToken.val == 45) {
                    double next = tokens.get(i + 1) instanceof NumberToken numberToken ? numberToken.val() : eval(tokens.subList(i + 1, tokens.size()), abs);
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
                    return ((NumberToken) tokens.get(0)).val();
                }

                tokens.set(i, new NumberToken(absToken.apply(eval(tokens.subList(i + 1, tokens.size()), true))));
                tokens.remove(i + 1);
            }
        }

        return tokens.size() > 0 ? ((NumberToken) tokens.get(0)).val() : 0;
    }

    /**
     * I don't care that intellij is screaming at me, I want to always know in my code, if a value is something or
     * nothing, no, nullable doesn't work, I want to have to explicitly specify what to do if there's no value,
     * because everyone has a habit of ignoring those.
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private static Optional<Token> makeToken(Optional<Class<? extends Token>> clazz, String param) {
        if (clazz.isEmpty()) return Optional.empty();
        return switch (clazz.get().getName()) {
            case "ca.rttv.chatcalc.tokens.NumberToken" -> Optional.of(new NumberToken(Double.parseDouble(param)));
            case "ca.rttv.chatcalc.tokens.OperatorToken" -> Optional.of(new OperatorToken(param.charAt(0)));
            case "ca.rttv.chatcalc.tokens.BracketToken" -> Optional.of(new BracketToken(param.charAt(0)));
            case "ca.rttv.chatcalc.tokens.FunctionToken" -> Optional.of(new FunctionToken(param));
            case "ca.rttv.chatcalc.tokens.AbsToken" -> Optional.of(new AbsToken());
            default -> Optional.empty();
        };
    }

    private static boolean isBracket(char c) {
        return c == 40 || c == 41;
    }

    private static boolean isOperator(char c) {
        return c == 42 || c == 43 || c == 45 || c == 47 || c == 94 || c == 37;
    }

    private static boolean isNumber(char c) {
        return c >= 48 && c <= 57 || c == 46;
    }
}
