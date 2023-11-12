package ca.rttv.chatcalc;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Contract;

import java.util.Optional;
import java.util.regex.Pattern;

public class ChatCalc {
    public static final Pattern NUMBER = Pattern.compile("[-+]?\\d+(\\.\\d+)?");
    public static final String SEPARATOR = ";";
    public static final char SEPARATOR_CHAR = ';';

    @Contract(value = "_->_", mutates = "param1")
    public static boolean tryParse(TextFieldWidget field) {
        final MinecraftClient client = MinecraftClient.getInstance();
        String originalText = field.getText();
        int cursor = field.getCursor();
        String text = ChatHelper.getWord(originalText, cursor);
        {
            String[] split = text.split("=");
            if (split.length == 2) {
                if (Config.JSON.has(split[0])) {
                    Config.JSON.addProperty(split[0], split[1]);
                    Config.refreshJson();
                    return ChatHelper.replaceWord(field, "");
                } else {
                    Optional<CallableFunction> func = CallableFunction.fromString(text);
                    if (func.isPresent()) {
                        Config.FUNCTIONS.put(func.get().name(), func.get());
                        Config.refreshJson();
                        return ChatHelper.replaceWord(field, "");
                    }
                    ;
                }
            } else if (split.length == 1) {
                if (Config.JSON.has(split[0])) {
                    return ChatHelper.replaceWord(field, Config.JSON.get(split[0]).getAsString());
                } else if (!split[0].isEmpty() && Config.JSON.has(split[0].substring(0, split[0].length() - 1)) && split[0].endsWith("?") && client.player != null) {
                    client.player.sendMessage(Text.translatable("chatcalc." + split[0].substring(0, split[0].length() - 1) + ".description"));
                    return false;
                }
            }
        }
        
        if ((text.equals("config?") || text.equals("cfg?") || text.equals("?")) && client.player != null) {
            client.player.sendMessage(Text.translatable("chatcalc.config.description"));
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
                double result = Config.makeEngine().eval(text, new FunctionParameter[0]);
                double us = (System.nanoTime() - start) / 1_000.0;
                if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
                    MinecraftClient.getInstance().player.sendMessage(Text.literal("Took " + us + "µs to parse equation"), true);
                    MinecraftClient.getInstance().player.sendMessage(Text.literal("Took " + us + "µs to parse equation"), false);
                }
                String solution = Config.getDecimalFormat().format(result); // so fast that creating a new one everytime doesn't matter, also lets me use fields
                if (solution.equals("-0")) {
                    solution = "0";
                }
                Config.saveToChatHud(originalText);
                Config.saveToClipboard(originalText);
                return add ? ChatHelper.addWordAfterIndex(field, solution) : ChatHelper.replaceWord(field, solution);
            } catch (Throwable t) {
                return false;
            }
        }
    }
}
