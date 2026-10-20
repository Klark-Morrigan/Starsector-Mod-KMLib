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
     * Heading of the notice shown when one of a mod's own features threw and was switched off. Two
     * slots: the mod's name, then the phrase below that names the failure.
     *
     * <p>Says the feature was switched off rather than that something broke, so the player knows
     * the game is still safe to play and why a part of it is missing.
     */
    public static final String COMPATIBILITY_NOTICE_FEATURE_TITLE = "compatibility_notice_feature_title";

    /** The phrase that heading names the failure with, which is the run of it that warns. No slots. */
    public static final String COMPATIBILITY_NOTICE_FEATURE_TITLE_ERROR = "compatibility_notice_feature_title_error";

    /**
     * What the player can do about a feature failure. Two slots: the mod, then the
     * report-to-developer phrase below.
     *
     * <p>Says the fault is the mod's own before asking for the report. A player who has met the
     * compatibility notice would otherwise look for a version to change, and there is none to change.
     */
    public static final String COMPATIBILITY_NOTICE_FEATURE_DIAGNOSIS = "compatibility_notice_feature_diagnosis";

    /**
     * What the player can do about it, where the install is behind the build. Three slots: the
     * third party, the too-old phrase below, the update phrase below.
     */
    public static final String COMPATIBILITY_NOTICE_DIAGNOSIS_OLDER_VERSION =
        "compatibility_notice_diagnosis_older_version";

    /**
     * What the player can do about it, where the install is ahead of the build. Five slots: the
     * third party, the carries-changes phrase below, the consuming mod, the depends-on phrase
     * below, the downgrade-or-wait phrase below.
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

    /**
     * What the player can do about it, where the two versions name one release. Two slots: the
     * third party, then the integration-fault phrase below.
     *
     * <p>Says nothing about updating or downgrading, there being nowhere to move to. Everything
     * else in the notice invites the player to compare the two versions, and this is the one shape
     * where that comparison leads nowhere: the install is the release the binding was type-checked
     * against, so the version is the one thing already known to be right.
     *
     * <p>Worded as "reports" rather than "is", which leaves room for a third party whose
     * self-report is wrong without spending a clause claiming it - Fast Rendering has reported
     * {@code v0.8.4} while being {@code v0.8.5rc1}.
     */
    public static final String COMPATIBILITY_NOTICE_DIAGNOSIS_SAME_VERSION =
        "compatibility_notice_diagnosis_same_version";

    /** The phrase naming the install as behind, which is a run that warns. No slots. */
    public static final String COMPATIBILITY_NOTICE_PHRASE_TOO_OLD = "compatibility_notice_phrase_too_old";

    /**
     * The phrase naming the install as ahead where a version was read, which is a run that warns.
     * No slots.
     */
    public static final String COMPATIBILITY_NOTICE_PHRASE_CARRIES_CHANGES =
        "compatibility_notice_phrase_carries_changes";

    /**
     * The same phrase as the second case under the lead, where it opens on the state rather than
     * following the third party's name. No slots.
     *
     * <p>Its own key rather than the one above with the state spliced on, because the two are one
     * run each in their sentences and a translation carries each sentence whole.
     */
    public static final String COMPATIBILITY_NOTICE_PHRASE_NEW_AND_CARRIES_CHANGES =
        "compatibility_notice_phrase_new_and_carries_changes";

    /**
     * The phrase closing that one, after the consuming mod is named. No slots.
     *
     * <p>Its own key because the mod it names is brought forward between the two halves, so the
     * warning around it is two runs rather than one.
     */
    public static final String COMPATIBILITY_NOTICE_PHRASE_DEPENDS_ON = "compatibility_notice_phrase_depends_on";

    /**
     * The phrase a diagnosis states the report with, where the versions match and there is nothing
     * to move to. One slot: the consuming mod.
     *
     * <p>An instruction rather than a verdict, matching the other diagnoses: every one of them
     * ends on something the player can do, and where no version can be changed what is left to do
     * is report it. A version match is exactly what a mirrored member that never existed, an
     * unexercised call path or an unhandled state survives, so the report is worth more here than
     * anywhere else in the notice - and reinstalling the third party is worth nothing, its version
     * being the one thing already known to be right.
     *
     * <p>Names the consuming mod because that is the mod the player installed and the address a
     * report goes to. Where the fault is really in the library underneath, its author depends on
     * the library and forwards it; nothing here can tell the two apart, and the integration row
     * names the binding for whoever can.
     */
    public static final String COMPATIBILITY_NOTICE_PHRASE_REPORT_TO_DEVELOPER =
        "compatibility_notice_phrase_report_to_developer";

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
     * The row naming which of a mod's own features failed. One slot: the key the record latched it
     * under, the mod's ID and its feature key joined.
     */
    public static final String COMPATIBILITY_NOTICE_ROW_FEATURE = "compatibility_notice_row_feature";

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

    /** What a failed read of Nexerelin's alliances costs. No slots. */
    public static final String COMPATIBILITY_LOST_NEXERELIN_ALLIANCES = "compatibility_lost_nexerelin_alliances";

    /** What a failed install of the Nexerelin colony-transfer relay costs. No slots. */
    public static final String COMPATIBILITY_LOST_NEXERELIN_COLONY_TRANSFERS =
        "compatibility_lost_nexerelin_colony_transfers";

    /** What a failed Nexerelin integration costs. No slots. */
    public static final String COMPATIBILITY_LOST_NEXERELIN_ROUTINES = "compatibility_lost_nexerelin_routines";

    /** What a failed Random Assortment of Things integration costs. No slots. */
    public static final String COMPATIBILITY_LOST_RAT_ACCESS_ROUTES = "compatibility_lost_rat_access_routes";

    /**
     * What a failed LunaLib binding does not cost. No slots.
     *
     * <p>Every one of these names the save, because that is the question a player reads a
     * compatibility notice asking themselves. None of them promises anything about another mod's
     * feature: what the library may vouch for is what the library does.
     */
    public static final String COMPATIBILITY_UNAFFECTED_LUNALIB_SETTINGS =
        "compatibility_unaffected_lunalib_settings";

    /** What a failed read of Nexerelin's alliances does not cost. No slots. */
    public static final String COMPATIBILITY_UNAFFECTED_NEXERELIN_ALLIANCES =
        "compatibility_unaffected_nexerelin_alliances";

    /** What a failed install of the Nexerelin colony-transfer relay does not cost. No slots. */
    public static final String COMPATIBILITY_UNAFFECTED_NEXERELIN_COLONY_TRANSFERS =
        "compatibility_unaffected_nexerelin_colony_transfers";

    /** What a failed Nexerelin integration does not cost. No slots. */
    public static final String COMPATIBILITY_UNAFFECTED_NEXERELIN_ROUTINES =
        "compatibility_unaffected_nexerelin_routines";

    /** What a failed Random Assortment of Things integration does not cost. No slots. */
    public static final String COMPATIBILITY_UNAFFECTED_RAT_ACCESS_ROUTES =
        "compatibility_unaffected_rat_access_routes";

    // What KMLib's console commands print, read through kmlib.mods.console.ConsoleStrings rather than the
    // accessors below: a translation of these carries an English copy the console can draw.

    /** A hand-over refused because the faction named already holds the colony. Two slots: colony, faction. */
    public static final String CONSOLE_COLONY_ALREADY_OWNED = "console_colony_already_owned";

    /** A colony founded. Two slots: the colony, then the faction holding it. */
    public static final String CONSOLE_COLONY_FOUNDED = "console_colony_founded";

    /** A colony handed over. Two slots: the colony, then the faction now holding it. */
    public static final String CONSOLE_COLONY_TRANSFERRED = "console_colony_transferred";

    /** The heading of the system entity listing's fleet section. No slots. */
    public static final String CONSOLE_ENTITIES_FLEETS = "console_entities_fleets";

    /** The system entity listing's heading. One slot: the system. */
    public static final String CONSOLE_ENTITIES_HEADING = "console_entities_heading";

    /** The system entity listing's heading when narrowed to gates. One slot: the system. */
    public static final String CONSOLE_ENTITIES_HEADING_GATES_ONLY = "console_entities_heading_gates_only";

    /** An orbiting entity's distance to its focus and its speed. Two slots: distance, degrees per day. */
    public static final String CONSOLE_ENTITIES_ORBIT = "console_entities_orbit";

    /** What an entity with no name is listed as. No slots. */
    public static final String CONSOLE_ENTITIES_UNNAMED = "console_entities_unnamed";

    /** The heading of the system entity listing's section for entities orbiting nothing. No slots. */
    public static final String CONSOLE_ENTITIES_UNORBITED = "console_entities_unorbited";

    /** What a keyword parameter expects when given another word. One slot: the keywords, listed. */
    public static final String CONSOLE_EXPECTED_ONE_OF = "console_expected_one_of";

    /** What the faction listing gives as the player faction's attitude to itself. No slots. */
    public static final String CONSOLE_FACTIONS_ATTITUDE_SELF = "console_factions_attitude_self";

    /** The faction listing's heading noun. No slots. */
    public static final String CONSOLE_FACTIONS_HEADER = "console_factions_header";

    /** The faction listing's heading noun when it lists no holdings. No slots. */
    public static final String CONSOLE_FACTIONS_HEADER_WITHOUT_HOLDINGS = "console_factions_header_without_holdings";

    /** The faction listing's heading line. One slot: the heading noun. */
    public static final String CONSOLE_FACTIONS_HEADING = "console_factions_heading";

    /** The faction listing's heading line under a filter. Two slots: the heading noun, then the filter. */
    public static final String CONSOLE_FACTIONS_HEADING_FILTERED = "console_factions_heading_filtered";

    /** How many places a faction holds. One slot: the count. */
    public static final String CONSOLE_FACTIONS_HOLDINGS = "console_factions_holdings";

    /** The systems a faction's places sit in. One slot: the systems, listed. */
    public static final String CONSOLE_FACTIONS_HOLDINGS_SYSTEMS = "console_factions_holdings_systems";

    /** How many of a faction's places are hidden and discoverable. Two slots: those two counts. */
    public static final String CONSOLE_FACTIONS_HOLDINGS_VISIBILITY = "console_factions_holdings_visibility";

    /** What a place in hyperspace is listed under in place of a system. No slots. */
    public static final String CONSOLE_FACTIONS_HYPERSPACE = "console_factions_hyperspace";

    /** The mod a faction came from. One slot: the mod. */
    public static final String CONSOLE_FACTIONS_SOURCE = "console_factions_source";

    /** The mark beside a faction treating the space around its holdings as its own. No slots. */
    public static final String CONSOLE_FACTIONS_TERRITORIAL = "console_factions_territorial";

    /** What the source of a faction no data file declares is given as. No slots. */
    public static final String CONSOLE_FACTIONS_UNATTRIBUTED = "console_factions_unattributed";

    /** What the console says when the faction listing went to the log. No slots. */
    public static final String CONSOLE_FACTIONS_WRITTEN_TO_LOG = "console_factions_written_to_log";

    /** A gate activated. Two slots: the gate's ID, then the system. */
    public static final String CONSOLE_GATE_ACTIVATED = "console_gate_activated";

    /** No gate under the ID asked for. Two slots: the ID, then the system. */
    public static final String CONSOLE_GATE_NOT_FOUND = "console_gate_not_found";

    /** What a listing with nothing in it says. No slots. */
    public static final String CONSOLE_LISTING_NONE = "console_listing_none";

    /** A flag given a value. One slot: the flag. */
    public static final String CONSOLE_PARAMETER_FLAG_GIVEN_VALUE = "console_parameter_flag_given_value";

    /** A value its parameter refuses. Three slots: the parameter, the value, what it expects. */
    public static final String CONSOLE_PARAMETER_INVALID_VALUE = "console_parameter_invalid_value";

    /** A required parameter left out. One slot: the parameter. */
    public static final String CONSOLE_PARAMETER_MISSING = "console_parameter_missing";

    /** A standalone parameter given with others. Two slots: the parameter, then the usage line. */
    public static final String CONSOLE_PARAMETER_STANDALONE_COMBINED = "console_parameter_standalone_combined";

    /** More positional values than slots. One slot: the usage line. */
    public static final String CONSOLE_PARAMETER_TOO_MANY = "console_parameter_too_many";

    /** A parameter name the command does not take. Two slots: the name, then the names it does take. */
    public static final String CONSOLE_PARAMETER_UNKNOWN = "console_parameter_unknown";

    /**
     * The usage line. One slot: the command's forms, whose grammar stays English in every locale because
     * the command table's syntax column restates it and a player types it.
     */
    public static final String CONSOLE_PARAMETER_USAGE = "console_parameter_usage";

    /** What the profiling command's row count expects. No slots. */
    public static final String CONSOLE_PROFILING_EXPECTED_TOP = "console_profiling_expected_top";

    /** The profiling timings written to the log. One slot: whose timings, as the command labels them. */
    public static final String CONSOLE_PROFILING_TIMINGS_LOGGED = "console_profiling_timings_logged";

    /** The profiling timings cleared. One slot: whose timings, as the command labels them. */
    public static final String CONSOLE_PROFILING_TIMINGS_RESET = "console_profiling_timings_reset";

    /** A command run outside a campaign. No slots. */
    public static final String CONSOLE_REQUIRES_CAMPAIGN = "console_requires_campaign";

    /** A command run outside a star system. No slots. */
    public static final String CONSOLE_REQUIRES_STAR_SYSTEM = "console_requires_star_system";

    /** A spawn with no focus in a system of several stars. Two slots: how many stars, their IDs listed. */
    public static final String CONSOLE_SPAWN_AMBIGUOUS_FOCUS = "console_spawn_ambiguous_focus";

    /** What the spawn command's jitter expects. No slots. */
    public static final String CONSOLE_SPAWN_EXPECTED_JITTER = "console_spawn_expected_jitter";

    /** What the spawn command's speed expects. No slots. */
    public static final String CONSOLE_SPAWN_EXPECTED_SPEED = "console_spawn_expected_speed";

    /** No entity under the focus ID asked for. Two slots: the ID, then the system. */
    public static final String CONSOLE_SPAWN_FOCUS_NOT_FOUND = "console_spawn_focus_not_found";

    /** A gate spawned. One slot: its ID. */
    public static final String CONSOLE_SPAWN_GATE_ADDED = "console_spawn_gate_added";

    /** A jump point spawned. Two slots: its name, then the system. */
    public static final String CONSOLE_SPAWN_JUMP_POINT_ADDED = "console_spawn_jump_point_added";

    /** A kind the spawn command does not know. Two slots: the word given, then the kinds it knows. */
    public static final String CONSOLE_SPAWN_UNKNOWN_KIND = "console_spawn_unknown_kind";

    /** The mark beside a system no jump point leads into. No slots. */
    public static final String CONSOLE_SPOILERS_CUT_OFF = "console_spoilers_cut_off";

    /** The map spoiler listing's heading. No slots. */
    public static final String CONSOLE_SPOILERS_HEADING = "console_spoilers_heading";

    /** The mark beside a market the player has not found. No slots. */
    public static final String CONSOLE_SPOILERS_UNDISCOVERED = "console_spoilers_undiscovered";

    /** No entity under the ID asked for. One slot: the ID. */
    public static final String CONSOLE_TARGET_ENTITY_NOT_FOUND = "console_target_entity_not_found";

    /** An entity asked for that holds no market. One slot: the ID. */
    public static final String CONSOLE_TARGET_ENTITY_WITHOUT_MARKET = "console_target_entity_without_market";

    /** No faction under the ID asked for. One slot: the ID. */
    public static final String CONSOLE_TARGET_FACTION_NOT_FOUND = "console_target_faction_not_found";

    /** A market asked for that is not the kind the command acts on. Two slots: the ID, the requirement. */
    public static final String CONSOLE_TARGET_MARKET_INELIGIBLE = "console_target_market_ineligible";

    /** No market of the kind asked for nearby. Two slots: the system, then the requirement. */
    public static final String CONSOLE_TARGET_NO_ELIGIBLE_MARKET = "console_target_no_eligible_market";

    /** No sector to find a faction in. No slots. */
    public static final String CONSOLE_TARGET_NO_SECTOR_FOR_FACTIONS = "console_target_no_sector_for_factions";

    /** No sector to find a market in. No slots. */
    public static final String CONSOLE_TARGET_NO_SECTOR_FOR_MARKETS = "console_target_no_sector_for_markets";

    /** A search for the nearest market from outside a star system. No slots. */
    public static final String CONSOLE_TARGET_NO_STAR_SYSTEM = "console_target_no_star_system";

    /**
     * What colonising needs of a market. No slots. Worded to complete both refusals that name a requirement,
     * so it carries its own article.
     */
    public static final String CONSOLE_TARGET_REQUIREMENT_COLONISABLE_BODY =
        "console_target_requirement_colonisable_body";

    /** What handing a colony over needs of a market. No slots. Worded as the one above is. */
    public static final String CONSOLE_TARGET_REQUIREMENT_EXISTING_COLONY =
        "console_target_requirement_existing_colony";

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
