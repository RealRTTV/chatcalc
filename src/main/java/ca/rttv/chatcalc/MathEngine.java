package ca.rttv.chatcalc;

import java.util.Optional;

public interface MathEngine {
    double eval(String input, Optional<FunctionParameter[]> paramaters);
}
