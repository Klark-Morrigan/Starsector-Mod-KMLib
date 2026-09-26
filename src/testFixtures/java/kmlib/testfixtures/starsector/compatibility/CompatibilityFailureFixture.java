package kmlib.testfixtures.starsector.compatibility;

import kmlib.opengl.FastRenderingBridgeDiagnostic;
import kmlib.starsector.compatibility.CompatibilityBreakage;
import kmlib.starsector.compatibility.CompatibilityConsumer;
import kmlib.starsector.compatibility.CompatibilityFailure;
import kmlib.starsector.compatibility.CompatibilityFailures;
import kmlib.starsector.compatibility.CompatibilitySubject;
import kmlib.starsector.compatibility.ModIntegration;
import kmlib.testfixtures.starsector.settings.StubbedModIds;

/**
 * One representative compatibility failure, with a builder per slot a case varies.
 *
 * <p>A suite about the record, the notice or the wording needs a failure to hand over and cares
 * about one of its slots at most: which versions it names, which detail broke, or what it costs.
 * Each builder varies that one slot and fills the rest from the constants here, so a case reads
 * as being about the slot it names and nothing else.
 *
 * <p>Also the drain for the process's own record, which every suite driving a guard that records
 * into it needs on both sides of a case.
 *
 * <p>Final class with a private constructor: fixture of static builders, no instances.
 */
public final class CompatibilityFailureFixture {

    /** The third party every failure here is about. */
    public static final String SUBJECT_NAME = "Fast Rendering";

    /** The key a suite latches that third party under on a record. */
    public static final String FAST_RENDERING_SUBJECT_KEY = "fast-rendering";

    /** A second subject's key, for a case about two subjects recorded apart. */
    public static final String NEXERELIN_SUBJECT_KEY = StubbedModIds.NEXERELIN;

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

    /** Which guard caught the binding, as the phrase the log's "failed while" row takes. */
    public static final String FAILURE_SITE = "resolving the binding";

    /** What a failed binding does not cost, for the notice's optional row. */
    public static final String UNAFFECTED_FEATURE = "On everything else, including your save.";

    /** What a second mod over the same binding loses, which is nothing the first one does. */
    public static final String COLONY_PANEL_LOST_FEATURE =
        "Colony panel rows will not show their upkeep this session.";

    /** The mod the representative consumer belongs to, which leads the key its records latch under. */
    public static final String MAP_OVERLAY_MOD_ID = "map-mod";

    /** A second mod, so a case about two mods over one binding cannot pass on one ID standing for two. */
    public static final String COLONY_PANEL_MOD_ID = "colony-mod";

    /** The feature the representative consumer took the binding for, as the other half of its key. */
    public static final String MAP_OVERLAY_FEATURE_KEY = "map-overlay";

    /** A mod over the binding, as the key its records latch under and the sentence it loses. */
    public static final CompatibilityConsumer MAP_OVERLAY_CONSUMER =
        new CompatibilityConsumer(MAP_OVERLAY_MOD_ID, MAP_OVERLAY_FEATURE_KEY, LOST_FEATURE, UNAFFECTED_FEATURE);

    /**
     * A second mod over the same binding, losing something of its own - so a case about two
     * consumers being told apart cannot pass on one sentence standing for both.
     */
    public static final CompatibilityConsumer COLONY_PANEL_CONSUMER =
        new CompatibilityConsumer(COLONY_PANEL_MOD_ID, "colony-panel", COLONY_PANEL_LOST_FEATURE);

    /** The third party a start-up step integrates with here, as the ID its records latch under. */
    public static final String INTEGRATED_MOD_ID = StubbedModIds.RANDOM_ASSORTMENT_OF_THINGS;

    /** That mod as a report names it, which is not a string its ID could be mistaken for. */
    public static final String INTEGRATED_MOD_NAME = "Random Assortment of Things";

    /**
     * A start-up step's integration with that mod, losing what the representative consumer loses.
     *
     * <p>Here rather than at each suite that needs one: the guard and the composition are two
     * subjects and both are about the same integration, and two copies of it are two that can be
     * edited apart until a case passes against an integration the other one never had.
     */
    public static final ModIntegration MOD_INTEGRATION =
        new ModIntegration(INTEGRATED_MOD_ID, INTEGRATED_MOD_NAME, MAP_OVERLAY_CONSUMER);

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
            MAP_OVERLAY_CONSUMER,
            new CompatibilityBreakage(FAILURE_SITE, BROKEN_DETAIL),
            null);
    }

    /**
     * @param brokenDetail the member or detail the log names as broken
     * @return the representative failure, told apart from another by that detail
     */
    public static CompatibilityFailure createFailureBrokenAt(String brokenDetail) {

        return new CompatibilityFailure(
            createUnversionedSubject(),
            MAP_OVERLAY_CONSUMER,
            new CompatibilityBreakage(FAILURE_SITE, brokenDetail),
            null);
    }

    /**
     * @param failureSite which guard caught the binding, as the log's "failed while" row states it
     * @return the representative failure, told apart from another by where it was caught
     */
    public static CompatibilityFailure createFailureCaughtWhile(String failureSite) {

        return new CompatibilityFailure(
            createUnversionedSubject(),
            MAP_OVERLAY_CONSUMER,
            new CompatibilityBreakage(failureSite, BROKEN_DETAIL),
            null);
    }

    /**
     * @param lostFeature the sentence naming what the session loses
     * @return the representative failure, told apart from another by the modal it composes
     */
    public static CompatibilityFailure createFailureLosing(String lostFeature) {

        return createFailureTakenBy(
            new CompatibilityConsumer(MAP_OVERLAY_MOD_ID, MAP_OVERLAY_FEATURE_KEY, lostFeature));
    }

    /**
     * @param consumer the mod the failure is filed for, as the record handed it to a describer
     * @return the representative failure, told apart from another by the consumer it carries
     */
    public static CompatibilityFailure createFailureTakenBy(CompatibilityConsumer consumer) {

        return new CompatibilityFailure(
            createUnversionedSubject(),
            consumer,
            new CompatibilityBreakage(FAILURE_SITE, BROKEN_DETAIL),
            null);
    }

    /**
     * Empties {@link CompatibilityFailures#SESSION_RECORD} of every failure not yet reported.
     *
     * <p>That record is the process's own and outlives a case, so a case left filling it hands the
     * next one to read it a failure it never filed. Its latch cannot be emptied, which is why a case
     * recording into it still needs a third party or consumer of its own.
     */
    public static void drainSessionRecord() {

        while (CompatibilityFailures.SESSION_RECORD.takeNextUnreported() != null) {
            // drained for its side effect.
        }
    }

    // The subject every builder that varies something other than a version shares: the third party
    // named, with neither version read. Stated once so a builder added beside them cannot spell a
    // different subject and have its case quietly be about two things.
    private static CompatibilitySubject createUnversionedSubject() {

        return new CompatibilitySubject(SUBJECT_NAME, null, null);
    }
}
