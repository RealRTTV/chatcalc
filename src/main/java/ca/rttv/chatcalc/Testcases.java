package ca.rttv.chatcalc;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Pair;
import net.minecraft.util.math.MathHelper;

import java.util.List;

public interface Testcases {
    List<Pair<String, Double>> TESTCASES = List.<Pair<String, Double>>of(
            new Pair<>("3+3", 6.0d),
            new Pair<>("4*4", 16.0d),
            new Pair<>("5(6)", 30.0d),
            new Pair<>("(6)5", 30.0d),
            new Pair<>("4^2", 16.0d),
            new Pair<>("sqrt(16)", 4.0d),
            new Pair<>("4*(4+3*3)", 52.0d),
            new Pair<>("8*2", 16.0d),
            new Pair<>("26+cos(0", 27.0d),
            new Pair<>("1+2)/3", 1.0d),
            new Pair<>("1+(2*3", 7.0d),
            new Pair<>("pie", Math.PI * Math.E),
            new Pair<>("(2phi-1)^2", 5.0d),
            new Pair<>("1+1", 2.0d),
            new Pair<>("1+2*3", 7.0d),
            new Pair<>("1+2*4/2", 5.0d),
            new Pair<>("1+12/3(2)", 3.0d),
            new Pair<>("1+(2*3)^2", 37.0d),
            new Pair<>("0.5(-2.5-0.1)", -1.3d),
            new Pair<>("sqrt(9)", 3.0d),
            new Pair<>("cbrt(9^3)", 9.0d),
            new Pair<>("ln(e^2)", 2.0d),
            new Pair<>("ln(exp(pi))", Math.PI),
            new Pair<>("log_10(1000)", 3.0d),
            new Pair<>("log(1000)", 3.0d),
            new Pair<>("2^3*2", 16.0d),
            new Pair<>("sin(90deg)", 1.0d),
            new Pair<>("cos(180deg)", -1.0d),
            new Pair<>("tan(45deg)", 1.0d),
            new Pair<>("cot(-45deg)", -1.0d),
            new Pair<>("sec(180deg)", -1.0d),
            new Pair<>("csc(90deg)", 1.0d),
            new Pair<>("arcsin(sin(90deg))", 90.0d),
            new Pair<>("arccos(cos(180deg))", 180.0d),
            new Pair<>("arctan(tan(45deg))", 45.0d),
            new Pair<>("arccot(cot(45deg))", 45.0d),
            new Pair<>("arcsec(sec(89deg))", 89.0d),
            new Pair<>("arccsc(csc(91deg))", 89.0d),
            new Pair<>("floor(-2.5)", -3.0d),
            new Pair<>("ceil(-2.5)", -2.0d),
            new Pair<>("round(-2.5-0.1)", -3.0d),
            new Pair<>("abs(-2.5-0.1)", 2.6d),
            new Pair<>("|-2.5-0.1|", 2.6d),
            new Pair<>("0.5|-2.5-0.1|", 1.3d),
            new Pair<>("5%360", 5.0d),
            new Pair<>("-5%360", 355.0d),
            // add the two remaining signed ones
            new Pair<>("min(sqrt(37);6", 6.0d),
            new Pair<>("max(sqrt(37);7", 7.0d),
            new Pair<>("max(sqrt(2);sqrt(3);sqrt(5);sqrt(7);sqrt(11);sqrt(13);sqrt(17);sqrt(19);sqrt(23);sqrt(29);sqrt(31);sqrt(37);sqrt(41", (double) MathHelper.sqrt(41)),
            new Pair<>("clamp(-e;-2;4)", -2.0d),
            new Pair<>("clamp(pi^2;-2;4", 4.0d),
            new Pair<>("clamp(pi;-2;4)", Math.PI),
            new Pair<>("cmp(-2;3)", -1.0d),
            new Pair<>("cmp(5;3)", 1.0d),
            new Pair<>("cmp(5;3;5)", 0.0d)
    );

    static void test(List<Pair<String, Double>> list) {
        final MinecraftClient client = MinecraftClient.getInstance();

        for (Pair<String, Double> entry : list) {
            try {
                double result = Config.makeEngine().eval(entry.getLeft(), new FunctionParameter[0]);
                if (Math.abs(entry.getRight() - result) <= 0.000001) {
                    client.player.sendMessage(Text.literal("§aTest case passed: " + entry.getLeft() + ", got " + entry.getRight()));
                } else {
                    client.player.sendMessage(Text.literal("§cTest case §n§cfailed: " + entry.getLeft() + ", expected " + entry.getRight() + ", got " + result));
                }
            } catch (Exception e) {
                client.player.sendMessage(Text.literal("§aTest case failed with exception: " + entry.getLeft() + ", expected " + entry.getRight() + ", got " + e));
            }
        }
    }
}
