package kmlib.starsector.ui.coreui;

/**
 * What a modal over the core UI is doing this frame: whether one is there at all, and how far through
 * its own fade it stands.
 *
 * <p>The two travel together because a caller standing aside for a modal needs both on the same frame
 * and needs them to agree. A modal claims every event outside its own box from the frame it is raised,
 * while its fade is still at nothing - so input stands down on <em>presence</em>, and only what is
 * drawn follows the <em>brightness</em>. Read separately, those two answers come off two walks of the
 * widget tree, and a modal raised between them would be reported to one half of a caller and not the
 * other.
 *
 * @param isShowing  whether a modal is up, true from the frame it is raised and until its fade has
 *                   fully run out
 * @param brightness how far through that fade it stands, 0..1 - the same curve it darkens the screen
 *                   by, so anything fading against it lands frame for frame
 */
public record ModalDialogState(
    boolean isShowing,
    float brightness) {

    /** No modal over the core UI, which is every ordinary frame. */
    public static final ModalDialogState NONE =
        new ModalDialogState(false, 0f);
}
