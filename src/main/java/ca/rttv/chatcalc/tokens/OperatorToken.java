package ca.rttv.chatcalc.tokens;

import net.minecraft.text.LiteralTextContent;
import net.minecraft.text.TextContent;

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
}