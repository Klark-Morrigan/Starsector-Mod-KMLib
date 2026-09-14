package kmlib.starsector.ui.input;

/**
 * Whether a pointer button is being held down on this frame.
 *
 * <p>A role rather than a static call for the reason {@link CursorPosition} is one: LWJGL's
 * {@link org.lwjgl.input.Mouse} answers button queries only once the game has created a mouse, so a
 * rule reaching for it directly would run only inside a running game - which is where a rule about
 * input is hardest to see failing.
 *
 * <p>Held rather than clicked, which is the distinction that decides who wants this. A click is an
 * edge, and an edge belongs to whatever consumes the event carrying it; this is the level a poller
 * reads, where the question is what the pointer is doing now rather than what it just did.
 * {@link kmlib.input.ClickEdgeDetector} turns a run of these samples back into an edge where one is
 * what is wanted.
 */
public interface PointerButtonHold {

    /**
     * @return whether the left button is down on this frame
     */
    boolean isLeftButtonHeld();
}
