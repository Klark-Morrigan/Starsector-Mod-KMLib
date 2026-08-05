package kmlib.starsector.ui.map.probes;

import org.apache.log4j.Logger;

/**
 * A probe's "this stopped working" news, said once per session.
 *
 * <p>Every probe here has to fail open - a read taken to refine a feature must not be able to
 * switch it off - which is exactly what would let a broken reach into the widget tree turn off
 * everything built on it without a word. The warning is what stops that, so it is WARN rather than
 * DEBUG, since it has to survive the default log level to be the warning it was meant to be.
 *
 * <p>Once, because the probes are called from render passes: a warning that repeated would be
 * written sixty times a second, and the second one says nothing the first did not. Holding the flag
 * beside the logger is what makes "warn, but only once" one decision in one place rather than a
 * rule each probe restates and can restate wrongly.
 *
 * <p>The first warning wins where a probe has more than one way to fail, silencing the rest for the
 * session. That costs nothing when either failure means the same thing - the probe cannot answer -
 * and a probe whose two failures mean different things wants two of these rather than one.
 */
final class SessionWarning {

    private final Logger logger;

    private boolean hasWarnedThisSession;

    /**
     * @param logger the owning probe's own logger, so the line is attributed to whichever read
     *               broke rather than to this holder
     */
    SessionWarning(Logger logger) {
        this.logger = logger;
    }

    /**
     * Whether this has already been said, so a probe whose message costs something to build - a
     * walk into the campaign UI, a tree read - can skip building one that would be swallowed.
     *
     * @return whether a warning has been written this session
     */
    boolean hasWarnedThisSession() {
        return hasWarnedThisSession;
    }

    /**
     * Says this once, if nothing has been said yet this session.
     *
     * @param message what stopped working, and what that costs the caller
     */
    void warnOnce(String message) {
        warnOnce(message, null);
    }

    /**
     * Says this once, if nothing has been said yet this session.
     *
     * @param message what stopped working, and what that costs the caller
     * @param failure what was thrown, or null where the read failed by answering nothing rather
     *                than by throwing
     */
    void warnOnce(String message, Throwable failure) {
        if (hasWarnedThisSession) {
            return;
        }
        hasWarnedThisSession = true;
        if (failure == null) {
            logger.warn(message);
        } else {
            logger.warn(message, failure);
        }
    }
}
