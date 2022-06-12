package ca.rttv.chatcalc.tokens;

public final class BracketToken implements Token {
    public final boolean isOpen;

    public BracketToken(char value) {
        this.isOpen = value == 40;
    }
}