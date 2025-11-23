package project.retreever.schema.resolver.util;

/**
 * Utility class for describing constraints applied to JSON properties.
 * These constraints mirror common validation rules such as
 * NOT_NULL, LENGTH, RANGE, REGEX, ENUM, etc.
 */
public final class JsonPropertyConstraint {

    // Core constraints
    public static final String NOT_NULL = "NOT_NULL";
    public static final String NOT_BLANK = "NOT_BLANK";
    public static final String NOT_EMPTY = "NOT_EMPTY";

    // Parameterized constraints
    private static final String ENUM = "ALLOWED_VALUES";
    private static final String MIN_LENGTH = "MIN_LENGTH";
    private static final String MAX_LENGTH = "MAX_LENGTH";
    private static final String MIN_VALUE = "MIN_VALUE";
    private static final String MAX_VALUE = "MAX_VALUE";
    private static final String REGEX = "REGEX";

    private JsonPropertyConstraint() {
        // utility class
    }

    // Length constraints
    public static String minLength(int minLength) {
        return format(MIN_LENGTH, minLength);
    }

    public static String maxLength(int maxLength) {
        return format(MAX_LENGTH, maxLength);
    }

    // Value range constraints
    public static String minValue(double minValue) {
        return format(MIN_VALUE, minValue);
    }

    public static String maxValue(double maxValue) {
        return format(MAX_VALUE, maxValue);
    }

    // Regex constraint
    public static String regex(String regex) {
        return format(REGEX, regex);
    }

    // Allowed enum values
    public static String enumValue(String... enumValues) {
        return ENUM + ":[" + String.join(", ", enumValues) + "]";
    }

    // Generic formatter
    private static String format(String type, Object value) {
        return type + ":" + value;
    }
}
