package kmlib.logging;

import org.apache.log4j.Logger;

/**
 * A "this stopped working" line, said once per session however many times it is reached.
 *
 * <p>The shape it exists for is a read or a draw that must fail open - one taken to refine a
 * feature must not be able to switch that feature off - which is exactly what would otherwise let
 * something break silently and take its feature with it without a word in the log. This is what
 * stops that, so it is WARN rather than DEBUG: it has to survive the default log level to be the
 * warning it was meant to be.
 *
 * <p>Once, because such code sits in per-frame paths: a warning that repeated would be written
 * sixty times a second, and the second says nothing the first did not. Holding the flag beside the
 * logger is what makes "warn, but only once" one decision in one place rather than a rule each
 * caller restates and can restate wrongly.
 *
 * <p>The first warning wins where there is more than one way to fail, silencing the rest for the
 * session. That costs nothing when either failure means the same thing - the answer cannot be had -
 * and two failures meaning different things want two of these rather than one.
 *
 * <p>Per instance rather than per class, so one holder's broken read cannot silence the news of
 * another's: how many of these to keep is the owner's decision about how many distinct failures it
 * has, and a shared static would take that decision away.
 */
public final class SessionWarning {

    private final Logger logger;

    private boolean hasWarnedThisSession;

    /**
     * @param logger the owner's own logger, so the line is attributed to whatever broke rather
     *               than to this holder
     */
    public SessionWarning(Logger logger) {
        this.logger = logger;
    }

    /**
     * Whether this has already been said, so an owner whose message costs something to build - a
     * walk into the campaign UI, a tree read - can skip building one that would be swallowed.
     *
     * @return whether a warning has been written this session
     */
    public boolean hasWarnedThisSession() {
        return hasWarnedThisSession;
    }

    /**
     * Says this once, if nothing has been said yet this session.
     *
     * @param message what stopped working, and what that costs the caller
     */
    public void warnOnce(String message) {
        warnOnce(message, null);
    }

    /**
     * Says this once, if nothing has been said yet this session.
     *
     * @param message what stopped working, and what that costs the caller
     * @param failure what was thrown, or null where it failed by answering nothing rather than by
     *                throwing
     */
    public void warnOnce(String message, Throwable failure) {

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
