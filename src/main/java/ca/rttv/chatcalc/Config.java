package ca.rttv.chatcalc;

import ca.rttv.chatcalc.tokens.NumberToken;
import ca.rttv.chatcalc.tokens.Token;
import com.google.gson.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Pair;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

public class Config {
    public static final JsonObject JSON;
    public static final Gson GSON;
    public static final File CONFIG_FILE;
    public static final Map<String, Pair<List<Token>, String>> FUNCTIONS;

    static {
        CONFIG_FILE = new File(".", "config/chatcalc.json");
        GSON = new GsonBuilder().setPrettyPrinting().create();
        JSON = new JsonObject();
        File dir = new File(".", "config");
        if ((dir.exists() && dir.isDirectory() || dir.mkdirs()) && !CONFIG_FILE.exists()) {
            JSON.addProperty("decimal_format", "#,##0.##");
            JSON.addProperty("radians", "false");
            JSON.addProperty("debug_tokens", "false");
            JSON.addProperty("euler", "false");
            JSON.addProperty("log_exceptions", "false");
            JSON.addProperty("copy_type", "none");
            refreshJson();
        }
        if (CONFIG_FILE.exists() && CONFIG_FILE.isFile() && CONFIG_FILE.canRead()) {
            readJson();
        }
        FUNCTIONS = new HashMap<>();
        if (JSON.has("functions")) {
            JSON.getAsJsonArray("functions").forEach(e -> FUNCTIONS.put(e.getAsString().split("\\(")[0], new Pair<>(MathEngine.tokenize(e.getAsString().split("=")[1]), e.getAsString().split("[()]")[1])));
        }
    }

    public static DecimalFormat getDecimalFormat() {
        return JSON.has("decimal_format") ? new DecimalFormat(JSON.get("decimal_format").getAsString()) : new DecimalFormat("#,##0.##");
    }

    public static double convertIfRadians(double value) {
        return JSON.has("radians") && Boolean.parseBoolean(JSON.get("radians").getAsString()) ? Math.toRadians(value) : value;
    }

    public static boolean logExceptions() {
        return JSON.has("log_exceptions") && Boolean.parseBoolean(JSON.get("log_exceptions").getAsString());
    }

    public static void refreshJson() {
        try {
            FileWriter writer = new FileWriter(CONFIG_FILE);
            JSON.add("functions", FUNCTIONS.entrySet().stream().map(x -> x.getKey() + "(" + x.getValue().getRight() + ")" + '=' + x.getValue().getLeft().stream().map(Object::toString).collect(Collectors.joining())).collect(JsonArray::new, JsonArray::add, JsonArray::addAll));
            writer.write(GSON.toJson(JSON));
            writer.close();
        } catch (Exception ignored) { }
    }

    public static void readJson() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(CONFIG_FILE));
            JSON.entrySet().forEach(entry -> JSON.remove(entry.getKey()));
            JsonParser.parseString(reader.lines().collect(Collectors.joining("\n"))).getAsJsonObject().entrySet().forEach(entry -> JSON.add(entry.getKey(), entry.getValue()));
            reader.close();
        } catch (Exception ignored) { }
    }

    public static boolean debugTokens() {
        return JSON.has("debug_tokens") && Boolean.parseBoolean(JSON.get("debug_tokens").getAsString());
    }

    public static boolean euler() {
        return JSON.has("euler") && Boolean.parseBoolean(JSON.get("euler").getAsString());
    }

    public static void saveToChatHud(String input) {
        if (JSON.has("copy_type") && JSON.get("copy_type").getAsString().equalsIgnoreCase("chat_history")) {
            final MinecraftClient client = MinecraftClient.getInstance();
            client.inGameHud.getChatHud().addToMessageHistory(input);
        }
    }

    public static double func(String name, double value) {
        if (FUNCTIONS.containsKey(name)) {
            List<Token> tokens = new ArrayList<>(FUNCTIONS.get(name).getLeft());
            MathEngine.simplify(tokens, false, Optional.of(new Pair<>(FUNCTIONS.get(name).getRight(), value)));
            return ((NumberToken) tokens.get(0)).val;
        } else {
            return value;
        }
    }

    public static void saveToClipboard(String input) {
        if (JSON.has("copy_type") && JSON.get("copy_type").getAsString().equalsIgnoreCase("clipboard")) {
            final MinecraftClient client = MinecraftClient.getInstance();
            client.keyboard.setClipboard(input);
        }
    }
}
