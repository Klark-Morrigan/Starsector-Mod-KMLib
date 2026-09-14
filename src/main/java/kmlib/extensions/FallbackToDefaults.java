package kmlib.extensions;

/**
 * Whether the code that offered work may do it its own way after the installed implementation
 * declines.
 *
 * <p>Declining is ordinarily how an implementation says "not this case, not this install" - a
 * colonisation that cannot found on this kind of body, a hand-over that has nothing to say about
 * this market - and the operation carrying on with its own sequence is exactly right. That is not
 * true of every integration. A mod that replaces what a colony <em>is</em> has no correct outcome
 * where the library founds one the game's own way instead: the colony that results is one its own
 * rules were never applied to, and it is wrong from that moment on in ways nothing later can see.
 *
 * <p>Stated at registration because only the registrant knows which of the two it is. The
 * operation cannot tell a decline it should absorb from one that means the install is broken, and
 * a library guessing would either crash on the ordinary case or paper over the serious one.
 *
 * <p>An enum rather than a flag, so the call site says which it means. Registering with a bare
 * {@code true} reads as nothing at all at the point where the decision is actually made.
 */
public enum FallbackToDefaults {

    /**
     * The operation does the work its own way when the implementation declines. What an optional
     * integration ordinarily wants, and what every decline this library expects to see is.
     */
    PERMITTED,

    /**
     * A decline is a failure rather than a hand-back. For an implementation that has to run
     * wherever it is installed, because the work done any other way would be wrong on this install
     * rather than merely plainer.
     */
    FORBIDDEN,
}
