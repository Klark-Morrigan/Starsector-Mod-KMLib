package kmlib.testfixtures.starsector.compatibility;

import kmlib.opengl.FastRenderingBridgeDiagnostic;
import kmlib.starsector.compatibility.CompatibilityConsumer;
import kmlib.starsector.compatibility.CompatibilityFailure;
import kmlib.starsector.compatibility.CompatibilitySubject;

/**
 * One representative compatibility failure, with a builder per slot a case varies.
 *
 * <p>A suite about the record, the notice or the wording needs a failure to hand over and cares
 * about one of its slots at most: which versions it names, which detail broke, or what it costs.
 * Each builder varies that one slot and fills the rest from the constants here, so a case reads
 * as being about the slot it names and nothing else.
 *
 * <p>Final class with a private constructor: fixture of static builders, no instances.
 */
public final class CompatibilityFailureFixture {

    /** The third party every failure here is about. */
    public static final String SUBJECT_NAME = "Fast Rendering";

    /** The key a suite latches that third party under on a record. */
    public static final String FAST_RENDERING_SUBJECT_KEY = "fast-rendering";

    /** A second subject's key, for a case about two subjects recorded apart. */
    public static final String NEXERELIN_SUBJECT_KEY = "nexerelin";

    /** The sentence naming what the session loses, in the wording a consumer would supply. */
    public static final String LOST_FEATURE = "Sector map overlays will not respond to the cursor this session.";

    /** The member that stopped holding, as a probe of the bridge reports one. */
    public static final FastRenderingBridgeDiagnostic.BrokenMember BROKEN_MEMBER =
        new FastRenderingBridgeDiagnostic.BrokenMember(
            "GLCommand.run",
            "ClassNotFoundException: com.genir.renderer.bridge.interfaces.GLCommand");

    /**
     * That member in the phrase a failure's detail slot takes.
     *
     * <p>Composed from the member above rather than spelled again, which is how the production
     * path fills the slot: a suite handing in a phrase the diagnostic would never produce would be
     * stating the wording rather than using it.
     */
    public static final String BROKEN_DETAIL = BROKEN_MEMBER.describe();

    /** What a second mod over the same binding loses, which is nothing the first one does. */
    public static final String COLONY_PANEL_LOST_FEATURE =
        "Colony panel rows will not show their upkeep this session.";

    /** A mod over the binding, as the key its records latch under and the sentence it loses. */
    public static final CompatibilityConsumer MAP_OVERLAY_CONSUMER =
        new CompatibilityConsumer("map-overlay", LOST_FEATURE);

    /**
     * A second mod over the same binding, losing something of its own - so a case about two
     * consumers being told apart cannot pass on one sentence standing for both.
     */
    public static final CompatibilityConsumer COLONY_PANEL_CONSUMER =
        new CompatibilityConsumer("colony-panel", COLONY_PANEL_LOST_FEATURE);

    private CompatibilityFailureFixture() {
        // fixture of static builders, no instances.
    }

    /**
     * @return the representative failure with neither version read and no cause
     */
    public static CompatibilityFailure createFailure() {

        return createFailureBetweenVersions(null, null);
    }

    /**
     * @param builtAgainstVersion the release KMLib was compiled against, or {@code null}
     * @param installedVersion    the release installed now, or {@code null}
     * @return the representative failure stated between those two versions
     */
    public static CompatibilityFailure createFailureBetweenVersions(
            String builtAgainstVersion,
            String installedVersion) {

        return new CompatibilityFailure(
            new CompatibilitySubject(SUBJECT_NAME, builtAgainstVersion, installedVersion),
            LOST_FEATURE,
            BROKEN_DETAIL,
            null);
    }

    /**
     * @param brokenDetail the member or detail the log names as broken
     * @return the representative failure, told apart from another by that detail
     */
    public static CompatibilityFailure createFailureBrokenAt(String brokenDetail) {

        return new CompatibilityFailure(
            createUnversionedSubject(),
            LOST_FEATURE,
            brokenDetail,
            null);
    }

    /**
     * @param lostFeature the sentence naming what the session loses
     * @return the representative failure, told apart from another by the modal it composes
     */
    public static CompatibilityFailure createFailureLosing(String lostFeature) {

        return new CompatibilityFailure(
            createUnversionedSubject(),
            lostFeature,
            BROKEN_DETAIL,
            null);
    }

    // The subject every builder that varies something other than a version shares: the third party
    // named, with neither version read. Stated once so a builder added beside them cannot spell a
    // different subject and have its case quietly be about two things.
    private static CompatibilitySubject createUnversionedSubject() {

        return new CompatibilitySubject(SUBJECT_NAME, null, null);
    }
}
