package ca.rttv.chatcalc;

import java.util.Optional;

public record CustomConstant(String name, String eval) {

    public static Optional<CustomConstant> fromString(String text) {
        int equalsIdx = text.indexOf('=');
        if (equalsIdx == -1) {
            return Optional.empty();
        }

        String lhs = text.substring(0, equalsIdx);
        String rhs = text.substring(equalsIdx + 1);

        if (!ChatCalc.CONSTANT.matcher(lhs).matches()) {
            return Optional.empty();
        }

        return Optional.of(new CustomConstant(lhs, rhs));
    }

    public double value() {
        return Config.makeEngine().eval(eval, new FunctionParameter[0]);
    }

    @Override
    public String toString() {
        return name + "=" + eval;
    }
}
