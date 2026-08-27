package kmlib.testfixtures.starsector.ui.coreui;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.coreui.CoreUiComponentRepainter;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link CoreUiComponentRepainter} that records what it was asked to repaint instead of drawing,
 * so the decisions around a repaint - whether one happens at all, which component, and what region
 * it is confined to - can be exercised without a GL context. Shipped from KMLib so both KMLib's and
 * consuming mods' tests drive the repaint seam through one shared double.
 *
 * <p>Optionally throws instead of recording, which is the live binding's own failure mode: the
 * draw entry point can go missing on any game build, and how a caller survives that is behaviour
 * worth pinning. The attempt is recorded before the throw, so a test can tell "never tried" from
 * "tried and failed".
 */
public final class CoreUiComponentRepainterFake implements CoreUiComponentRepainter {

    private final List<Object> repaintedComponents = new ArrayList<>();
    private final List<Rectangle> repaintedRegions = new ArrayList<>();
    private final RuntimeException failure;

    /** A repainter that records every call and draws nothing. */
    public CoreUiComponentRepainterFake() {
        this(null);
    }

    /**
     * @param failure what to throw after recording each call, or null to record and return
     */
    public CoreUiComponentRepainterFake(RuntimeException failure) {
        this.failure = failure;
    }

    /**
     * @return the components repainted, in call order
     */
    public List<Object> getRepaintedComponents() {
        return List.copyOf(repaintedComponents);
    }

    /**
     * @return the regions each repaint was clipped to, in call order
     */
    public List<Rectangle> getRepaintedRegions() {
        return List.copyOf(repaintedRegions);
    }

    @Override
    public void repaintClippedTo(Object component, Rectangle uiRegion) {

        repaintedComponents.add(component);
        repaintedRegions.add(uiRegion);

        if (failure != null) {
            throw failure;
        }
    }
}
