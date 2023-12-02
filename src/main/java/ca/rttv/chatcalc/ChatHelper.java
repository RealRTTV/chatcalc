package ca.rttv.chatcalc;

import net.minecraft.client.gui.widget.TextFieldWidget;

public class ChatHelper {
    public static String getSection(String input, int cursor) {
        return input.substring(ChatHelper.getStartOfSection(input, cursor), ChatHelper.getEndOfSection(input, cursor));
    }

    public static boolean replaceSection(TextFieldWidget field, String replacement) {
        String input = field.getText();
        int cursor = field.getCursor();
        int start = ChatHelper.getStartOfSection(input, cursor);
        int end = ChatHelper.getEndOfSection(input, cursor);
        String output = input.substring(0, start) + replacement + input.substring(end);
        if (output.length() > 256 || input.substring(start, end).equals(replacement)) {
            return false;
        }
        field.setText(output);
        return true;
    }

    public static boolean addSectionAfterIndex(TextFieldWidget field, String word) {
        String input = field.getText();
        int index = ChatHelper.getEndOfSection(input, field.getCursor());
        String output = input.substring(0, index) + word + input.substring(index);
        if (output.length() > 256) {
            return false;
        }
        field.setText(output);
        return true;
    }

    public static int getStartOfSection(String input, int cursor) {
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

    public static int getEndOfSection(String input, int cursor) {
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
