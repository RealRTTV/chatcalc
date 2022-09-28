package ca.rttv.chatcalc.tokens;

import net.minecraft.text.LiteralTextContent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

public final class AbsToken implements Token {
    public AbsToken() { }

    public double apply(double value) {
        return Math.abs(value);
    }

    @Override
    public String toString() {
        return "|";
    }

    @Override
    public Text toText() {
        return MutableText.of(new LiteralTextContent("§d|"));
    }
}
