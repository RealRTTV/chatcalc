package ca.rttv.chatcalc;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.gui.widget.TextFieldWidget;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.DecimalFormat;
import java.util.stream.Collectors;

public class EventHandler {
    public static JsonObject json;
    public static File configFile;
    public static Gson GSON;

    private static DecimalFormat getDecimalFormat() {
        return new DecimalFormat(json.get("decimalFormat").getAsString());
    }

    public static boolean runExpression(TextFieldWidget field) {
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
        }

        return EventHandler.runExprReplace(field) || EventHandler.runExprAdd(field);
    }

    private static boolean runExprReplace(TextFieldWidget field) {
        String originalText = field.getText();
        int cursor = field.getCursor();
        try {
            String word = ChatHelper.getWord(originalText, cursor);
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
        } catch (Exception ignored) {}
    }

    static {
        configFile = new File(".", "config/chatcalc.json");
        GSON = new GsonBuilder().setPrettyPrinting().create();
        File dir = new File(".", "config");
        if ((dir.exists() && dir.isDirectory() || dir.mkdirs()) && !configFile.exists()) {
            JsonObject json = new JsonObject();
            json.addProperty("decimalFormat", "#,##0.##");
            EventHandler.json = json;
            EventHandler.refreshJson();
        }
        if (configFile.exists() && configFile.isFile() && configFile.canRead()) {
            EventHandler.readJson();
        }
    }

    public static void readJson() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(configFile));
            EventHandler.json = JsonParser.parseString(reader.lines().collect(Collectors.joining("\n"))).getAsJsonObject();
            reader.close();
        } catch (Exception ignored) {}
    }
}
