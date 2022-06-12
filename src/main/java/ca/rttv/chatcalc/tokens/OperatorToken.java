package ca.rttv.chatcalc.tokens;

public final class OperatorToken implements Token {
    public final char value;

    public OperatorToken(char value) {
        this.value = value;
    }

    public double apply(double prev, double next) {
        return switch (value) {
            case 42 -> prev * next;
            case 43 -> prev + next;
            case 45 -> prev - next;
            case 47 -> prev / next;
            case 94 -> Math.pow(prev, next);
            case 37 -> prev % next;
            default -> 0;
        };
    }
}