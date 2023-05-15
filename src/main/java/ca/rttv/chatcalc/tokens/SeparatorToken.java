package ca.rttv.chatcalc.tokens;

import ca.rttv.chatcalc.ChatCalc;
import net.minecraft.text.LiteralTextContent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

public final class SeparatorToken implements Token {
    @Override
    public String toString() {
        return ChatCalc.SEPARATOR;
    }

    @Override
    public Text toText() {
        return MutableText.of(new LiteralTextContent("§6" + ChatCalc.SEPARATOR));
    }
}
