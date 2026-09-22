package kmlib.starsector.compatibility;

import kmlib.text.KmlibStrings;

import java.util.List;
import java.util.Objects;

/**
 * One line of the notice a player reads: the wording, and the runs of it that carry emphasis.
 *
 * <p>Emphasis is named rather than marked up. Every run that stands out is a value the composition
 * substituted into the wording - a mod's name, a version, or a phrase out of the strings file - so
 * the composition already holds each one and simply says which they were. Nothing parses the
 * wording, and nothing in a value can be mistaken for a directive.
 *
 * <p><b>The runs are in reading order, and must stay that way.</b> The engine matches each run from
 * where the last one ended, so an out-of-order run either matches a later occurrence than it meant
 * or nothing at all. That same rule is what lets a run repeat: a name highlighted early and then
 * appearing again inside a later phrase is matched once each, in turn.
 *
 * <p>What colour a kind of emphasis takes is not decided here. A surface that can tint reads the
 * runs and maps them; one that takes only a string reads {@link #lineText()} and drops them.
 *
 * @param lineText        the wording with its values already in place
 * @param emphasisedRuns  the runs that stand out, in the order they appear in {@code lineText}
 */
public record CompatibilityNoticeLine(
    String lineText,
    List<EmphasisedRun> emphasisedRuns) {

    public CompatibilityNoticeLine {

        KmlibStrings.requireText(
            lineText,
            "A notice line with no wording would take a player's eye and tell them nothing.");
        Objects.requireNonNull(
            emphasisedRuns,
            "A notice line with no run list could not say which of it stands out.");

        emphasisedRuns = List.copyOf(emphasisedRuns);
    }

    /**
     * One run of a line that stands out from the rest of it.
     *
     * @param runText  the run itself, exactly as it appears in the line
     * @param emphasis what kind of emphasis it carries
     */
    public record EmphasisedRun(
        String runText,
        Emphasis emphasis) {

        public EmphasisedRun {

            KmlibStrings.requireText(
                runText,
                "A run with no text could not be found in the line it is meant to stand out from.");
            Objects.requireNonNull(
                emphasis,
                "A run with no kind of emphasis could not be told from the wording around it.");
        }
    }

    /** What a run of a notice line stands out as. */
    public enum Emphasis {

        /** Brought forward: a name, a version, or the place to look. */
        HIGHLIGHT,

        /** Warned with: what went wrong, or what the player has to do about it. */
        WARNING
    }
}
