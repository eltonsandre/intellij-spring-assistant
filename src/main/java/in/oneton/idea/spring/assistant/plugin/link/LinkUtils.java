package in.oneton.idea.spring.assistant.plugin.link;

import org.jetbrains.annotations.NotNull;

public abstract class LinkUtils {

    public static final String PLACEHOLDER_REGEX = "^(\\$\\{)(.*)(\\})";

    public static final String PLACERHOLDER_PREFIX = "${";
    public static final String PLACERHOLDER_SUFIX = "}";


    public static String getKeyToPlaceholder(@NotNull final String valueKey) {
        return valueKey.substring(valueKey.indexOf(PLACERHOLDER_PREFIX) + 2, valueKey.indexOf(PLACERHOLDER_SUFIX));
    }

    public static boolean isPlaceholderContainer(@NotNull final String valueKey) {
        return valueKey.matches(PLACEHOLDER_REGEX);
    }


    public static String truncateStringWithEllipsis(final String text, final int length) {
        if (text != null && text.length() > length) {
            return text.substring(0, length - 3) + "...";
        }
        return text;
    }
}
