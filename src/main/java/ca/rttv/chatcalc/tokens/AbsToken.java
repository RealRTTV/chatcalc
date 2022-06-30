package ca.rttv.chatcalc.tokens;

import net.minecraft.text.LiteralTextContent;
import net.minecraft.text.TextContent;

public class AbsToken implements Token {
    public AbsToken() { }

    public double apply(double value) {
        return Math.abs(value);
    }

    @Override
    public String toString() {
        return "\033[0;35m|";
    }

    @Override
    public TextContent getText() {
        return new LiteralTextContent("§d|");
    }
}
