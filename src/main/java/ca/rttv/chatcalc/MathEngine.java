package ca.rttv.chatcalc;

import ca.rttv.chatcalc.tokens.*;
import com.mojang.logging.LogUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.LiteralTextContent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class MathEngine {
    public static final Logger LOGGER = LogUtils.getLogger();

    public static double eval(String input) {
        MinecraftClient client = MinecraftClient.getInstance();
        input = input.toLowerCase()
                     .replace("pi", "3.141592653589793")
                     .replace("**", "^")
                     .replaceAll(",(?! )", "");
        if (EventHandler.euler()) {
            input = input.replaceAll("(?!c)e(?!il)", "2.718281828459045");
        }
        if (client.player != null) {
            input = input.replace("x", String.valueOf(client.player.getX()))
                         .replace("y", String.valueOf(client.player.getY()))
                         .replace("z", String.valueOf(client.player.getZ()));
        }
        List<Token> tokens = new ArrayList<>(input.length()); // array of custom objects which do different things each
        Optional<Class<? extends Token>> currentType = Optional.empty();
        StringBuilder sb = new StringBuilder();

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

        boolean isOpeningAbsToken = true; // inverts between for abs tokens, represents if its opening or closing
        for (int i = 0; i < tokens.size() - 1; i++) {
            Token token = tokens.get(i);
            Token number = tokens.get(i + 1);
            if (token instanceof OperatorToken operatorToken && (operatorToken.val == 43 || operatorToken.val == 45) && number instanceof NumberToken numberToken) {
                double signed = operatorToken.val == 45 ? -numberToken.val : numberToken.val;
                if (i == 0) { // if im an operator, and the next is a number, of course we should modify that
                    tokens.set(i + 1, new NumberToken(signed));
                    tokens.remove(i); // yes, we should skip the next one
                    continue;
                }

                Token previous = tokens.get(i - 1);
                if (previous instanceof OperatorToken) {
                    tokens.set(i + 1, new NumberToken(signed));
                    //noinspection SuspiciousListRemoveInLoop -- checked, yes, we should skip the next one
                    tokens.remove(i);
                } else if (previous instanceof BracketToken bracketToken && bracketToken.isOpen) {
                    tokens.set(i + 1, new NumberToken(signed));
                    //noinspection SuspiciousListRemoveInLoop -- checked, yes, we should skip the next one
                    tokens.remove(i);
                } else if (previous instanceof AbsToken) {
                    if (isOpeningAbsToken) {
                        tokens.set(i + 1, new NumberToken(signed));
                        //noinspection SuspiciousListRemoveInLoop -- checked, yes, we should skip the next one
                        tokens.remove(i);
                    }
                    isOpeningAbsToken = !isOpeningAbsToken;
                }
            }
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

    public static double eval(List<Token> tokens, boolean abs) {
        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if (token instanceof BracketToken bracketToken) { // brackets
                //noinspection SuspiciousListRemoveInLoop -- checked
                tokens.remove(i);
                if (bracketToken.isOpen) {
                    eval(tokens.subList(i, tokens.size()), false);
                } else {
                    eval(tokens.subList(0, i), false);
                    return Double.NaN;
                }
            } else if (token instanceof AbsToken absToken) { // absolute value
                if (abs) {
                    Token first = tokens.get(0);
                    if (first instanceof NumberToken numberToken) {
                        tokens.set(i, new NumberToken(absToken.apply(numberToken.val)));
                        return numberToken.val;
                    }
                    return Double.NaN;
                } else {
                    eval(tokens.subList(i, tokens.size()), true);
                }
            } else if (token instanceof FunctionToken functionToken) { // function
                //noinspection SuspiciousListRemoveInLoop -- checked
                tokens.remove(i); // thyself
                //noinspection SuspiciousListRemoveInLoop -- checked
                tokens.remove(i); // opening bracket
                eval(tokens.subList(i, tokens.size()), false);
                Token param = tokens.get(i);
                double value = param instanceof NumberToken numberToken ? numberToken.val : Double.NaN;
                double output = functionToken.apply(value);
                tokens.set(i, new NumberToken(output));
            }
        }

        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if (token instanceof OperatorToken operatorToken && operatorToken.val == 94) { // exponent
                i = operatorToken.eval(tokens, i);
            }
        }

        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if (token instanceof OperatorToken operatorToken && (operatorToken.val == 47 || operatorToken.val == 42)) { // division & multiplication
                i = operatorToken.eval(tokens, i);
            }
        }

        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if (token instanceof OperatorToken operatorToken && operatorToken.val == 37) { // modulo
                i = operatorToken.eval(tokens, i);
            }
        }

        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if (token instanceof OperatorToken operatorToken && (operatorToken.val == 43 || operatorToken.val == 45)) { // addition & subtraction
                i = operatorToken.eval(tokens, i);
            }
        }

        for (int i = 0; i < tokens.size() - 1; i++) {
            Token token = tokens.get(i);
            Token next = tokens.get(i + 1);
            if (token instanceof NumberToken first && next instanceof NumberToken second) { // (4)5 multiplication style, should be done last because its probably safest
                //noinspection SuspiciousListRemoveInLoop -- checked
                tokens.remove(i);
                tokens.set(i, new NumberToken(first.val * second.val));
            }
        }

        return tokens.size() > 0 && tokens.get(0) instanceof NumberToken numberToken ? numberToken.val : Double.NaN;
    }

    /**
     *     I don't care that intellij is screaming at me, I want to always know in my code, if a value is something or
     *     nothing, no, nullable doesn't work, I want to have to explicitly specify what to do if there's no value,
     *     because everyone has a habit of ignoring those.
     * <p>
     *     thank you for listening to my ted-talk
     * <p>
     *     this post was made by a rust user
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

    private static boolean isAbs(char c) {
        return c == 124;
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
