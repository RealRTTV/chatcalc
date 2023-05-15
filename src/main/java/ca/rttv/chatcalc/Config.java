package ca.rttv.chatcalc;

import ca.rttv.chatcalc.tokens.NumberToken;
import ca.rttv.chatcalc.tokens.Token;
import com.google.common.collect.ImmutableMap;
import com.google.gson.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Pair;
import oshi.util.tuples.Triplet;

import java.io.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

public class Config {
    public static final JsonObject JSON;
    public static final Gson GSON;
    public static final File CONFIG_FILE;
    public static final Map<String, Pair<List<Token>, String[]>> FUNCTIONS;
    public static final ImmutableMap<String, String> DEFAULTS;

    static {
        DEFAULTS = ImmutableMap.<String, String>builder()
                .put("decimal_format", "#,##0.##")
                .put("radians", "false")
                .put("debug_tokens", "false")
                .put("log_exceptions", "false")
                .put("copy_type", "none")
                .put("calculate_last", "true")
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

    public static double convertIfRadians(double value) {
        return Boolean.parseBoolean(JSON.get("radians").getAsString()) ? value : Math.toRadians(value); // sine takes in radians, so we have to do inverse, if we have radians, it'll convert, if we don't, we need to cancel out
    }

    public static boolean logExceptions() {
        return Boolean.parseBoolean(JSON.get("log_exceptions").getAsString());
    }

    public static void refreshJson() {
        try {
            FileWriter writer = new FileWriter(CONFIG_FILE);
            JSON.add("functions", FUNCTIONS.entrySet().stream().map(x -> x.getKey() + "(" + String.join(ChatCalc.SEPARATOR, x.getValue().getRight()) + ")=" + x.getValue().getLeft().stream().map(Object::toString).collect(Collectors.joining())).collect(JsonArray::new, JsonArray::add, JsonArray::addAll));
            writer.write(GSON.toJson(JSON));
            JSON.remove("functions");
            writer.close();
        } catch (Exception ignored) { }
    }

    public static void readJson() {
        try (BufferedReader reader = new BufferedReader(new FileReader(CONFIG_FILE))) {
            JsonObject tempJson;
            try {
                tempJson = JsonParser.parseString(reader.lines().collect(Collectors.joining("\n"))).getAsJsonObject();
            } catch (Exception ignored) {
                tempJson = new JsonObject();
            }
            JsonObject json = tempJson; // annoying lambda requirement
            DEFAULTS.forEach((key, defaultValue) -> JSON.add(key, json.get(key) instanceof JsonPrimitive primitive && primitive.isString() ? primitive : new JsonPrimitive(defaultValue)));
            if (json.get("functions") instanceof JsonArray array) {
                array.forEach(e -> {
                    if (e instanceof JsonPrimitive primitive && primitive.isString()) {
                        parseFunction(e.getAsString()).ifPresent(parsedFunction -> FUNCTIONS.put(parsedFunction.getA(), new Pair<>(parsedFunction.getB(), parsedFunction.getC())));
                    }
                });
            }
        } catch (Exception ignored) { }
    }

    public static Optional<Triplet<String, List<Token>, String[]>> parseFunction(String function) {
        int functionNameEnd = function.indexOf('(');
        if (functionNameEnd > 0) {
            String functionName = function.substring(0, functionNameEnd);
            int paramsEnd = function.substring(functionNameEnd).indexOf(')') + functionNameEnd;
            if (functionName.matches("[A-Za-z]+") && paramsEnd > 0 && function.substring(paramsEnd + 1).startsWith("=") && function.length() > paramsEnd + 2) { // I'm not commenting why this works, I know it, It's just hard to explain
                String[] params = function.substring(functionNameEnd + 1, paramsEnd).split(ChatCalc.SEPARATOR);
                for (String param : params) {
                    if (!param.matches("[A-Za-z]")) {
                        return Optional.empty();
                    }
                }
                String rest = function.substring(paramsEnd + 2);
                try {
                    List<Token> tokens = MathEngine.tokenize(rest);
                    return Optional.of(new Triplet<>(functionName, tokens, params));
//                    System.out.printf("fn: %s, params: %s, rest: %s%n", functionName, params, rest);
                } catch (Exception ignored) { }
            }
        }
        return Optional.empty();
    }

    public static boolean debugTokens() {
        return Boolean.parseBoolean(JSON.get("debug_tokens").getAsString());
    }

    public static void saveToChatHud(String input) {
        if (JSON.get("copy_type").getAsString().equalsIgnoreCase("chat_history")) {
            final MinecraftClient client = MinecraftClient.getInstance();
            client.inGameHud.getChatHud().addToMessageHistory(input);
        }
    }

    public static double func(String name, double... values) {
        if (FUNCTIONS.containsKey(name)) {
            if (values.length != FUNCTIONS.get(name).getRight().length) {
                throw new IllegalArgumentException();
            }
            List<Token> tokens = new ArrayList<>(FUNCTIONS.get(name).getLeft());
            FunctionParameter[] parameters = new FunctionParameter[values.length];
            for (int i = 0; i < parameters.length; i++) {
                parameters[i] = new FunctionParameter(FUNCTIONS.get(name).getRight()[i], values[i]);
            }
            MathEngine.simplify(tokens, false, Optional.of(parameters));
            if (tokens.get(0) instanceof NumberToken numberToken) {
                return numberToken.val;
            }
            throw new IllegalArgumentException();
        } else {
            if (values.length == 0) {
                throw new IllegalArgumentException();
            }
            return values[0];
        }
    }

    public static void saveToClipboard(String input) {
        if (JSON.get("copy_type").getAsString().equalsIgnoreCase("clipboard")) {
            final MinecraftClient client = MinecraftClient.getInstance();
            client.keyboard.setClipboard(input);
        }
    }
}
