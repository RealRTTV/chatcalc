package ca.rttv.chatcalc.tokens;

import net.minecraft.text.LiteralTextContent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.List;

public final class BracketToken implements Token {
    public final boolean isOpen;

    @Override
    public String toString() {
        return isOpen ? "(" : ")";
    }

    public BracketToken(char value) {
        isOpen = value == 40;
    }

    @Override
    public Text toText() {
        return MutableText.of(new LiteralTextContent("§a" + (isOpen ? '(' : ')')));
    }

    public int getStart(List<Token> tokens, int index) {
        if (isOpen) {
            throw new RuntimeException("Expected closing bracket before operator, not an open one");
        }

        int depth = 0;

        for (int i = index - 1; i >= 0; i--) {
            Token token = tokens.get(i);
            if (token instanceof BracketToken bracketToken) {
                if (bracketToken.isOpen && depth-- == 0) {
                    return i;
                } else if (!bracketToken.isOpen) {
                    depth++;
                }
            }
        }

        throw new RuntimeException("Could not find opening bracket to closing bracket @ Token index " + index);
    }
}