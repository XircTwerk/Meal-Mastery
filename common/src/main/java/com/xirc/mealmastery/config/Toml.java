package com.xirc.mealmastery.config;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The config file format: reflective TOML over the plain-field config classes.
 *
 * <p>Deliberately hand-rolled rather than pulling in a TOML library. The jar
 * ships no third-party code at all, and a config document made of scalars,
 * string arrays and one array of tables does not justify being the first
 * exception.</p>
 *
 * <p>Only the subset those classes actually use is supported: booleans,
 * integers, floating point, strings, enums, arrays of strings or integers,
 * nested sections, and arrays of sections. Anything else throws rather than
 * silently writing a file that cannot be read back.</p>
 *
 * <p>Reading is forgiving in both directions on purpose. An unknown key is
 * ignored, so downgrading does not destroy a file; a missing key keeps the
 * field's default, which is what lets a new setting appear in an old file
 * without the player losing what they had set.</p>
 */
public final class Toml {

    /** Thrown when a document cannot be parsed at all; the caller quarantines. */
    public static final class TomlException extends RuntimeException {
        public TomlException(String message) {
            super(message);
        }
    }

    private Toml() {
    }

    // ---------------------------------------------------------------- writing

    public static String write(Object config, List<String> header) {
        StringBuilder out = new StringBuilder();
        for (String line : header) {
            out.append(line.isEmpty() ? "#" : "# ").append(line).append('\n');
        }
        writeTable(out, "", config, !header.isEmpty(), true);
        return out.toString();
    }

    /**
     * Writes one table's keys, then its sub-tables.
     *
     * <p>The ordering is a TOML requirement, not a preference: a bare key after
     * a {@code [section]} header belongs to that section, so every scalar has
     * to be emitted before the first nested header regardless of the order the
     * fields happen to be declared in.</p>
     */
    private static void writeTable(StringBuilder out, String path, Object owner,
                                   boolean spaced, boolean comments) {
        List<Field> scalars = new ArrayList<>();
        List<Field> tables = new ArrayList<>();
        for (Field field : fieldsOf(owner.getClass())) {
            (isTable(field) ? tables : scalars).add(field);
        }

        for (Field field : scalars) {
            Object value = get(field, owner);
            if (value == null) {
                continue;
            }
            if (spaced) {
                out.append('\n');
                spaced = false;
            }
            if (comments) {
                comment(out, field.getAnnotation(Comment.class));
            }
            out.append(field.getName()).append(" = ").append(format(value)).append('\n');
        }

        for (Field field : tables) {
            Object value = get(field, owner);
            if (value == null) {
                continue;
            }
            String child = path.isEmpty() ? field.getName() : path + "." + field.getName();
            Comment note = field.getAnnotation(Comment.class);
            if (value instanceof List<?> elements) {
                boolean first = true;
                for (Object element : elements) {
                    if (element == null) {
                        continue;
                    }
                    out.append('\n');
                    comment(out, note);
                    note = null;
                    out.append("[[").append(child).append("]]\n");
                    // Only the first entry is annotated; repeating the same
                    // field comments on every element buries the values.
                    writeTable(out, child, element, false, first);
                    first = false;
                }
            } else {
                out.append('\n');
                comment(out, note);
                comment(out, value.getClass().getAnnotation(Comment.class));
                out.append('[').append(child).append("]\n");
                writeTable(out, child, value, false, true);
            }
        }
    }

    private static void comment(StringBuilder out, Comment note) {
        if (note == null) {
            return;
        }
        for (String line : note.value()) {
            out.append(line.isEmpty() ? "#" : "# ").append(line).append('\n');
        }
    }

    private static String format(Object value) {
        if (value instanceof Boolean || value instanceof Integer || value instanceof Long) {
            return value.toString();
        }
        if (value instanceof Float || value instanceof Double) {
            // Printed at the field's own precision: widening 0.2F to a double
            // would write 0.20000000298023224 into a file meant to be read.
            String text = value instanceof Float single
                    ? Float.toString(single)
                    : Double.toString((Double) value);
            // TOML floats must carry a point or an exponent; "1" would read back
            // as an integer, which is legal but changes the document's meaning.
            return text.contains(".") || text.contains("E") || text.contains("e") ? text : text + ".0";
        }
        if (value instanceof Enum<?> constant) {
            return quote(constant.name());
        }
        if (value instanceof String text) {
            return quote(text);
        }
        if (value instanceof List<?> elements) {
            StringBuilder out = new StringBuilder("[");
            for (int i = 0; i < elements.size(); i++) {
                out.append(i == 0 ? "" : ", ").append(format(elements.get(i)));
            }
            return out.append(']').toString();
        }
        throw new IllegalArgumentException("No TOML representation for " + value.getClass());
    }

