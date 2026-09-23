package kmlib.testfixtures.starsector.compatibility;

import kmlib.testfixtures.starsector.settings.StarsectorSettingsFake;

import java.util.Map;

/**
 * The compatibility notice's templates as stand-ins that expose their slots, installed into the
 * game's settings in place of the shipped wording.
 *
 * <p>Each template names its key and lists its slots in order, so what a failure composes reads
 * as which value landed in which slot without any wording being known - and two failures that
 * differ in one slot compose two different notices. A copy of the shipped wording would agree with
 * the code however the shipped file is edited; the shipped wording is a matter for the suite that
 * reads the file.
 *
 * <p>The choice and report phrases are worded rather than bracketed, because they are the templates
 * split into runs: what a case reads there is which words fell either side of a value, which braces
 * would make unreadable.
 *
 * <p>Final class with a private constructor: fixture of static wiring, no instances.
 */
public final class CompatibilitySlotTemplates {

    private static final Map<String, String> TEMPLATES_BY_KEY = Map.ofEntries(
        Map.entry("compatibility_notice_title", "title{%s|%s|%s}"),
        Map.entry("compatibility_notice_title_error", "failed"),
        Map.entry("compatibility_notice_diagnosis_older_version", "diagnose-older{%s|%s|%s}"),
        Map.entry("compatibility_notice_diagnosis_newer_version", "diagnose-newer{%s|%s|%s|%s|%s}"),
        Map.entry("compatibility_notice_diagnosis_unknown_version", "diagnose-unknown{%s}"),
        Map.entry("compatibility_notice_diagnosis_unknown_older", "case-older{%s|%s}"),
        Map.entry("compatibility_notice_diagnosis_unknown_newer", "case-newer{%s|%s|%s|%s}"),
        Map.entry("compatibility_notice_diagnosis_same_version", "diagnose-same{%s|%s}"),
        Map.entry("compatibility_notice_phrase_too_old", "is-old"),
        Map.entry("compatibility_notice_phrase_carries_changes", "is-changed"),
        Map.entry("compatibility_notice_phrase_new_and_carries_changes", "is-new"),
        Map.entry("compatibility_notice_phrase_depends_on", "needs-it"),
        Map.entry("compatibility_notice_phrase_report_to_developer", "report-to %s dev"),
        Map.entry("compatibility_notice_action_update", "do-update{%s}"),
        Map.entry("compatibility_notice_action_downgrade_or_wait", "down %s to %s or-wait %s up"),
        Map.entry("compatibility_notice_row_mod", "mod{%s}"),
        Map.entry("compatibility_notice_row_integration", "integration{%s}"),
        Map.entry("compatibility_notice_row_targeted", "targeted{%s}"),
        Map.entry("compatibility_notice_row_detected", "detected{%s}"),
        Map.entry("compatibility_notice_row_broken", "broken{%s}"),
        Map.entry("compatibility_notice_row_failed_while", "failedwhile{%s}"),
        Map.entry("compatibility_notice_row_effect", "effect{%s}"),
        Map.entry("compatibility_notice_row_no_effect", "noeffect{%s}"),
        Map.entry("compatibility_notice_see_log", "seelog{%s}"),
        Map.entry("compatibility_notice_version_unknown", "?"),
        Map.entry("compatibility_notice_confirm_button", "ok{}"));

    private CompatibilitySlotTemplates() {
        // fixture of static wiring, no instances.
    }

    /**
     * Installs the slot templates as the settings' answer for their keys, and nothing for any
     * other key; the caller clears the settings when its case is done.
     */
    public static void installSlotTemplates() {

        StarsectorSettingsFake.installSettings((category, key) -> TEMPLATES_BY_KEY.get(key));
    }

    /**
     * The templates themselves, for a case that has to install something else beside them - a
     * subject reading a colour as well as its wording cannot take the one-call install above,
     * which answers wording alone.
     *
     * @return key to template, unknown keys absent
     */
    public static Map<String, String> readSlotTemplates() {

        return TEMPLATES_BY_KEY;
    }
}
