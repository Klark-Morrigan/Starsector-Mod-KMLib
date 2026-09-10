package kmlib.starsector.ui.map.probes;

import kmlib.logging.SessionWarning;

import org.apache.log4j.Logger;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The probes' shared "this stopped working" lines, gathered so a reader who starts listening part
 * way through a session can be told what they missed.
 *
 * <p>A probe warns once per session because it is read from a render pass, where a repeated warning
 * would be written sixty times a second. That is right while the audience is fixed, and wrong the
 * moment it is not: a diagnostic switched on after a reach has already broken finds the one line
 * explaining why it can say nothing already spent, and so reports nothing and explains nothing.
 * Re-arming at the point someone asks to be told closes that.
 *
 * <p>Only the warnings a probe keeps one of per class are gathered here. Those are the ones that
 * outlive any particular reader and can therefore be found already spent. A probe holding one per
 * instance is a different case and stays out: its warning is made and discarded with the holder, so
 * a new holder starts with an unspent one already, and gathering them here would pin every instance
 * ever built for as long as the game runs.
 *
 * <p>Gathering them does not pool them. Each stays its own flag, so one probe's broken read still
 * cannot silence the news of another's - what is shared is only the moment they are all let speak
 * again.
 */
public final class MapProbeWarnings {

    // Written once per probe at class-init and read on a settings change, so contention is not the
    // concern; what is, is that class-init can happen on a loader thread while a change fires on
    // another, and a plain list iterated mid-registration would throw where a diagnostic must not.
    private static final List<SessionWarning> sharedWarnings = new CopyOnWriteArrayList<>();

    private MapProbeWarnings() {
    }

    /**
     * Lets every gathered warning be said once more, for a reader who has just asked to be told.
     *
     * <p>Call where the asking happens - a diagnostic being switched on - rather than on a retry.
     * The reaches behind these fail the same way every frame, so anything that re-arms on failure
     * turns "once per session" back into a warning a frame.
     */
    public static void rearmAllWarnings() {
        sharedWarnings.forEach(SessionWarning::rearmWarning);
    }

    /**
     * A warning a probe keeps one of for the whole session, gathered so it can be re-armed.
     *
     * @param logger the probe's own logger, so the line is attributed to whatever broke
     * @return the warning, which behaves exactly as one built directly
     */
    static SessionWarning createSharedWarning(Logger logger) {

        var warning = new SessionWarning(logger);
        sharedWarnings.add(warning);
        return warning;
    }
}
