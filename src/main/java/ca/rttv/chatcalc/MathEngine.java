package ca.rttv.chatcalc;

import ca.rttv.chatcalc.tokens.*;
import com.mojang.logging.LogUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.LiteralTextContent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Pair;
import org.jetbrains.annotations.Contract;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class MathEngine {
    public static final Logger LOGGER = LogUtils.getLogger();

    @Contract(value = "!null->_", pure = true)
    public static double eval(String input) {
        MinecraftClient client = MinecraftClient.getInstance();
        List<Token> tokens = tokenize(input);
        {
            if (Config.debugTokens() && client.player != null) {
                LOGGER.info(tokens.stream().map(Object::toString).collect(Collectors.joining()));

                MutableText text = MutableText.of(new LiteralTextContent("§r"));
                text.getSiblings().addAll(tokens.stream().map(Token::toText).toList());
                text.getSiblings().add(Text.literal("§r"));
                client.player.sendMessage(text);
            }
        } // print to console for debug

        simplify(tokens, false, Optional.empty());
        if (tokens.size() > 0 && tokens.get(0) instanceof NumberToken numberToken) {
            return numberToken.val;
        }
        throw new IndexOutOfBoundsException();
    }
    
    @Contract(value = "!null->!null", pure = true)
    public static List<Token> tokenize(String input) {
        input = input.toLowerCase()
                .replace("pi", "3.141592653589793")
                .replace("tau", "6.283185307179586")
                .replace("**", "^")
                .replaceAll(",", "")
                .replaceAll("", "2.718281828459045");
        List<Token> tokens = new ArrayList<>(input.length() >> 1); // just a guess
        {
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

                if (sb.length() > 0 && !currentType.equals(Optional.of(FunctionToken.class))) {
                    makeToken(currentType, sb.toString()).ifPresent(tokens::add);
                    sb = new StringBuilder();
                }
                sb.append(c); // if something is not a member of another type, it just assumes it's an unknown function
                currentType = Optional.of(FunctionToken.class);
            }

            if (sb.length() > 0) { // this segment on every one of these is to finish a number token
                makeToken(currentType, sb.toString()).ifPresent(tokens::add);
            }
        } // parse to tokens
        {
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
        } // convert - + signs on numbers when appropriate
        return tokens;
    }

    @Contract(value = "_,_,_->_", mutates = "param1")
    public static void simplify(List<Token> tokens, boolean abs, Optional<Pair<String, Double>> variable) {
        final MinecraftClient client = MinecraftClient.getInstance();
        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if (token instanceof BracketToken bracketToken) {
                //noinspection SuspiciousListRemoveInLoop -- checked
                tokens.remove(i);
                if (bracketToken.isOpen) {
                    simplify(tokens.subList(i, tokens.size()), false, variable);
                } else {
                    simplify(tokens.subList(0, i), false, variable);
                    return;
                }
            } else if (token instanceof AbsToken absToken) { // absolute value
                if (abs) {
                    Token first = tokens.get(0);
                    if (first instanceof NumberToken numberToken) {
                        tokens.set(i, new NumberToken(absToken.apply(numberToken.val)));
                        return;
                    }
                    return;
                } else {
                    simplify(tokens.subList(i, tokens.size()), true, variable);
                }
            } else if (token instanceof FunctionToken functionToken) { // function
                if (variable.isPresent() && variable.get().getLeft().equals(functionToken.func)) { // since 'a' will be considered a function, I have to detect if it is, then convert it into a constant
                    tokens.set(i, new NumberToken(variable.get().getRight()));
                } else if (functionToken.func.length() == 1 && isPos(functionToken.func.charAt(0)) && client.player != null) {
                    tokens.set(i, new NumberToken(switch (functionToken.func.charAt(0)) {
                        case 'x' -> client.player.getX();
                        case 'y' -> client.player.getY();
                        case 'z' -> client.player.getZ();
                        default -> throw new NullPointerException();
                    }));
                } else {
                    //noinspection SuspiciousListRemoveInLoop -- checked
                    tokens.remove(i); // thyself
                    //noinspection SuspiciousListRemoveInLoop -- checked
                    tokens.remove(i); // opening bracket
                    simplify(tokens.subList(i, tokens.size()), false, variable);
                    Token param = tokens.get(i);
                    double value = param instanceof NumberToken numberToken ? numberToken.val : Double.NaN;
                    double output = functionToken.apply(value);
                    tokens.set(i, new NumberToken(output));
                }
            }
        } // brackets

        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if (token instanceof OperatorToken operatorToken && operatorToken.val == 94) {
                i = operatorToken.eval(tokens, i, variable);
            }
        } // exponent

        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if (token instanceof OperatorToken operatorToken && (operatorToken.val == 47 || operatorToken.val == 42)) {
                i = operatorToken.eval(tokens, i, variable);
                continue;
            }
            if (i < tokens.size() - 1 && token instanceof NumberToken first && tokens.get(i + 1) instanceof NumberToken second) { // (4)5 multiplication style, should be done next to the normal multiplication
                tokens.remove(i);
                tokens.set(i, new NumberToken(first.val * second.val));
            }
        } // division & multiplication

        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if (token instanceof OperatorToken operatorToken && operatorToken.val == 37) {
                i = operatorToken.eval(tokens, i, variable);
            }
        } // modulo

        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if (token instanceof OperatorToken operatorToken && (operatorToken.val == 43 || operatorToken.val == 45)) {
                i = operatorToken.eval(tokens, i, variable);
            }
        } // addition & subtraction
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
    @Contract(value = "_,!null->!null", pure = true)
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private static Optional<Token> makeToken(Optional<Class<? extends Token>> clazz, String param) {
        return clazz.map(value -> switch (value.getName()) {
            case "ca.rttv.chatcalc.tokens.NumberToken" -> new NumberToken(Double.parseDouble(param));
            case "ca.rttv.chatcalc.tokens.OperatorToken" -> new OperatorToken(param.charAt(0));
            case "ca.rttv.chatcalc.tokens.BracketToken" -> new BracketToken(param.charAt(0));
            case "ca.rttv.chatcalc.tokens.FunctionToken" -> new FunctionToken(param);
            case "ca.rttv.chatcalc.tokens.AbsToken" -> new AbsToken();
            default -> null;
        });
    }

    @Contract(value = "_->_", pure = true)
    private static boolean isAbs(char c) {
        return c == 124;
    }

    @Contract(value = "_->_", pure = true)
    private static boolean isBracket(char c) {
        return c == 40 || c == 41;
    }

    @Contract(value = "_->_", pure = true)
    private static boolean isOperator(char c) {
        return c == 42 || c == 43 || c == 45 || c == 47 || c == 94 || c == 37;
    }

    @Contract(value = "_->_", pure = true)
    private static boolean isNumber(char c) {
        return c >= 48 && c <= 57 || c == 46;
    }

    @Contract(value = "_->_", pure = true)
    private static boolean isPos(char c) {
        return c >= 120 && c <= 122;
    }
}
