package kmlib.starsector.strings;

/**
 * KMLib's own localisation entry point. Holds the category KMLib registers in
 * {@code data/strings/strings.json}, the IDs of the strings inside it, and thin
 * {@link #get(String)} / {@link #format(String, Object...)} accessors that bind
 * that category so call sites never repeat it.
 *
 * <p>{@link StarsectorStrings} is intentionally mod-agnostic and takes
 * {@code (category, key)} on every call; this holder is the KMLib-specific other
 * half it documents - a category-bound shortcut, not a parallel implementation.
 * The keys live in exactly one place and never reappear as loose literals that
 * can drift from the JSON.
 */
public final class KmlibStringKeys {

    /** strings.json category namespacing every KMLib-owned string. */
    public static final String CATEGORY = "kmlib";

    /**
     * Heading of the modal shown when a binding to third-party code stops
     * holding. One slot: the third party's name.
     *
     * <p>Never the word "mod" for the subject - some subjects, Fast Rendering
     * among them, are install patches with no folder under {@code mods\} and no
     * {@code mod_info.json}, so calling one a mod sends the player to a mod
     * manager that does not list it. The bare name reads correctly either way.
     */
    public static final String COMPATIBILITY_NOTICE_TITLE = "compatibility_notice_title";

    /**
     * Names both versions when the installed one could be read. Three slots: the
     * subject's name, the version KMLib was built against, and the version
     * installed.
     *
     * <p>Naming both is what turns "version mismatch" into an actionable
     * sentence, and it removes any need to classify the failure as too-new or
     * too-old - the two numbers say which it is. Versions carry their own
     * {@code v} prefix, as the subjects self-report them, so the template adds
     * none.
     */
    public static final String COMPATIBILITY_NOTICE_BUILT_AGAINST = "compatibility_notice_built_against";

    /**
     * The same sentence for an installed version that could not be read. Two
     * slots: the subject's name and the version KMLib was built against.
     *
     * <p>Reading a subject's version is itself a third-party binding and can
     * fail on its own, so the wording has to hold without the second number.
     * The either/or clause covers both directions the mismatch can run in,
     * which is why too-new and too-old need no separate handling.
     */
    public static final String COMPATIBILITY_NOTICE_BUILT_AGAINST_UNREADABLE =
        "compatibility_notice_built_against_unreadable";

    /**
     * What the player loses and what they do not. One slot: the sentence naming
     * the lost feature.
     *
     * <p>That sentence is the caller's to supply, out of its own strings, and is
     * the only part of the notice that is: what a mismatch costs is knowledge of
     * the feature that broke, which lives with whoever built it. Written here it
     * would name a consumer's feature and would have to be rewritten for the
     * second client of the channel.
     *
     * <p>The reassurance around it is the point of the paragraph. A modal about a
     * version mismatch reads as "your save is in danger" unless it says
     * otherwise, and the degraded state costs a feature for the session and
     * nothing else.
     */
    public static final String COMPATIBILITY_NOTICE_CONSEQUENCE = "compatibility_notice_consequence";

    /**
     * Stands in for a version slot nothing could fill, so a report never prints
     * {@code null} at a player. Reads as a parenthetical after the subject's
     * name, which keeps the surrounding sentence grammatical whichever slot is
     * missing.
     */
    public static final String COMPATIBILITY_NOTICE_VERSION_UNKNOWN = "compatibility_notice_version_unknown";

    /** Display word for a jump point, e.g. "&lt;focus&gt; Jump-point &lt;radius&gt;". */
    public static final String JUMP_POINT_LABEL = "jump_point_label";

    private KmlibStringKeys() {
    }

    /**
     * Looks up {@code key} under KMLib's category.
     *
     * @param key the string ID inside the category
     * @return the string, with the fallback {@link StarsectorStrings#get(String, String)} applies
     */
    public static String get(String key) {
        return StarsectorStrings.get(CATEGORY, key);
    }

    /**
     * Formats {@code key}'s template against {@code args}.
     *
     * @param key  the string ID inside the category
     * @param args the values for the template's slots, in order
     * @return the filled template, with the fallback
     *         {@link StarsectorStrings#format(String, String, Object...)} applies
     */
    public static String format(String key, Object... args) {
        return StarsectorStrings.format(CATEGORY, key, args);
    }
}
