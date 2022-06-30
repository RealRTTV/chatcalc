package ca.rttv.chatcalc.tokens;

import net.minecraft.text.LiteralTextContent;
import net.minecraft.text.TextContent;

public final class BracketToken implements Token {
    public final boolean isOpen;

    @Override
    public String toString() {
        return "\033[0;32m" + (isOpen ? '(' : ')');
    }

    public BracketToken(char value) {
        isOpen = value == 40;
    }

    @Override
    public TextContent getText() {
        return new LiteralTextContent("§a" + (isOpen ? '(' : ')'));
    }
}