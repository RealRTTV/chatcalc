package ca.rttv.chatcalc.tokens;

public final class NumberToken implements Token {
    public final double value;

    public NumberToken(double value) {
        this.value = value;
    }
}