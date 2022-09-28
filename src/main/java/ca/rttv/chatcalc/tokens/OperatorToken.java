package ca.rttv.chatcalc.tokens;

import ca.rttv.chatcalc.MathEngine;
import net.minecraft.text.LiteralTextContent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Pair;
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
    public int eval(List<Token> tokens, int i, Optional<Pair<String, Double>> variable) {
        if (tokens.get(i - 1) instanceof BracketToken bracketToken) {
            tokens.remove(--i);
            int start = bracketToken.getStart(tokens, i);
            MathEngine.simplify(tokens.subList(start, i), false, variable);
        }
        double left = tokens.get(i - 1) instanceof NumberToken numberToken ? numberToken.val : Double.NaN;
        if (tokens.get(i + 1) instanceof BracketToken) {
            tokens.remove(--i);
            MathEngine.simplify(tokens.subList(i, tokens.size()), false, variable);
        }
        double right = tokens.get(i + 1) instanceof NumberToken numberToken ? numberToken.val : Double.NaN;
        tokens.set(i, new NumberToken(apply(left, right)));
        tokens.remove(--i);
        tokens.remove(i + 1);
        i--;
        return i;
    }
}