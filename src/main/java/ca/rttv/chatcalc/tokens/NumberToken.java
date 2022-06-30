package ca.rttv.chatcalc.tokens;

import net.minecraft.text.LiteralTextContent;
import net.minecraft.text.TextContent;

public class NumberToken implements Token {
    private final double val;

    public NumberToken(double value) {
        val = value;
    }

    public double val() {
        return val;
    }

    @Override
    public String toString() {
        return "\033[0m" + val;
    }

    @Override
    public TextContent getText() {
        return new LiteralTextContent("§f" + val);
    }
}