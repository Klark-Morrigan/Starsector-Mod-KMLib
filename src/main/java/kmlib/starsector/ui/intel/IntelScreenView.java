package kmlib.starsector.ui.intel;

import kmlib.math.geometry.Rectangle;

/**
 * Reads the campaign intel screen (the {@code Intel} tab) from code that lives outside that screen.
 * What it exposes is not on the published UI API: whether the intel tab is the one showing, the
 * screen rectangle of the intel screen's embedded map preview (its "map visor") while that preview
 * is actually lit, and whether that preview is drawing the starscape rather than the ordinary map.
 * The reads name the map visor rather than a bare "visor" because the intel screen carries only this
 * one map, and the rectangle is that map's - not the screen's own bounds.
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
    Rectangle getMapVisorRect();

    /**
     * Whether the map visor is in starscape mode: its Starscape filter checked while it shows
     * hyperspace. The game then paints the stylised starfield in place of the ordinary map and
     * suppresses the terrain layers drawn above it, so a custom overlay riding those layers is not
     * on screen even though the visor is lit. The intel screen keeps its own filter state, separate
     * from the full campaign map's, so this answers for the preview alone.
     *
     * @return whether the map visor is drawing the starscape, or {@code false} when the state
     *         cannot be read - the same answer as the game painting the map normally, which is what
     *         an unreadable link leaves it doing
     */
    boolean isMapStarscapeModeOn();
}
