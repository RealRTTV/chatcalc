package ca.rttv.chatcalc.tokens;

public class AbsToken implements Token {
    public AbsToken() {}

    public double apply(double value) {
        return Math.abs(value);
    }
}
