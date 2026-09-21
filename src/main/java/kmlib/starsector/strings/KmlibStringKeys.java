package kmlib.starsector.strings;

/**
 * KMLib's own localisation entry point. Holds the category KMLib registers in
 * {@code data/strings/strings.json}, the IDs of the strings inside it, and thin {@link #get(String)}
 * / {@link #format(String, Object...)} accessors that bind that category so call sites never repeat
 * it.
 *
 * <p>{@link StarsectorStrings} is intentionally mod-agnostic and takes {@code (category, key)} on
 * every call; this holder is the KMLib-specific other half it documents - a category-bound
 * shortcut, not a parallel implementation. The keys live in exactly one place and never reappear as
 * loose literals that can drift from the JSON.
 */
public final class KmlibStringKeys {

    /** strings.json category namespacing every KMLib-owned string. */
    public static final String CATEGORY = "kmlib";

    /**
     * Heading of the modal shown when a binding to third-party code stops holding. One slot: the
     * third party's name.
     *
     * <p>Never the word "mod" for the subject - some subjects, Fast Rendering among them, are install
     * patches with no folder under {@code mods\} and no {@code mod_info.json}, so calling one a mod
     * sends the player to a mod manager that does not list it. The bare name reads correctly either
     * way.
     */
    public static final String COMPATIBILITY_NOTICE_TITLE = "compatibility_notice_title";

    /**
     * The sentence closing the heading paragraph, after the title. No slots.
     *
     * <p>Its own string rather than the tail of the title, because it is the generic half: every
     * subject's notice carries it and no subject's wording changes it. Kept honest by
     * {@code CompatibilityNotice}, which writes the log block before it asks for the dialog and
     * whatever the dialog then does - so a player sent to the log always finds something there.
     */
    public static final String COMPATIBILITY_NOTICE_SEE_LOG = "compatibility_notice_see_log";

    /**
     * The row naming the mod that lost something. One slot: the mod as
     * {@code CompatibilityConsumer.describeMod()} words it - its own name beside its ID, or the ID
     * alone where the game lists no such mod.
     *
     * <p>A row of the body and never part of the heading. The heading names the third party that
     * stopped holding, and a heading carrying the consuming mod's name too would put the player's
     * eye on the mod that is working correctly.
     */
    public static final String COMPATIBILITY_NOTICE_ROW_MOD = "compatibility_notice_row_mod";

    /**
     * The row naming what this build was type-checked against. One slot: that version.
     *
     * <p>Naming both versions is what turns "version mismatch" into an actionable sentence, and it
     * removes any need to classify the failure as too-new or too-old - the two numbers say which it
     * is. The subject is not named again here: the heading above already says whose versions these
     * are, and a row repeating it reads as a second subject. A self-reported {@code v} prefix is
     * stripped before the slot is filled, the label already saying that a version is what follows.
     */
    public static final String COMPATIBILITY_NOTICE_ROW_BUILT_FOR = "compatibility_notice_row_built_for";

    /** The row naming the installed version. One slot: that version. */
    public static final String COMPATIBILITY_NOTICE_ROW_INSTALLED = "compatibility_notice_row_installed";

    /**
     * The row naming what the player loses. One slot: the consumer's own sentence.
     *
     * <p>That sentence is the caller's to supply, out of its own strings, and is the only part of the
     * notice that is: what a mismatch costs is knowledge of the feature that broke, which lives with
     * whoever built it. Written here it would name a consumer's feature and would have to be
     * rewritten for the second client of the channel.
     */
    public static final String COMPATIBILITY_NOTICE_ROW_EFFECT = "compatibility_notice_row_effect";

    /**
     * The row naming what goes on working. One slot: the consumer's own sentence for it.
     *
     * <p>Left out of the notice entirely where the consumer supplied none. The sentence is the
     * consumer's for the same reason the lost one is, and more so: the library cannot promise
     * anything about another mod's feature or another mod's save, so a reassurance written here
     * would be the library vouching for a mod it knows nothing about.
     */
    public static final String COMPATIBILITY_NOTICE_ROW_NO_EFFECT = "compatibility_notice_row_no_effect";

    /**
     * Stands in for a version slot nothing could fill, so a report never prints {@code null} at a
     * player. One word, because it sits in a value slot beside a label that already says a version
     * belongs there, and both version rows take it - a row that explained instead of answering
     * would be the longest line in a block whose panel is a fixed size.
     *
     * <p>Reading a subject's version is itself a third-party binding and can fail on its own, so
     * the report has to hold without either number. Which direction a mismatch runs in is not
     * guessed at here: where both versions are known the numbers say it, and where one is not,
     * nothing said in this slot would be true of both cases.
     */
    public static final String COMPATIBILITY_NOTICE_VERSION_UNKNOWN = "compatibility_notice_version_unknown";

    /**
     * The label on the one button the notice's dialog carries. No slots.
     *
     * <p>A confirm dialog with no cancel label renders a single button, which is how the game's own
     * one-button notices are put up. The word is the player's, so it ships here rather than sitting
     * as a literal beside the call.
     */
    public static final String COMPATIBILITY_NOTICE_CONFIRM_BUTTON = "compatibility_notice_confirm_button";

    /**
     * What the player loses where the library's own LunaLib bindings did not install. No slots.
     *
     * <p>The library's own sentence, in the slot a consuming mod fills with its. Where one of
     * KMLib's start-up steps binds to a third-party mod, the library is the mod that lost something
     * by that binding, so it writes its consequence out of its own strings exactly as a consuming
     * mod does - which is also what shows the channel takes more than one client.
     */
    public static final String COMPATIBILITY_LOST_LUNALIB_SETTINGS = "compatibility_lost_lunalib_settings";

    /** What a failed Nexerelin integration costs. No slots. */
    public static final String COMPATIBILITY_LOST_NEXERELIN_ROUTINES = "compatibility_lost_nexerelin_routines";

    /** What a failed Random Assortment of Things integration costs. No slots. */
    public static final String COMPATIBILITY_LOST_RAT_ACCESS_ROUTES = "compatibility_lost_rat_access_routes";

    /**
     * What a failed LunaLib binding does not cost. No slots.
     *
     * <p>Every one of these three names the save, because that is the question a player reads a
     * compatibility modal asking themselves. None of them promises anything about another mod's
     * feature: what the library may vouch for is what the library does.
     */
    public static final String COMPATIBILITY_UNAFFECTED_LUNALIB_SETTINGS =
        "compatibility_unaffected_lunalib_settings";

    /** What a failed Nexerelin integration does not cost. No slots. */
    public static final String COMPATIBILITY_UNAFFECTED_NEXERELIN_ROUTINES =
        "compatibility_unaffected_nexerelin_routines";

    /** What a failed Random Assortment of Things integration does not cost. No slots. */
    public static final String COMPATIBILITY_UNAFFECTED_RAT_ACCESS_ROUTES =
        "compatibility_unaffected_rat_access_routes";

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
