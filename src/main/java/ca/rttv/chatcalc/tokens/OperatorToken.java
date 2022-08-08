package ca.rttv.chatcalc.tokens;

import ca.rttv.chatcalc.MathEngine;
import net.minecraft.text.LiteralTextContent;
import net.minecraft.text.TextContent;

import java.util.List;

public final class OperatorToken implements Token {
    public final char val;

    public OperatorToken(char value) {
        val = value;
    }

    public double apply(double prev, double next) {
        return switch (val) {
            case 42 -> prev * next;
            case 43 -> prev + next;
            case 45 -> prev - next;
            case 47 -> prev / next;
            case 94 -> Math.pow(prev, next);
            case 37 -> prev % next;
            default -> throw new IllegalArgumentException("Invalid operator token: " + val);
        };
    }

    @Override
    public String toString() {
        return "\033[0;31m" + val;
    }

    @Override
    public TextContent getText() {
        return new LiteralTextContent("§c" + val);
    }

    public int eval(List<Token> tokens, int i) {
        if (tokens.get(i - 1) instanceof BracketToken bracketToken) {
            tokens.remove(--i);
            int start = bracketToken.getStart(tokens, i);
            MathEngine.eval(tokens.subList(start, i), false);
        }
        double left = tokens.get(i - 1) instanceof NumberToken numberToken ? numberToken.val : Double.NaN;
        if (tokens.get(i + 1) instanceof BracketToken) {
            tokens.remove(--i);
            MathEngine.eval(tokens.subList(i, tokens.size()), false);
        }
        double right = tokens.get(i + 1) instanceof NumberToken numberToken ? numberToken.val : Double.NaN;
        tokens.set(i, new NumberToken(apply(left, right)));
        tokens.remove(--i);
        tokens.remove(i + 1);
        i--;
        return i;
    }
}