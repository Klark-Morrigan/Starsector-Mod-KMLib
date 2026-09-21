package kmlib.starsector.compatibility;

import kmlib.starsector.settings.modmanager.InstalledMods;
import kmlib.text.KmlibStrings;

import java.util.Objects;

/**
 * A third-party mod that a start-up step binds to, and what the mod running that step loses where
 * the binding does not take.
 *
 * <p>The pairing is the whole of it. A report needs the third party it is about and the mod that
 * lost something by it, and a guard holding one without the other would file half a report. Its own
 * value rather than a run of arguments beside the step, because a guard call naming three loose
 * strings is three a caller can transpose with nothing to catch it - and because the pair is read
 * only on a path that almost never runs, so it travels as one thing a caller composes there.
 *
 * <p>A mismatch is stated as two versions and a mod integration has one. Nothing was type-checked
 * against an optional mod - the binding goes through the game's own API rather than a compile - so
 * the built-for row stands at its unknown wording, while the installed version is published and is
 * read when a report is composed.
 *
 * @param subjectModId   the third party's own mod ID, as its {@code mod_info.json} declares it: the
 *                       key a record latches the third party under, and what the installed version
 *                       is read by. The game holds one spec per ID, so no two third parties can
 *                       collide on it
 * @param subjectModName the third party as a report names it, held rather than looked up: the name
 *                       leads the modal's heading, and a heading falling back to a bare ID because
 *                       the mod manager could not answer would read as a mod nobody has heard of
 * @param consumer       the mod whose step took the binding, whose key completes the latch and
 *                       whose sentences say what the failure costs and what it does not
 */
public record ModIntegration(
    String subjectModId,
    String subjectModName,
    CompatibilityConsumer consumer) {

    public ModIntegration {

        KmlibStrings.requireText(
            subjectModId,
            "An integration with no mod ID could not say which third party a failure is about.");
        KmlibStrings.requireText(
            subjectModName,
            "An integration with no mod name would head its report with a blank.");
        Objects.requireNonNull(
            consumer,
            "An integration with no consumer could not say whose feature the binding cost.");
    }

    /**
     * Builds the failure a report is drawn from, for a step that bound to this mod and did not
     * install.
     *
     * <p>Composed on the failure path and nowhere else: the version read below goes through the
     * game's mod manager, which is a question worth asking once about a step that broke and never
     * about the ones that worked.
     *
     * @param failureSite         where the binding stopped holding, in the wording of whichever
     *                            guard caught it - the guard being the only thing that knows which
     *                            one it was
     * @param installationFailure what the step threw, carried as the failure's cause so the log
     *                            line beside the block renders a trace of it
     * @return the failure to record, with the consumer's sentences already in it
     */
    public CompatibilityFailure composeFailure(String failureSite, Throwable installationFailure) {

        Objects.requireNonNull(
            installationFailure,
            "A failure composed from nothing thrown would report an installation that worked.");

        // The built-for slot stands at its unknown wording: there is no release an optional-mod
        // binding was checked against, so there is nothing a build could have stamped for it.
        return new CompatibilityFailure(
            new CompatibilitySubject(subjectModName, null, InstalledMods.readModVersion(subjectModId)),
            consumer,
            new CompatibilityBreakage(failureSite, describeThrown(installationFailure)),
            installationFailure);
    }

    // What no longer holds, where nothing probed it. A failed installation has no member to point
    // at - the step called into the mod and the mod refused - so what was thrown is the whole of the
    // finding: its type, and whatever the third party said about it.
    private static String describeThrown(Throwable installationFailure) {

        var described = installationFailure.toString();

        // A throwable whose own description answers nothing would fail the breakage's non-blank
        // check while a report about a failure was being composed, so the type name stands in.
        return KmlibStrings.hasText(described)
            ? described
            : installationFailure.getClass().getName();
    }
}
