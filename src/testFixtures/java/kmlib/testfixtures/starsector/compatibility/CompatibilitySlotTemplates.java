package kmlib.testfixtures.starsector.compatibility;

import kmlib.testfixtures.starsector.settings.StarsectorSettingsFake;

import java.util.Map;

/**
 * The compatibility notice's templates as stand-ins that expose their slots, installed into the
 * game's settings in place of the shipped wording.
 *
 * <p>Each template names its key and lists its slots in order, so what a failure composes reads
 * as which value landed in which slot without any wording being known - and two failures that
 * differ in one slot compose two different modals. A copy of the shipped wording would agree with
 * the code however the shipped file is edited; the shipped wording is a matter for the suite that
 * reads the file.
 *
 * <p>Final class with a private constructor: fixture of static wiring, no instances.
 */
public final class CompatibilitySlotTemplates {

    private static final Map<String, String> TEMPLATES_BY_KEY = Map.of(
        "compatibility_notice_title", "title[%s]",
        "compatibility_notice_built_against", "built[%s|%s|%s]",
        "compatibility_notice_built_against_unreadable", "unreadable[%s|%s]",
        "compatibility_notice_consequence", "consequence[%s]",
        "compatibility_notice_version_unknown", "?");

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
}
