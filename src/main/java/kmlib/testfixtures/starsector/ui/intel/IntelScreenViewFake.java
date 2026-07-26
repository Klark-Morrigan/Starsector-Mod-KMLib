package kmlib.testfixtures.starsector.ui.intel;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.intel.IntelScreenView;

/**
 * An {@link IntelScreenView} whose reads are set directly, so code that gates on the intel screen
 * can be exercised without a running game. Shipped from KMLib so both KMLib's and consuming mods'
 * tests drive the intel-screen seam through one shared double.
 *
 * <p>A {@code null} visor rectangle stands for "no lit visor to draw over" - the intel tab is not
 * showing, or the preview is blanked - which is the state the port models and callers survive.
 */
public final class IntelScreenViewFake implements IntelScreenView {
    private boolean isIntelTabOpen;
    private Rectangle mapVisorRect;

    public void setIntelTabOpen(boolean isIntelTabOpen) {
        this.isIntelTabOpen = isIntelTabOpen;
    }

    public void setMapVisorRect(Rectangle mapVisorRect) {
        this.mapVisorRect = mapVisorRect;
    }

    @Override
    public boolean isIntelTabOpen() {
        return isIntelTabOpen;
    }

    @Override
    public Rectangle getMapVisorRect() {
        return mapVisorRect;
    }
}
