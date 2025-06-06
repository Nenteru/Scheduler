package util;

public class MarkdownFormatter {
    private static final char[] SPECIAL_CHARACTERS = {
        '_', '*', '[', ']', '(', ')', '~', '`', '>', '#', '+', '-', '=', '|', 
        '{', '}', '.', '!', ':', ';', '\\'
    };

    /**
     * Экранирует специальные символы для Telegram MarkdownV2 формата.
     * Порядок экранирования важен! Сначала экранируем сам escape-символ.
     * @param text Текст для экранирования
     * @return Экранированный текст
     */
    public static String escapeMarkdownV2(String text) {
        if (text == null) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            boolean isSpecial = false;
            for (char specialChar : SPECIAL_CHARACTERS) {
                if (specialChar == c) {
                    result.append('\\').append(c);
                    isSpecial = true;
                    break;
                }
            }
            if (!isSpecial) {
                result.append(c);
            }
        }
        return result.toString();
    }

    public static String bold(String text) {
        return "*" + escapeMarkdownV2(text).replace("*", "") + "*";
    }

    public static String italic(String text) {
        return "_" + text + "_";
    }

    public static String code(String text) {
        return "`" + escapeMarkdownV2(text).replace("`", "") + "`";
    }

    public static String codeBlock(String text) {
        return "```\n" + text + "\n```";
    }

    public static String formatAsMarkdownV2(String text, boolean isBold, boolean isCode) {
        String escaped = escapeMarkdownV2(text);
        if (isBold) {
            escaped = "*" + escaped.replace("*", "") + "*";
        }
        if (isCode) {
            escaped = "`" + escaped.replace("`", "") + "`";
        }
        return escaped;
    }
} 