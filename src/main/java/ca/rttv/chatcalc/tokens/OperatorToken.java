package ca.rttv.chatcalc.tokens;

import ca.rttv.chatcalc.FunctionParameter;
import ca.rttv.chatcalc.TokenizedMathEngine;
import net.minecraft.text.LiteralTextContent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Contract;

import java.util.List;
import java.util.Optional;

public final class OperatorToken implements Token {
    public final char val;

    public OperatorToken(char value) {
        val = value;
    }

    public double apply(double prev, double next) {
        return switch (val) {
            case 42 -> prev * next;
            case 43 -> prev + next;
            case 45 -> prev - next;
            case 47 -> prev / next;
            case 94 -> Math.pow(prev, next);
            case 37 -> prev % next;
            default -> throw new IllegalArgumentException("Invalid operator token: " + val);
        };
    }

    @Override
    public String toString() {
        return String.valueOf(val);
    }

    @Override
    public Text toText() {
        return MutableText.of(new LiteralTextContent("§c" + val));
    }

    @Contract(value = "_,_,_->_", mutates = "param1")
    public int eval(List<Token> tokens, int i, Optional<FunctionParameter[]> params) {
        double left;
        if (i == 0) {
            left = 0;
        } else {
            if (tokens.get(i - 1) instanceof BracketToken bracketToken) {
                tokens.remove(--i);
                int start = bracketToken.getStart(tokens, i);
                TokenizedMathEngine.simplify(tokens.subList(start, i), false, params); // safe because the only engine to use tokens is the token engin
            }

            if (tokens.get(i - 1) instanceof NumberToken numberToken) {
                tokens.remove(--i);
                left = numberToken.val;
            } else {
                left = 0;
            }
        }
        if (tokens.get(i + 1) instanceof BracketToken) {
            tokens.remove(--i);
            TokenizedMathEngine.simplify(tokens.subList(i, tokens.size()), false, params); // safe because the only engine to use tokens is the token engin
        }
        if (tokens.get(i + 1) instanceof OperatorToken op && (op.val == '+' || op.val == '-')) {
            tokens.remove(i + 1);
            TokenizedMathEngine.simplify(tokens.subList(i + 1, tokens.size()), false, params); // safe because the only engine to use tokens is the token engin
            if (tokens.get(i + 1) instanceof NumberToken right && op.val == '-') {
                tokens.set(i + 1, new NumberToken(-right.val));
            }
        }
        if (!(tokens.get(i + 1) instanceof NumberToken right)) {
            throw new IllegalArgumentException();
        }
        tokens.set(i, new NumberToken(apply(left, right.val)));
        tokens.remove(i + 1);
        return i;
    }
}