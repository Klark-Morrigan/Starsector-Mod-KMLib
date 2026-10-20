package kmlib.mods.console;

import kmlib.starsector.strings.StarsectorStrings;

/**
 * The read for every string a console command prints, taking {@code (category, key)} as
 * {@link StarsectorStrings} does.
 *
 * <p>Console Commands draws in fonts of its own, which hold no CJK glyphs, so a Chinese line prints as a row
 * of {@code ?}. A translated console string therefore carries the default's English after
 * {@link #ENGLISH_COPY_MARKER}, and this read answers that English copy. A string with no marker, such as
 * every string of the default locale, answers whole. Every piece a message is built from comes back in one
 * language, so a template fills and a report composes as it would untranslated.
 *
 * <p>The translation is kept, and held to the default's arguments, for the day the console's fonts draw it.
 * TODO: Answer the part before the marker once Console Commands ships fonts holding CJK glyphs, then cut the
 * English copies from the translated bundles.
 *
 * <p>Free of Console Commands' own types, so a class reading its text loads on an install without that mod.
 */
public final class ConsoleStrings {

    /**
     * What every console string's key starts with, so a locale check can find the strings that must carry an
     * English copy. The text a console prints and the text a screen draws are read differently, and the key
     * is the one place both the reader and the check can see which a string is.
     */
    public static final String CONSOLE_KEY_PREFIX = "console_";

    /**
     * What parts a translation from the English copy beneath it: a blank line, then a line naming the
     * language. Console Commands' table spells each line break as a backslash-n, so its help cells carry
     * this with every newline written that way.
     */
    public static final String ENGLISH_COPY_MARKER = "\n\n-- English --\n";

    private ConsoleStrings() {
    }

    /**
     * The console string registered under {@code (category, key)}: its English copy where it carries one,
     * else the whole string.
     *
     * @param category the strings.json category the key sits in
     * @param key      the string ID inside that category
     * @return the wording, or {@link StarsectorStrings#REDACTED} where the read fails
     */
    public static String get(String category, String key) {
        return readEnglishCopy(StarsectorStrings.get(category, key));
    }

    /**
     * {@link #get(String, String)} filled against {@code args}, the English copy alone being filled.
     *
     * @param category the strings.json category the key sits in
     * @param key      the string ID of the template inside that category
     * @param args     what fills the template's slots, in order
     * @return the filled template, or {@link StarsectorStrings#REDACTED} where the read or the fill fails
     */
    public static String format(String category, String key, Object... args) {
        return StarsectorStrings.fillTemplate(get(category, key), args);
    }

    // The part after the marker, or the whole wording where it carries none.
    private static String readEnglishCopy(String wording) {

        var markerIndex = wording.indexOf(ENGLISH_COPY_MARKER);

        return markerIndex < 0
            ? wording
            : wording.substring(markerIndex + ENGLISH_COPY_MARKER.length());
    }
}
