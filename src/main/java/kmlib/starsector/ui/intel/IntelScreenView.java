package kmlib.starsector.ui.intel;

import kmlib.math.geometry.Rectangle;

/**
 * Reads the campaign intel screen (the {@code Intel} tab) from code that lives outside that screen.
 * Two things it exposes are not on the published UI API: whether the intel tab is the one showing,
 * and the screen rectangle of the intel screen's embedded map preview (the "visor") while that
 * preview is actually lit.
 *
 * <p>The two are separate questions, not one: the intel core tab is a container for three sub-tabs -
 * Intel, Planets and Factions - and only the Intel one carries the visor, so the tab-open read stays
 * true across all three while the visor rectangle is present on Intel alone.
 *
 * <p>Depending on this role keeps that code free of the game's concrete intel panel, which the API
 * jar does not publish and which cannot be stood up in a unit test. {@link VanillaIntelScreenView}
 * is the binding that routes to the live screen at runtime.
 */
public interface IntelScreenView {

    /**
     * @return whether the intel screen is the active core tab
     */
    boolean isIntelTabOpen();

    /**
     * @return the screen rectangle of the intel screen's embedded map preview, with the UI origin
     *         at the bottom-left, or {@code null} when there is no lit visor to draw over - the
     *         intel tab is not the one showing, one of the sibling sub-tabs that share it (Planets,
     *         Factions) is up instead, the panel is not laid out yet, or the preview is blanked (a
     *         large-description item hides it). A non-null result is exactly the signal that the
     *         visor is present.
     */
    Rectangle getVisorRect();
}
