package org.skysongdev.skysongEconomy.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * A read-only view of one section of a parsed YAML file, with typed getters that fail with a
 * {@link ConfigException} naming the exact key at fault.
 * <p>
 * It wraps the plain maps SnakeYAML produces, so config parsing can be unit tested without a
 * server.
 */
public final class ConfigSection {
    private final String path;
    private final Map<String, Object> values;

    private ConfigSection(String path, Map<String, Object> values) {
        this.path = path;
        this.values = values;
    }

    /**
     * Wraps the root of a parsed file.
     *
     * @param fileName shown in error messages, e.g. {@code businesses/types.yml}
     * @param root     the parsed file; {@code null} (an empty file) is treated as empty
     */
    public static ConfigSection root(String fileName, Map<?, ?> root) {
        return new ConfigSection(fileName + ":", copyKeys(root));
    }

    /** @return where this section is, e.g. {@code businesses/types.yml: types.gathering} */
    public String path() {
        return path;
    }

    /** @return the keys of this section, in file order */
    public Set<String> keys() {
        return values.keySet();
    }

    public boolean contains(String key) {
        return values.containsKey(key);
    }

    /** @return the sub-section at {@code key}, or an empty one when it is missing */
    public ConfigSection section(String key) {
        Object value = values.get(key);
        if (value == null) {
            return new ConfigSection(childPath(key), Map.of());
        }
        if (!(value instanceof Map<?, ?> map)) {
            throw error(key, "must be a section");
        }
        return new ConfigSection(childPath(key), copyKeys(map));
    }

    /** @return the text at {@code key}, or {@code fallback} when it is missing */
    public String string(String key, String fallback) {
        Object value = values.get(key);
        if (value == null) {
            return fallback;
        }
        if (value instanceof Map<?, ?> || value instanceof List<?>) {
            throw error(key, "must be text");
        }
        return value.toString();
    }

    /** @return the text at {@code key} */
    public String requireString(String key) {
        String value = string(key, null);
        if (value == null || value.isBlank()) {
            throw error(key, "is required");
        }
        return value;
    }

    /** @return the whole number at {@code key}, or {@code fallback} when it is missing */
    public int integer(String key, int fallback) {
        Object value = values.get(key);
        if (value == null) {
            return fallback;
        }
        if (value instanceof Integer || value instanceof Long || value instanceof Short) {
            return ((Number) value).intValue();
        }
        throw error(key, "must be a whole number");
    }

    /** @return the number at {@code key}, or {@code fallback} when it is missing */
    public double number(String key, double fallback) {
        Object value = values.get(key);
        if (value == null) {
            return fallback;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        throw error(key, "must be a number");
    }

    /** @return the true/false value at {@code key}, or {@code fallback} when it is missing */
    public boolean bool(String key, boolean fallback) {
        Object value = values.get(key);
        if (value == null) {
            return fallback;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        throw error(key, "must be true or false");
    }

    /** @return the list of texts at {@code key}, empty when it is missing */
    public List<String> strings(String key) {
        Object value = values.get(key);
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> list)) {
            throw error(key, "must be a list");
        }
        List<String> result = new ArrayList<>(list.size());
        for (Object element : list) {
            if (element == null || element instanceof Map<?, ?> || element instanceof List<?>) {
                throw error(key, "must be a list of texts");
            }
            result.add(element.toString());
        }
        return List.copyOf(result);
    }

    /**
     * Lists every value below this section that is not itself a section, by its dotted path
     * from here, e.g. {@code business.created}. Lists stay lists.
     */
    public Map<String, Object> leaves() {
        Map<String, Object> leaves = new LinkedHashMap<>();
        collectLeaves("", values, leaves);
        return leaves;
    }

    private static void collectLeaves(String prefix, Map<?, ?> map, Map<String, Object> into) {
        map.forEach((key, value) -> {
            String path = prefix + key;
            if (value instanceof Map<?, ?> child) {
                collectLeaves(path + ".", child, into);
            } else if (value != null) {
                into.put(path, value);
            }
        });
    }

    /** @return an error about {@code key} of this section, ready to throw */
    public ConfigException error(String key, String problem) {
        return new ConfigException(childPath(key) + " " + problem);
    }

    private String childPath(String key) {
        return path.endsWith(":") ? path + " " + key : path + "." + key;
    }

    private static Map<String, Object> copyKeys(Map<?, ?> map) {
        if (map == null) {
            return Map.of();
        }
        Map<String, Object> copy = new LinkedHashMap<>();
        map.forEach((key, value) -> copy.put(String.valueOf(Objects.requireNonNull(key)), value));
        return copy;
    }
}
