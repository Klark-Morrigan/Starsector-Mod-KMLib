package kmlib.starsector.ui.coreui;

/**
 * What something raised over a screen is doing this frame: whether it holds the screen, and how far
 * through its own fade it stands.
 *
 * <p>The two travel together because whatever stands aside for an overlay needs both on the same frame and
 * needs them to agree. Input stands down on <em>presence</em>, from the frame the overlay takes the screen
 * and while its fade is still at nothing; only what is drawn follows the <em>fade</em>. Read separately,
 * those two answers can come off two readings taken either side of an overlay appearing, and one half of a
 * caller would be told what the other was not.
 *
 * <p>Which frames each half covers is the producer's to state, and the two published here differ. A modal
 * the game raises holds the screen until its fade has fully run out, that being how long the game keeps it
 * in the tree intercepting; something a mod raises itself may let go the moment it is dismissed and go on
 * painting for the length of its fall. Both are honest answers to "does this hold the screen", which is
 * why one shape serves both - and why a caller reads the flag rather than inferring it from the fraction.
 *
 * @param isRaised     whether the overlay holds the screen, true from the frame it takes it
 * @param fadeFraction how far through its own fade it stands, 0..1 - the curve it darkens the screen by,
 *                     so anything fading against it lands frame for frame
 */
public record OverlayPresence(
    boolean isRaised,
    float fadeFraction) {

    /** Nothing raised over the screen, which is every ordinary frame. */
    public static final OverlayPresence NONE =
        new OverlayPresence(false, 0f);
}
