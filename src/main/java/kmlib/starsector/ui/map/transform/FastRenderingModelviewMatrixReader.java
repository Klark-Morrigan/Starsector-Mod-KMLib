package kmlib.starsector.ui.map.transform;

import kmlib.opengl.FastRendering;
import kmlib.starsector.compatibility.CompatibilityBreakage;
import kmlib.starsector.compatibility.CompatibilityConsumer;
import kmlib.starsector.compatibility.CompatibilityFailure;
import kmlib.starsector.compatibility.CompatibilityFailures;
import kmlib.starsector.compatibility.CompatibilitySubject;
import kmlib.starsector.ui.compatibility.ScreenCompatibilityNotices;

import java.util.Objects;

/**
 * Reports the modelview Fast Rendering draws with, the binding of {@link ModelviewMatrixReader} for
 * that renderer.
 *
 * <p>The read itself is the stock one. From Fast Rendering {@value FastRendering#FIRST_MODELVIEW_READ_RELEASE}
 * the bridge answers {@code glGetFloat(GL_MODELVIEW_MATRIX)} inline, from a copy of the matrix it keeps
 * on the calling thread, already in GL's column-major layout. What this binding adds is the guard
 * around that read.
 *
 * <p>Earlier releases refuse the read mid-render: {@code UnsupportedOperationException} from
 * {@code v0.8.9}, {@code NoSuchMethodError} before it. Let out of a render pass, either costs the
 * game over a hover highlight. So the first failure turns the reading off for the session and files
 * one report telling the player which release the read needs. {@code docs/dev/rendering-environment.md}
 * records the read and its history.
 *
 * <p>One reader per consumer, built by {@link ModelviewMatrixReaders}, rather than a shared
 * singleton. A reader that could not say who it serves could not record a failure against anyone,
 * and what a failed read costs is the taking mod's to state.
 */
public final class FastRenderingModelviewMatrixReader implements ModelviewMatrixReader {

    /** Where the read failed, as the phrase completing the report's "failed while" row. */
    static final String FAILURE_SITE = "reading the modelview back through the bridge";

    /** The read the bridge did not serve, as the report's "broken" row names it. */
    static final String REFUSED_READ = "GL11.glGetFloat(GL_MODELVIEW_MATRIX, FloatBuffer)";

    // Who takes the reading and where its failure is filed. Held rather than reached for, so the
    // sentence a player reads is the taking mod's and a suite records into a record of its own.
    private final CompatibilityConsumer consumer;
    private final CompatibilityFailures failureRecord;

    // The read this binding guards. Taken rather than named, so a suite can hand in a read that
    // refuses the call - the one thing a live renderer will not do on demand.
    private final ModelviewMatrixReader glReader;

    // Latched on the first failure and never cleared: the renderer does not change while the game
    // runs, so a read it refused once it refuses for the session.
    private boolean isReadRefused;

    FastRenderingModelviewMatrixReader(
            CompatibilityConsumer consumer,
            CompatibilityFailures failureRecord,
            ModelviewMatrixReader glReader) {

        this.consumer = Objects.requireNonNull(
            consumer,
            "A reader with no consumer could not say whose feature a refused read costs.");
        this.failureRecord = Objects.requireNonNull(
            failureRecord,
            "A reader with nowhere to record would degrade silently and tell no player why.");
        this.glReader = Objects.requireNonNull(
            glReader,
            "A reader with no read to guard would have nothing to report.");
    }

    @Override
    public float[] readModelviewMatrix() {

        // Checked before the bridge is reached: asking again would only throw again, every frame the
        // map is open.
        if (isReadRefused) {
            return null;
        }
        try {
            return glReader.readModelviewMatrix();

        } catch (LinkageError | RuntimeException readFailure) {
            // Both shapes a refusal has taken: a method the bridge did not declare, and one it
            // declares and refuses. A fault in the JVM itself is not caught, being the one thing not
            // worth trading for a degraded overlay.
            isReadRefused = true;
            recordReadFailure(readFailure);
            return null;
        }
    }

    // The slots of one failure: the renderer with the release the read needs and the one installed,
    // what was refused and where, the consumer's own sentence, and the caught error as the cause a
    // log line carries a trace from.
    //
    // Takes the installed version rather than reading it, so what fills which slot is stated against
    // a version a suite chooses - the live read reports whichever jar the machine has.
    static CompatibilityFailure composeReadFailure(
            CompatibilityConsumer consumer,
            Throwable readFailure,
            String installedVersion) {

        return new CompatibilityFailure(
            new CompatibilitySubject(
                FastRendering.COMPATIBILITY_SUBJECT_NAME,
                FastRendering.FIRST_MODELVIEW_READ_RELEASE,
                installedVersion),
            consumer,
            new CompatibilityBreakage(FAILURE_SITE, REFUSED_READ),
            readFailure);
    }

    private void recordReadFailure(Throwable readFailure) {

        // Composed against the consumer the record hands back rather than the one held: the two
        // differ only where the record found the consumer's key reused, and the report is filed
        // under whichever key the record settled on. The version is read inside the description, so
        // it is paid on the record that is kept and not on one the latch ignores.
        failureRecord.recordOnce(
            FastRendering.COMPATIBILITY_SUBJECT_KEY,
            consumer,
            recordedAs -> composeReadFailure(recordedAs, readFailure, FastRendering.readInstalledVersion()));

        // Told on the screen it was found on. The read fails during a map pass, and the script that
        // shows the campaign's dialog is not advanced while a core screen is up - so left to that
        // reporter alone, the failure would be shown only once the player had left the map it was
        // about. A raise that finds no screen leaves the record untouched and the dialog gets it.
        ScreenCompatibilityNotices.showPendingFailureOnScreen(failureRecord);
    }
}
