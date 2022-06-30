package ca.rttv.chatcalc;

import net.minecraft.client.gui.widget.TextFieldWidget;

public class ChatHelper {
    public static String getWord(String input, int cursor) {
        return input.substring(ChatHelper.getStartOfWord(input, cursor), ChatHelper.getEndOfWord(input, cursor));
    }

    public static boolean replaceWord(TextFieldWidget field, String replacement) {
        String input = field.getText();
        int cursor = field.getCursor();
        int start = ChatHelper.getStartOfWord(input, cursor);
        int end = ChatHelper.getEndOfWord(input, cursor);
        String output = input.substring(0, start) + replacement + input.substring(end);
        if (output.length() > 256 || input.substring(start, end).equals(replacement)) {
            return false;
        }
        field.setText(output);
        return true;
    }

    public static boolean addWordAfterIndex(TextFieldWidget field, int index, String word) {
        String input = field.getText();
        String output = input.substring(0, index) + word + input.substring(index);
        if (output.length() > 256) {
            return false;
        }
        field.setText(output);
        return true;
    }

    public static int getStartOfWord(String input, int cursor) {
        if (cursor == 0) {
            return 0;
        }
        if (input.charAt(cursor - 1) == ' ') {
            return cursor;
        }
        for (int i = cursor - 1; i > 0; --i) {
            if (input.charAt(i - 1) == ' ') {
                return i;
            }
        }
        return 0;
    }

    public static int getEndOfWord(String input, int cursor) {
        if (cursor == input.length() - 1) {
            return cursor;
        }
        for (int i = cursor; i < input.length(); ++i) {
            if (input.charAt(i) == ' ') {
                return i;
            }
        }
        return input.length();
    }
}
