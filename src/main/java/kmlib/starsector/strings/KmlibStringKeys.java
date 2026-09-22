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
 *
 * <p>The compatibility notice's emphasis is carried by its slots rather than by any markup. Every
 * run that stands out is a value filled into a template - a name, a version, or one of the phrases
 * below - so the composition knows each one and nothing parses the wording. A phrase meant to stand
 * out therefore has a key of its own.
 */
public final class KmlibStringKeys {

    /** strings.json category namespacing every KMLib-owned string. */
    public static final String CATEGORY = "kmlib";

    /**
     * Heading of the notice shown when a binding to third-party code stops holding. Three slots:
     * the phrase below that names the failure, the consuming mod's name, then the third party's.
     *
     * <p>Never the word "mod" for the subject - some subjects, Fast Rendering among them, are install
     * patches with no folder under {@code mods\} and no {@code mod_info.json}, so calling one a mod
     * sends the player to a mod manager that does not list it. The bare name reads correctly either
     * way.
     */
    public static final String COMPATIBILITY_NOTICE_TITLE = "compatibility_notice_title";

    /**
     * The phrase the heading names the failure with, which is the run of it that warns. No slots.
     *
     * <p>Its own key rather than the opening words of the heading, because it is what stands out
     * and the composition has to hand it to a surface as a run of its own.
     */
    public static final String COMPATIBILITY_NOTICE_TITLE_ERROR = "compatibility_notice_title_error";

    /**
     * What the player can do about it, where the install is behind the build. Two slots: the third
     * party, the update phrase below.
     */
    public static final String COMPATIBILITY_NOTICE_DIAGNOSIS_OLDER_VERSION =
        "compatibility_notice_diagnosis_older_version";

    /**
     * What the player can do about it, where the install is ahead of the build. Three slots: the
     * third party, the consuming mod, the downgrade-or-wait phrase below.
     */
    public static final String COMPATIBILITY_NOTICE_DIAGNOSIS_NEWER_VERSION =
        "compatibility_notice_diagnosis_newer_version";

    /**
     * The line opening the diagnosis where the installed version could not be read. One slot: the
     * third party.
     *
     * <p>Three lines rather than one sentence, because with one version unread both directions are
     * live at once and a sentence carrying both reads as a single tangled claim. A lead and two
     * cases under it say the same thing as a choice the player can scan.
     */
    public static final String COMPATIBILITY_NOTICE_DIAGNOSIS_UNKNOWN_VERSION =
        "compatibility_notice_diagnosis_unknown_version";

    /**
     * The first of the two cases under that lead. Two slots: the too-old phrase below, then the
     * update phrase below.
     */
    public static final String COMPATIBILITY_NOTICE_DIAGNOSIS_UNKNOWN_OLDER =
        "compatibility_notice_diagnosis_unknown_older";

    /**
     * The second of the two cases under that lead. Four slots: the carries-changes phrase below,
     * the consuming mod, the depends-on phrase below, then the downgrade-or-wait phrase below.
     */
    public static final String COMPATIBILITY_NOTICE_DIAGNOSIS_UNKNOWN_NEWER =
        "compatibility_notice_diagnosis_unknown_newer";

    /** The phrase naming the install as behind, which is a run that warns. No slots. */
    public static final String COMPATIBILITY_NOTICE_PHRASE_TOO_OLD = "compatibility_notice_phrase_too_old";

    /** The phrase naming the install as ahead, which is a run that warns. No slots. */
    public static final String COMPATIBILITY_NOTICE_PHRASE_CARRIES_CHANGES =
        "compatibility_notice_phrase_carries_changes";

    /**
     * The phrase closing that one, after the consuming mod is named. No slots.
     *
     * <p>Its own key because the mod it names is brought forward between the two halves, so the
     * warning around it is two runs rather than one.
     */
    public static final String COMPATIBILITY_NOTICE_PHRASE_DEPENDS_ON = "compatibility_notice_phrase_depends_on";

    /**
     * The phrase a diagnosis states the update with, which is the run of it that warns. One slot:
     * the targeted version.
     *
     * <p>The version is inside the warned run rather than brought forward beside it, so the whole
     * instruction reads as one thing the player has to act on.
     */
    public static final String COMPATIBILITY_NOTICE_ACTION_UPDATE = "compatibility_notice_action_update";

