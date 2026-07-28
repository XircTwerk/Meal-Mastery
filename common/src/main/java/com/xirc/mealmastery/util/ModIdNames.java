package com.xirc.mealmastery.util;

import java.util.Locale;

/**
 * Fallback pretty-printing for mod ids the loader has no metadata for.
 *
 * <p>Used by the journal's source-mod labels so an unknown namespace still
 * reads as {@code "Nethers Delight"} rather than {@code "nethersdelight"}.</p>
 */
public final class ModIdNames {
    private ModIdNames() {
    }

    public static String prettify(String modId) {
        if (modId == null || modId.isEmpty()) {
            return "Unknown";
        }
        String[] words = modId.replace('_', ' ').replace('-', ' ').split(" ");
        StringBuilder builder = new StringBuilder(modId.length());
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(word.charAt(0)))
                    .append(word.substring(1).toLowerCase(Locale.ROOT));
        }
        return builder.length() == 0 ? modId : builder.toString();
    }
}
