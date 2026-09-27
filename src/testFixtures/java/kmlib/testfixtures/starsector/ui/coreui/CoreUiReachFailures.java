package kmlib.testfixtures.starsector.ui.coreui;

import java.lang.reflect.InvocationTargetException;

/**
 * The two ways a reach through {@code CoreUiTree} fails on a game build it does not recognise, as
 * the caller of that reach meets them. Published as a fixture variant so both KMLib's and consuming
 * mods' tests can throw through a boundary what the game would, rather than the unchecked exception
 * that is easiest to write and that a boundary catching too little still contains.
 *
 * <p>Each answers any type, so it stands in for a reach wherever one is taken as a supplier.
 */
public final class CoreUiReachFailures {

    private CoreUiReachFailures() {
    }

    /**
     * Fails as a hop whose target threw: the target's own failure wrapped in the checked exception
     * reflection wraps it in, and thrown undeclared, since the reach passes on what it caught
     * rather than declaring it.
     *
     * @param <T> whatever the reach would have answered
     * @return never
     */
    public static <T> T throwWrappedGameFailure() {

        throw throwUndeclared(new InvocationTargetException(
            new IllegalStateException("The game's own method refused the call.")));
    }

    /**
     * Fails as a hop onto a member this game build no longer carries.
     *
     * @param <T> whatever the reach would have answered
     * @return never
     */
    public static <T> T throwUnlinkedMember() {

        throw new NoSuchMethodError("A member this game build no longer carries.");
    }

    // Throws a checked exception past the compiler's check, which is how the reach hands one on.
    @SuppressWarnings("unchecked")
    private static <E extends Throwable> RuntimeException throwUndeclared(Throwable thrown) throws E {
        throw (E) thrown;
    }
}