    private static String quote(String text) {
        StringBuilder out = new StringBuilder("\"");
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> out.append(c);
            }
        }
        return out.append('"').toString();
    }

    // ---------------------------------------------------------------- reading

    public static <T> T read(String text, Class<T> type) {
        Map<String, Object> root = parse(text);
        T config = instantiate(type);
        bind(root, config);
        return config;
    }

    /** Parses into nested maps; array-of-table headers accumulate into lists. */
    private static Map<String, Object> parse(String text) {
        Map<String, Object> root = new LinkedHashMap<>();
        Map<String, Object> current = root;
        int lineNumber = 0;
        for (String raw : text.split("\r?\n")) {
            lineNumber++;
            String line = strip(raw);
            if (line.isEmpty()) {
                continue;
            }
            if (line.startsWith("[[") && line.endsWith("]]")) {
                current = arrayTable(root, line.substring(2, line.length() - 2).trim(), lineNumber);
            } else if (line.startsWith("[") && line.endsWith("]")) {
                current = table(root, line.substring(1, line.length() - 1).trim(), lineNumber);
            } else {
                int equals = line.indexOf('=');
                if (equals < 0) {
                    throw new TomlException("line " + lineNumber + ": expected 'key = value'");
                }
                String key = line.substring(0, equals).trim();
                if (key.isEmpty()) {
                    throw new TomlException("line " + lineNumber + ": missing key");
                }
                current.put(key, value(line.substring(equals + 1).trim(), lineNumber));
            }
        }
        return root;
    }

    /** Removes an unquoted trailing comment and surrounding whitespace. */
    private static String strip(String raw) {
        boolean quoted = false;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '"' && (i == 0 || raw.charAt(i - 1) != '\\')) {
                quoted = !quoted;
            } else if (c == '#' && !quoted) {
                return raw.substring(0, i).trim();
            }
        }
        return raw.trim();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> table(Map<String, Object> root, String path, int line) {
        Map<String, Object> node = root;
        for (String part : path.split("\\.")) {
            String key = part.trim();
            if (key.isEmpty()) {
                throw new TomlException("line " + line + ": empty section name");
            }
            Object existing = node.get(key);
            if (existing instanceof Map) {
                node = (Map<String, Object>) existing;
            } else if (existing instanceof List<?> elements && !elements.isEmpty()
                    && elements.get(elements.size() - 1) instanceof Map) {
                node = (Map<String, Object>) elements.get(elements.size() - 1);
            } else {
                Map<String, Object> child = new LinkedHashMap<>();
                node.put(key, child);
                node = child;
            }
        }
        return node;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> arrayTable(Map<String, Object> root, String path, int line) {
        int split = path.lastIndexOf('.');
        Map<String, Object> parent = split < 0 ? root : table(root, path.substring(0, split), line);
        String key = (split < 0 ? path : path.substring(split + 1)).trim();
        Object existing = parent.get(key);
        List<Object> elements;
        if (existing instanceof List) {
            elements = (List<Object>) existing;
        } else {
            elements = new ArrayList<>();
            parent.put(key, elements);
        }
        Map<String, Object> element = new LinkedHashMap<>();
        elements.add(element);
        return element;
    }

    private static Object value(String raw, int line) {
        if (raw.isEmpty()) {
            throw new TomlException("line " + line + ": missing value");
        }
        if (raw.startsWith("[")) {
            if (!raw.endsWith("]")) {
                throw new TomlException("line " + line + ": array is not closed on one line");
            }
            List<Object> elements = new ArrayList<>();
            for (String part : splitArray(raw.substring(1, raw.length() - 1), line)) {
                if (!part.isBlank()) {
                    elements.add(value(part.trim(), line));
                }
            }
            return elements;
        }
        if (raw.startsWith("\"")) {
            if (raw.length() < 2 || !raw.endsWith("\"")) {
                throw new TomlException("line " + line + ": string is not closed");
            }
            return unquote(raw.substring(1, raw.length() - 1));
        }
        if (raw.equals("true") || raw.equals("false")) {
            return Boolean.valueOf(raw);
        }
        String number = raw.replace("_", "");
        try {
            if (number.contains(".") || number.contains("e") || number.contains("E")) {
                return Double.valueOf(number);
            }
            return Long.valueOf(number);
        } catch (NumberFormatException failure) {
            throw new TomlException("line " + line + ": '" + raw + "' is not a value");
        }
    }

    private static List<String> splitArray(String body, int line) {
        List<String> parts = new ArrayList<>();
        boolean quoted = false;
        int depth = 0;
        int start = 0;
        for (int i = 0; i < body.length(); i++) {
            char c = body.charAt(i);
            if (c == '"' && (i == 0 || body.charAt(i - 1) != '\\')) {
                quoted = !quoted;
            } else if (!quoted && c == '[') {
                depth++;
            } else if (!quoted && c == ']') {
                depth--;
            } else if (!quoted && depth == 0 && c == ',') {
                parts.add(body.substring(start, i));
                start = i + 1;
            }
        }
        if (quoted) {
            throw new TomlException("line " + line + ": string is not closed");
        }
        parts.add(body.substring(start));
        return parts;
    }

    private static String unquote(String text) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c != '\\' || i + 1 >= text.length()) {
                out.append(c);
                continue;
            }
            char next = text.charAt(++i);
            switch (next) {
                case 'n' -> out.append('\n');
                case 'r' -> out.append('\r');
                case 't' -> out.append('\t');
                case '"' -> out.append('"');
                case '\\' -> out.append('\\');
                default -> out.append(next);
            }
        }
        return out.toString();
    }

    // ---------------------------------------------------------------- binding

    @SuppressWarnings("unchecked")
    private static void bind(Map<String, Object> source, Object target) {
        for (Field field : fieldsOf(target.getClass())) {
            Object raw = source.get(field.getName());
            if (raw == null) {
                continue;
            }
            Class<?> type = field.getType();
            try {
                if (raw instanceof Map && !List.class.isAssignableFrom(type)) {
                    Object child = get(field, target);
                    if (child == null) {
                        child = instantiate(type);
                        set(field, target, child);
                    }
                    bind((Map<String, Object>) raw, child);
                } else if (List.class.isAssignableFrom(type) && raw instanceof List<?> elements) {
                    set(field, target, elements(elements, elementType(field)));
                } else {
                    Object converted = convert(raw, type);
                    if (converted != null) {
                        set(field, target, converted);
                    }
                }
            } catch (RuntimeException failure) {
                // One unreadable key must not cost the whole file. The field
                // keeps its default and validate() reports nothing, because
                // nothing about the loaded state is out of range.
                com.xirc.mealmastery.MealMasteryLog.LOGGER.warn(
                        "Ignoring config key '{}': {}", field.getName(), failure.getMessage());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Object> elements(List<?> raw, Class<?> element) {
        List<Object> out = new ArrayList<>();
        for (Object item : raw) {
            if (item instanceof Map && !isScalar(element)) {
                Object child = instantiate(element);
                bind((Map<String, Object>) item, child);
                out.add(child);
            } else {
                Object converted = convert(item, element);
                if (converted != null) {
                    out.add(converted);
                }
            }
        }
        return out;
    }

    private static Object convert(Object raw, Class<?> type) {
        if (type == boolean.class || type == Boolean.class) {
            return raw instanceof Boolean flag ? flag : null;
        }
        if (raw instanceof Number number) {
            if (type == int.class || type == Integer.class) {
                return number.intValue();
            }
            if (type == long.class || type == Long.class) {
                return number.longValue();
            }
            if (type == double.class || type == Double.class) {
                return number.doubleValue();
            }
            if (type == float.class || type == Float.class) {
                return number.floatValue();
            }
        }
        if (raw instanceof String text) {
            if (type == String.class) {
                return text;
            }
            if (type.isEnum()) {
                for (Object constant : type.getEnumConstants()) {
                    if (((Enum<?>) constant).name().equalsIgnoreCase(text.trim())) {
                        return constant;
                    }
                }
                throw new IllegalArgumentException(
                        "'" + text + "' is not one of " + names(type));
            }
        }
        return null;
    }

    private static String names(Class<?> type) {
        List<String> names = new ArrayList<>();
        for (Object constant : type.getEnumConstants()) {
            names.add(((Enum<?>) constant).name().toLowerCase(Locale.ROOT));
        }
        return String.join(", ", names);
    }

    // ------------------------------------------------------------- reflection

    private static List<Field> fieldsOf(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        for (Field field : type.getDeclaredFields()) {
            int modifiers = field.getModifiers();
            if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers)
                    || field.isSynthetic()) {
                continue;
            }
            field.setAccessible(true);
            fields.add(field);
        }
        return fields;
    }

    private static boolean isTable(Field field) {
        Class<?> type = field.getType();
        if (List.class.isAssignableFrom(type)) {
            return !isScalar(elementType(field));
        }
        return !isScalar(type);
    }

    private static boolean isScalar(Class<?> type) {
        return type.isPrimitive() || type.isEnum() || type == String.class
                || Number.class.isAssignableFrom(type) || type == Boolean.class;
    }

    private static Class<?> elementType(Field field) {
        Type generic = field.getGenericType();
        if (generic instanceof ParameterizedType parameterized) {
            Type[] arguments = parameterized.getActualTypeArguments();
            if (arguments.length == 1 && arguments[0] instanceof Class<?> element) {
                return element;
            }
        }
        return String.class;
    }

    private static Object get(Field field, Object owner) {
        try {
            return field.get(owner);
        } catch (IllegalAccessException failure) {
            throw new IllegalStateException("Unreadable config field " + field.getName(), failure);
        }
    }

    private static void set(Field field, Object owner, Object value) {
        try {
            field.set(owner, value);
        } catch (IllegalAccessException failure) {
            throw new IllegalStateException("Unwritable config field " + field.getName(), failure);
        }
    }

    private static <T> T instantiate(Class<T> type) {
        try {
            var constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException(
                    type.getName() + " needs a no-argument constructor", failure);
        }
    }
}
