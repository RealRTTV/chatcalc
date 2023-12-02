package ca.rttv.chatcalc;

import com.google.common.collect.ImmutableMap;
import com.google.gson.*;
import net.minecraft.client.MinecraftClient;

import java.io.*;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class Config {
    public static final JsonObject JSON;
    public static final Gson GSON;
    public static final File CONFIG_FILE;
    public static final Map<String, CustomFunction> FUNCTIONS;
    public static final Map<String, CustomConstant> CONSTANTS;
    public static final ImmutableMap<String, String> DEFAULTS;

    static {
        DEFAULTS = ImmutableMap.<String, String>builder()
                .put("decimal_format", "#,##0.##")
                .put("radians", "false")
                .put("copy_type", "none")
                .put("calculate_last", "true")
                .put("display_above", "true")
                .build();
        CONFIG_FILE = new File(".", "config/chatcalc.json");
        GSON = new GsonBuilder().setPrettyPrinting().create();
        JSON = new JsonObject();
        File dir = new File(".", "config");
        if ((dir.exists() && dir.isDirectory() || dir.mkdirs()) && !CONFIG_FILE.exists()) {
            try {
                //noinspection ResultOfMethodCallIgnored
                CONFIG_FILE.createNewFile();
                FileWriter writer = new FileWriter(CONFIG_FILE);
                writer.write("{\n");
                for (Map.Entry<String, String> element : DEFAULTS.entrySet()) {
                    writer.write(String.format("    \"%s\": \"%s\",\n", element.getKey(), element.getValue()));
                }
                writer.write("    \"functions\": []\n");
                writer.write("}");
                writer.close();
            } catch (IOException ignored) {}
        }
        FUNCTIONS = new HashMap<>();
        CONSTANTS = new HashMap<>();
        if (CONFIG_FILE.exists() && CONFIG_FILE.isFile() && CONFIG_FILE.canRead()) {
            readJson();
        }
    }

    public static boolean calculateLast() {
        return Boolean.parseBoolean(JSON.get("calculate_last").getAsString());
    }

    public static DecimalFormat getDecimalFormat() {
        return new DecimalFormat(JSON.get("decimal_format").getAsString());
    }

    public static double convertFromDegrees(double value) {
        return Boolean.parseBoolean(JSON.get("radians").getAsString()) ? Math.toRadians(value) : value;
    }

    public static double convertFromRadians(double value) {
        return Boolean.parseBoolean(JSON.get("radians").getAsString()) ? value : Math.toDegrees(value);
    }

    public static double convertToRadians(double value) {
        return Boolean.parseBoolean(JSON.get("radians").getAsString()) ? value : Math.toRadians(value);
    }

    public static boolean radians() {
        return Boolean.parseBoolean(JSON.get("radians").getAsString());
    }

    public static void refreshJson() {
        try {
            FileWriter writer = new FileWriter(CONFIG_FILE);
            JSON.add("functions", FUNCTIONS.values().stream().map(Object::toString).collect(JsonArray::new, JsonArray::add, JsonArray::addAll));
            JSON.add("constants", CONSTANTS.values().stream().map(Object::toString).collect(JsonArray::new, JsonArray::add, JsonArray::addAll));
            writer.write(GSON.toJson(JSON));
            JSON.remove("functions");
            JSON.remove("constants");
            writer.close();
        } catch (Exception ignored) { }
    }

    public static void readJson() {
        try (BufferedReader reader = new BufferedReader(new FileReader(CONFIG_FILE))) {
            var json = new Object() {
                JsonObject obj;
            };
            try {
                json.obj = JsonParser.parseString(reader.lines().collect(Collectors.joining("\n"))).getAsJsonObject();
            } catch (Exception ignored) {
                json.obj = new JsonObject();
            }
            DEFAULTS.forEach((key, defaultValue) -> JSON.add(key, json.obj.get(key) instanceof JsonPrimitive primitive && primitive.isString() ? primitive : new JsonPrimitive(defaultValue)));
            if (json.obj.get("functions") instanceof JsonArray array) {
                array.forEach(e -> {
                    if (e instanceof JsonPrimitive primitive && primitive.isString()) {
                        CustomFunction.fromString(e.getAsString()).ifPresent(func -> FUNCTIONS.put(func.name(), func));
                    }
                });
            }
            if (json.obj.get("constants") instanceof JsonArray array) {
                for (JsonElement e : array) {
                    if (e instanceof JsonPrimitive primitive && primitive.isString()) {
                        CustomConstant.fromString(e.getAsString()).ifPresent(constant -> CONSTANTS.put(constant.name(), constant));
                    }
                }
            }
        } catch (Exception ignored) { }
    }

    public static boolean displayAbove() { return Boolean.parseBoolean(JSON.get("display_above").getAsString()); }

    public static void saveToChatHud(String input) {
        if (JSON.get("copy_type").getAsString().equalsIgnoreCase("chat_history")) {
            final MinecraftClient client = MinecraftClient.getInstance();
            client.inGameHud.getChatHud().addToMessageHistory(input);
        }
    }

    public static double func(String name, double... values) {
        CustomFunction func = FUNCTIONS.get(name);
        if (func != null) {
            if (values.length != func.params().length) {
                throw new IllegalArgumentException("Invalid amount of arguments for custom function");
            }
            String input = func.eval();
            FunctionParameter[] parameters = new FunctionParameter[values.length];
            for (int i = 0; i < parameters.length; i++) {
                parameters[i] = new FunctionParameter(func.params()[i], values[i]);
            }
            return Config.makeEngine().eval(input, parameters);
        } else {
            throw new IllegalArgumentException("Tried to call unknown function: " + name);
        }
    }

    public static void saveToClipboard(String input) {
        if (JSON.get("copy_type").getAsString().equalsIgnoreCase("clipboard")) {
            final MinecraftClient client = MinecraftClient.getInstance();
            client.keyboard.setClipboard(input);
        }
    }

    public static MathEngine makeEngine() {
        return new NibbleMathEngine();
    }
}
