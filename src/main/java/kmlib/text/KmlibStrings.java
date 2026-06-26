package kmlib.text;

/**
 * Generic, Starsector-agnostic string predicates shared across the
 * KMLib jar. Lives in its own package (not {@code kmlib.starsector.*})
 * because the helpers here are pure text utilities - they have no
 * dependency on Starsector's API surface and are reused by any KMLib
 * code, Starsector-related or not.
 *
 * <p>Sibling concept to
 * {@link kmlib.starsector.strings.StarsectorStrings}, which is the
 * defensive wrapper around Starsector's {@code settings.json}
 * localisation lookups; this class is purely string-manipulation.
 */
public final class KmlibStrings {

    private KmlibStrings() {
    }

    /**
     * Returns {@code true} when {@code value} is non-null and contains
     * at least one non-whitespace character. Lives here so KMLib does
     * not depend on a util module (Apache Commons, Guava, Spring) just
     * for one predicate; multiple internal callers ({@code
     * StarsectorStrings}, {@code StarsectorPlayerFactionResolver},
     * future consumers) share one implementation.
     */
    public static boolean hasText(String value) {
        if (value == null) {
            return false;
        }
        for (var i = 0; i < value.length(); i++) {
            if (!Character.isWhitespace(value.charAt(i))) {
                return true;
            }
        }
        return false;
    }
}
