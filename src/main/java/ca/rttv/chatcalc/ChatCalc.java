package ca.rttv.chatcalc;

import com.mojang.datafixers.util.Either;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import com.mojang.datafixers.util.Pair;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Optional;
import java.util.regex.Pattern;

public class ChatCalc {
    public static final HashSet<String> CONSTANT_TABLE = new HashSet<>();
    public static final HashSet<Pair<String, Integer>> FUNCTION_TABLE = new HashSet<>();
    public static final Pattern NUMBER = Pattern.compile("[-+]?(\\d,?)*(\\.\\d+)?");
    public static final Pattern FUNCTION = Pattern.compile("[a-zA-Z]+\\(([a-zA-Z]+;)*?([a-zA-Z]+)\\)");
    public static final Pattern CONSTANT = Pattern.compile("[a-zA-Z]+");
    public static final String SEPARATOR = ";";
    public static final char SEPARATOR_CHAR = ';';

    @Contract(value = "_->_", mutates = "param1")
    public static boolean tryParse(@NotNull TextFieldWidget field) {
        final MinecraftClient client = MinecraftClient.getInstance();
        String originalText = field.getText();
        int cursor = field.getCursor();
        String text = ChatHelper.getSection(originalText, cursor);
        {
            String[] split = text.split("=");
            if (split.length == 2) {
                if (Config.JSON.has(split[0])) {
                    Config.JSON.addProperty(split[0], split[1]);
                    Config.refreshJson();
                    return ChatHelper.replaceSection(field, "");
                } else {
                    Optional<Either<CustomFunction, CustomConstant>> either = parseDeclaration(text);
                    if (either.isPresent()) {
                        Optional<CustomFunction> left = either.get().left();
                        Optional<CustomConstant> right = either.get().right();
                        if (left.isPresent()) {
                            Config.FUNCTIONS.put(new Pair<>(left.get().name(), left.get().params().length), left.get());
                            Config.refreshJson();
                            return ChatHelper.replaceSection(field, "");
                        } else if (right.isPresent()) {
                            Config.CONSTANTS.put(right.get().name(), right.get());
                            Config.refreshJson();
                            return ChatHelper.replaceSection(field, "");
                        }
                    }
                }
            } else if (split.length == 1) {
                if (Config.JSON.has(split[0])) {
                    return ChatHelper.replaceSection(field, Config.JSON.get(split[0]).getAsString());
                } else if (!split[0].isEmpty() && Config.JSON.has(split[0].substring(0, split[0].length() - 1)) && split[0].endsWith("?") && client.player != null) {
                    client.player.sendMessage(Text.translatable("chatcalc." + split[0].substring(0, split[0].length() - 1) + ".description"));
                    return false;
                } else {
                    Optional<Either<CustomFunction, CustomConstant>> either = parseDeclaration(text);
                    if (either.isPresent()) {
                        Optional<CustomFunction> left = either.get().left();
                        Optional<CustomConstant> right = either.get().right();
                        if (left.isPresent()) {
                            Pair<String, Integer> pair = new Pair<>(left.get().name(), left.get().params().length);
                            if (Config.FUNCTIONS.containsKey(pair)) {
                                Config.FUNCTIONS.remove(pair);
                                Config.refreshJson();
                                return ChatHelper.replaceSection(field, "");
                            }
                        } else if (right.isPresent()) {
                            if (Config.CONSTANTS.containsKey(right.get().name())) {
                                Config.CONSTANTS.remove(right.get().name());
                                Config.refreshJson();
                                return ChatHelper.replaceSection(field, "");
                            }
                        }
                    }
                }
            }
        }
        
        if ((text.equals("config?") || text.equals("cfg?") || text.equals("?")) && client.player != null) {
            client.player.sendMessage(Text.translatable("chatcalc.config.description"));
            return false;
        } else if (text.equals("testcases?")) {
            Testcases.test(Testcases.TESTCASES);
            return false;
        } else if (text.equals("functions?")) {
            client.player.sendMessage(Config.FUNCTIONS.values().stream().map(CustomFunction::toString).map(str -> Text.literal(str).styled(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, str)).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Click to copy to clipboard"))))).collect(() -> Text.literal("Currently defined custom functions are:"), (a, b) -> a.append(Text.literal("\n").append(b)), MutableText::append));
            return false;
        } else if (text.equals("constants?")) {
            client.player.sendMessage(Config.CONSTANTS.values().stream().map(CustomConstant::toString).map(str -> Text.literal(str).styled(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, str)).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Click to copy to clipboard"))))).collect(() -> Text.literal("Currently defined custom constants are:"), (a, b) -> a.append(Text.literal("\n").append(b)), MutableText::append));
            return false;
        } else if (NUMBER.matcher(text).matches()) {
            return false;
        } else {
            boolean add = false;
            if (text.endsWith("=")) {
                text = text.substring(0, text.length() - 1);
                add = true;
            }
            try {
                long start = System.nanoTime();
                CONSTANT_TABLE.clear();
                FUNCTION_TABLE.clear();
                double result = Config.makeEngine().eval(text, new FunctionParameter[0]);
                double micros = (System.nanoTime() - start) / 1_000.0;
                if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
                    MinecraftClient.getInstance().player.sendMessage(Text.literal("Took " + micros + "µs to parse equation"), true);
                    MinecraftClient.getInstance().player.sendMessage(Text.literal("Took " + micros + "µs to parse equation"), false);
                }
                String solution = Config.getDecimalFormat().format(result); // so fast that creating a new one everytime doesn't matter, also lets me use fields
                if (solution.equals("-0")) {
                    solution = "0";
                }
                Config.saveToChatHud(originalText);
                Config.saveToClipboard(originalText);
                return add ? ChatHelper.addSectionAfterIndex(field, solution) : ChatHelper.replaceSection(field, solution);
            } catch (Throwable t) {
                return false;
            }
        }
    }

    private static Optional<Either<CustomFunction, CustomConstant>> parseDeclaration(String text) {
        return CustomFunction.fromString(text).map(Either::<CustomFunction, CustomConstant>left).or(() -> CustomConstant.fromString(text).map(Either::<CustomFunction, CustomConstant>right));
    }
}
