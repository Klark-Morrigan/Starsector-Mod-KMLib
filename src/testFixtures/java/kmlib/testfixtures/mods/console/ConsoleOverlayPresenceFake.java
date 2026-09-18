package kmlib.testfixtures.mods.console;

import kmlib.mods.console.ConsoleOverlayPresence;

/**
 * A console overlay that is up or down because a test said so, standing in for the live read of
 * Console Commands' own panel wherever the question is only "and what does the caller do then?".
 *
 * <p>Stands in at the presence rather than at the gate above it, which is the seam that exists:
 * the gate's fail-open handling is behaviour a caller inherits rather than behaviour a suite
 * should replace, so a case drives this and lets the real gate run over it. The gate only reaches
 * this read on an install reporting the mod enabled, which
 * {@link kmlib.testfixtures.starsector.settings.ModStateScopes#runWithModEnabled} arranges - so a
 * case that leaves the scope off is posing the mod-absent install instead.
 *
 * <p>Counts the asks so a case can pin that the gate short-circuits before the console is touched
 * at all - the thing that keeps an install without Console Commands from resolving a class it
 * does not have.
 *
 * <p>Published as a fixture variant with the rest of {@code kmlib.testfixtures} because the callers
 * that stand down for a console are spread across the series: the answer decides whether they draw
 * and route at all, and that is the behaviour their own tests pin.
 */
public final class ConsoleOverlayPresenceFake implements ConsoleOverlayPresence {

    private int askCount;
    private boolean isOverlayUp;

    @Override
    public boolean isOverlayUp() {

        askCount++;
        return isOverlayUp;
    }

    /**
     * @return how many times the console has been asked whether its overlay is up
     */
    public int readAskCount() {
        return askCount;
    }

    /**
     * Takes the console down again, as if the player had dismissed it.
     */
    public void closeConsole() {
        isOverlayUp = false;
    }

    /**
     * Puts a console up, as if the player had just summoned one.
     */
    public void openConsole() {
        isOverlayUp = true;
    }
}
