package ca.rttv.chatcalc.tokens;

import net.minecraft.text.LiteralTextContent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

public final class NumberToken implements Token {
    public final double val;

    public NumberToken(double value) {
        val = value;
    }

    @Override
    public String toString() {
        return String.valueOf(val);
    }

    @Override
    public Text toText() {
        return MutableText.of(new LiteralTextContent("§f" + val));
    }
}