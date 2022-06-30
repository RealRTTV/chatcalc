package ca.rttv.chatcalc;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.DecimalFormat;
import java.util.stream.Collectors;

public class EventHandler {
    public static final Logger LOGGER;
    public static final JsonObject json;
    public static final Gson GSON;
    public static final File configFile;

    static {
        configFile = new File(".", "config/chatcalc.json");
        GSON = new GsonBuilder().setPrettyPrinting().create();
        json = new JsonObject();
        LOGGER = LogUtils.getLogger();
        File dir = new File(".", "config");
        if ((dir.exists() && dir.isDirectory() || dir.mkdirs()) && !configFile.exists()) {
            json.addProperty("decimal_format", "#,##0.##");
            json.addProperty("radians", "false");
            json.addProperty("debug_tokens", "false");
            EventHandler.refreshJson();
        }
        if (configFile.exists() && configFile.isFile() && configFile.canRead()) {
            EventHandler.readJson();
        }
    }

    private static DecimalFormat getDecimalFormat() {
        return new DecimalFormat(json.get("decimal_format").getAsString());
    }

    public static double convertIfRadians(double value) {
        return Boolean.parseBoolean(json.get("radians").getAsString()) ? Math.toRadians(value) : value;
    }

    public static boolean runExpression(TextFieldWidget field) {
        final MinecraftClient client = MinecraftClient.getInstance();
        String originalText = field.getText();
        String text = originalText.substring(0, field.getCursor());
        String[] split = text.split("=");
        if (split.length == 2) {
            if (json.has(split[0])) {
                json.addProperty(split[0], split[1]);
                EventHandler.refreshJson();
                return ChatHelper.replaceWord(field, "");
            }
        } else if (json.has(split[0])) {
            return ChatHelper.replaceWord(field, json.get(split[0]).getAsString());
        } else if (split[0].length() > 0 && json.has(split[0].substring(0, split[0].length() - 1)) && split[0].endsWith("?") && client.player != null) {
            client.player.sendMessage(Text.translatable("chatcalc." + split[0].substring(0, split[0].length() - 1) + ".description"));
            return false;
        }

        try {
            Double.parseDouble(ChatHelper.getWord(field.getText(), field.getCursor()));
            return false;
        } catch (NumberFormatException e) {
            return EventHandler.runExprReplace(field) || EventHandler.runExprAdd(field);
        }
    }

    private static boolean runExprReplace(TextFieldWidget field) {
        String originalText = field.getText();
        int cursor = field.getCursor();
        try {
            String word = ChatHelper.getWord(originalText, cursor);
            if (word.endsWith("=") || word.length() == 0) {
                return false;
            }
            double solution = MathEngine.eval(word);
            String solStr = EventHandler.getDecimalFormat().format(solution);
            return ChatHelper.replaceWord(field, solStr);
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean runExprAdd(TextFieldWidget field) {
        String originalText = field.getText();
        int cursor = field.getCursor();
        try {
            String word = ChatHelper.getWord(originalText, cursor);
            if (!word.endsWith("=")) {
                return false;
            }
            word = word.substring(0, word.length() - 1);
            double solution = MathEngine.eval(word);
            String solStr = EventHandler.getDecimalFormat().format(solution);
            return ChatHelper.addWordAfterIndex(field, ChatHelper.getEndOfWord(originalText, cursor), solStr);
        } catch (Exception e) {
            return false;
        }
    }

    public static void refreshJson() {
        try {
            FileWriter writer = new FileWriter(configFile);
            writer.write(GSON.toJson(json));
            writer.close();
        } catch (Exception ignored) {
        }
    }

    public static void readJson() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(configFile));
            json.entrySet().forEach(entry -> json.remove(entry.getKey()));
            JsonParser.parseString(reader.lines().collect(Collectors.joining("\n"))).getAsJsonObject().entrySet().forEach(entry -> json.add(entry.getKey(), entry.getValue()));
            reader.close();
        } catch (Exception ignored) {
        }
    }

    public static boolean debugTokens() {
        return Boolean.parseBoolean(json.get("debug_tokens").getAsString());
    }
}
