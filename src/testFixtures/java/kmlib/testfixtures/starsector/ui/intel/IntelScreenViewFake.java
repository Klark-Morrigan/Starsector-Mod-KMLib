package kmlib.testfixtures.starsector.ui.intel;

import com.fs.starfarer.api.ui.UIComponentAPI;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.intel.IntelScreenView;
import kmlib.starsector.ui.intel.MapVisorState;

/**
 * An {@link IntelScreenView} whose reads are set directly, so code that gates on the intel screen
 * can be exercised without a running game. Published as a fixture variant so both KMLib's and consuming mods'
 * tests drive the intel-screen seam through one shared double.
 *
 * <p>A {@code null} visor rectangle stands for "no lit visor to draw over" - the intel tab is not
 * showing, or the preview is blanked - which is the state the port models and callers survive. The
 * starscape flag is set independently of it, since a lit visor can be drawing either the ordinary
 * map or the starscape.
 *
 * <p>The visor's rectangle and its component are set independently even though the live binding
 * derives one from the other, because a test drives whichever of the two its subject reads and
 * building a component mock to set a rectangle would be ceremony for every test that wants neither.
 * A test whose subject reads both is the one that has to set both.
 *
 * <p>The combined visor state is the exception: it is derived from the rectangle and the starscape
 * flag rather than set on its own. The live binding takes all three off one reading, so a fake that
 * let them be set apart could pose a screen no game can be in - a visor that is showing to one read
 * and absent to the next - and a subject that passed against it would still be wrong in the game.
 */
public final class IntelScreenViewFake implements IntelScreenView {
    private boolean isIntelTabOpen;
    private boolean isMapStarscapeModeOn;
    private Rectangle mapVisorRect;
    private UIComponentAPI mapVisorWidget;

    public void setIntelTabOpen(boolean isIntelTabOpen) {
        this.isIntelTabOpen = isIntelTabOpen;
    }

    public void setMapStarscapeModeOn(boolean isMapStarscapeModeOn) {
        this.isMapStarscapeModeOn = isMapStarscapeModeOn;
    }

    public void setMapVisorRect(Rectangle mapVisorRect) {
        this.mapVisorRect = mapVisorRect;
    }

    public void setMapVisorWidget(UIComponentAPI mapVisorWidget) {
        this.mapVisorWidget = mapVisorWidget;
    }

    @Override
    public boolean isIntelTabOpen() {
        return isIntelTabOpen;
    }

    @Override
    public Rectangle getMapVisorRect() {
        return mapVisorRect;
    }

    @Override
    public UIComponentAPI getMapVisorWidget() {
        return mapVisorWidget;
    }

    @Override
    public MapVisorState readMapVisorState() {

        if (mapVisorRect == null) {
            return MapVisorState.NOT_SHOWING;
        }

        return isMapStarscapeModeOn
            ? MapVisorState.SHOWING_IN_STARSCAPE_MODE
            : MapVisorState.SHOWING_WITH_STARSCAPE_OFF;
    }

    @Override
    public boolean isMapStarscapeModeOn() {
        return isMapStarscapeModeOn;
    }
}
