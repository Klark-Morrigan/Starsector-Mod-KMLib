package kmlib.testfixtures.starsector.compatibility;

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

    /** The member that stopped holding, as the log names it. */
    public static final String BROKEN_DETAIL = "GLCommand is absent";

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
            new CompatibilitySubject(SUBJECT_NAME, null, null),
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
            new CompatibilitySubject(SUBJECT_NAME, null, null),
            lostFeature,
            BROKEN_DETAIL,
            null);
    }
}
