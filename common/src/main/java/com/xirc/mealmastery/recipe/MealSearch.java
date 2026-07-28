package com.xirc.mealmastery.recipe;

import java.util.List;
import java.util.Locale;

/**
 * The journal's search behaviour.
 *
 * <p>Operates on a flattened projection rather than on live {@code Component}s
 * so the matching rules are testable and so the client can build the index once
 * per screen open instead of re-translating on every keystroke.</p>
 *
 * <p>Search is intentionally forgiving: a bare term matches any field, while a
 * {@code field:term} prefix restricts it. Nothing here does fuzzy matching —
 * players searching {@code tomato} expect tomato dishes, not everything within
 * two edits of it.</p>
 */
public final class MealSearch {

    private MealSearch() {
    }

    /**
     * Everything one dish can be found by.
     *
     * @param displayName translated item name
     * @param recipeId    the mastery target id, matched as a whole and by path
     * @param modName     translated mod name
     * @param modId       source namespace
     * @param methods     translated method names
     * @param category    translated category name
     * @param ingredients translated ingredient names
     */
    public record Indexed(String displayName,
                          String recipeId,
                          String modName,
                          String modId,
                          List<String> methods,
                          String category,
                          List<String> ingredients) {

        public Indexed {
            methods = List.copyOf(methods);
            ingredients = List.copyOf(ingredients);
        }
    }

    public enum Field {
        ANY(""),
        NAME("name"),
        INGREDIENT("ingredient"),
        MOD("mod"),
        METHOD("method"),
        CATEGORY("category"),
        ID("id");

        private final String prefix;

        Field(String prefix) {
            this.prefix = prefix;
        }

        public String prefix() {
            return prefix;
        }

        static Field byPrefix(String prefix) {
            for (Field field : values()) {
                if (field != ANY && field.prefix.equals(prefix)) {
                    return field;
                }
            }
            return null;
        }
    }

    /** A parsed query term. */
    public record Term(Field field, String text) {
    }

    /**
     * Splits a raw query into terms. Whitespace separates terms and every term
     * must match, so {@code tomato pot} finds tomato dishes made in a pot.
     */
    public static List<Term> parse(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        List<Term> terms = new java.util.ArrayList<>();
        for (String raw : query.trim().toLowerCase(Locale.ROOT).split("\\s+")) {
            int colon = raw.indexOf(':');
            // A colon that looks like a namespace ("farmersdelight:onion_soup")
            // is part of the term, not a field selector.
            if (colon > 0 && colon < raw.length() - 1) {
                Field field = Field.byPrefix(raw.substring(0, colon));
                if (field != null) {
                    terms.add(new Term(field, raw.substring(colon + 1)));
                    continue;
                }
            }
            terms.add(new Term(Field.ANY, raw));
        }
        return terms;
    }

    public static boolean matches(Indexed indexed, String query) {
        List<Term> terms = parse(query);
        if (terms.isEmpty()) {
            return true;
        }
        for (Term term : terms) {
            if (!matches(indexed, term)) {
                return false;
            }
        }
        return true;
    }

    public static boolean matches(Indexed indexed, Term term) {
        String text = term.text();
        if (text.isEmpty()) {
            return true;
        }
        return switch (term.field()) {
            case NAME -> contains(indexed.displayName(), text);
            case INGREDIENT -> containsAny(indexed.ingredients(), text);
            case MOD -> contains(indexed.modName(), text) || contains(indexed.modId(), text);
            case METHOD -> containsAny(indexed.methods(), text);
            case CATEGORY -> contains(indexed.category(), text);
            case ID -> contains(indexed.recipeId(), text);
            case ANY -> contains(indexed.displayName(), text)
                    || contains(indexed.recipeId(), text)
                    || contains(indexed.modName(), text)
                    || contains(indexed.modId(), text)
                    || contains(indexed.category(), text)
                    || containsAny(indexed.methods(), text)
                    || containsAny(indexed.ingredients(), text);
        };
    }

    private static boolean contains(String haystack, String needle) {
        return haystack != null && haystack.toLowerCase(Locale.ROOT).contains(needle);
    }

    private static boolean containsAny(List<String> haystacks, String needle) {
        for (String haystack : haystacks) {
            if (contains(haystack, needle)) {
                return true;
            }
        }
        return false;
    }
}