    /**
     * The phrase a diagnosis states the choice with, which is the run of it that warns. Three
     * slots: the third party, the targeted version, the consuming mod.
     *
     * <p>One clause rather than the words around its values, so that what warns is a phrase a
     * translation can carry whole. Its values are inside the warned run and are not brought forward
     * again: the run stands out as one thing.
     */
    public static final String COMPATIBILITY_NOTICE_ACTION_DOWNGRADE_OR_WAIT =
        "compatibility_notice_action_downgrade_or_wait";

    /**
     * The row naming the mod that lost something. One slot: the mod as the report words it - its
     * own name beside its ID and version where the game holds them, or the ID alone where it does
     * not.
     *
     * <p>A row of the body and never part of the heading's emphasis. The heading names both parties;
     * this row is where the consuming mod is pinned down to a version.
     */
    public static final String COMPATIBILITY_NOTICE_ROW_MOD = "compatibility_notice_row_mod";

    /**
     * The row naming which integration of the consuming mod broke. One slot: the key the record
     * latched it under, the mod's ID and its feature key joined.
     */
    public static final String COMPATIBILITY_NOTICE_ROW_INTEGRATION = "compatibility_notice_row_integration";

    /**
     * The row naming what this build was type-checked against. One slot: that version.
     *
     * <p>Naming both versions is what turns "version mismatch" into an actionable sentence. The
     * subject is not named again here: the heading above already says whose versions these are. A
     * self-reported {@code v} prefix is stripped before the slot is filled, the label already saying
     * that a version is what follows.
     */
    public static final String COMPATIBILITY_NOTICE_ROW_TARGETED = "compatibility_notice_row_targeted";

    /** The row naming the installed version. One slot: that version. */
    public static final String COMPATIBILITY_NOTICE_ROW_DETECTED = "compatibility_notice_row_detected";

    /**
     * The row naming what no longer holds. One slot: the member or detail, as whichever guard
     * caught it words it - every mirrored member a probe found broken, or what a step threw.
     */
    public static final String COMPATIBILITY_NOTICE_ROW_BROKEN = "compatibility_notice_row_broken";

    /**
     * The row naming which guard caught the binding. One slot: the phrase that guard files under.
     */
    public static final String COMPATIBILITY_NOTICE_ROW_FAILED_WHILE = "compatibility_notice_row_failed_while";

    /**
     * The row naming what the player loses. One slot: the consumer's own sentence, warned with.
     *
     * <p>That sentence is the caller's to supply, out of its own strings, and is the only part of the
     * notice that is: what a mismatch costs is knowledge of the feature that broke, which lives with
     * whoever built it. Written here it would name a consumer's feature and would have to be
     * rewritten for the second client of the channel.
     */
    public static final String COMPATIBILITY_NOTICE_ROW_EFFECT = "compatibility_notice_row_effect";

    /**
     * The row naming what goes on working. One slot: the consumer's own sentence for it, set at
     * ease rather than warned with - it is the one row that is good news.
     *
     * <p>Left out of the notice entirely where the consumer supplied none. The sentence is the
     * consumer's for the same reason the lost one is, and more so: the library cannot promise
     * anything about another mod's feature or another mod's save, so a reassurance written here
     * would be the library vouching for a mod it knows nothing about.
     */
    public static final String COMPATIBILITY_NOTICE_ROW_NO_EFFECT = "compatibility_notice_row_no_effect";

    /**
     * The sentence closing the notice, pointing at the log. One slot: the log's own file name,
     * which is not wording and so is not translated.
     *
     * <p>Its own string because it is the generic half: every subject's notice carries it and no
     * subject's wording changes it. Kept honest by the reporters, which write the log block before
     * they ask for anything on screen - so a player sent to the log always finds something there.
     */
    public static final String COMPATIBILITY_NOTICE_SEE_LOG = "compatibility_notice_see_log";

    /**
     * Stands in for a version slot nothing could fill, so a report never prints {@code null} at a
     * player. One word, because it sits in a value slot beside a label that already says a version
     * belongs there, and both version rows take it.
     */
    public static final String COMPATIBILITY_NOTICE_VERSION_UNKNOWN = "compatibility_notice_version_unknown";

    /**
     * The label on the one button the notice carries. No slots.
     *
     * <p>The word is the player's, so it ships here rather than sitting as a literal beside the
     * widget.
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
     * compatibility notice asking themselves. None of them promises anything about another mod's
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
