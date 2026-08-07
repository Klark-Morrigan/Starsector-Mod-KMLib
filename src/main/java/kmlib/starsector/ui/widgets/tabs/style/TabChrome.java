package kmlib.starsector.ui.widgets.tabs.style;

/**
 * Which surface a tab row's paint is laid onto. Vanilla keeps two tab conventions and a KM panel is drawn
 * to sit among whichever one stands nearest it: the sector map's seamless Sector/System strip, and the
 * intel screen's row of raised buttons standing clear of one another. The choice therefore travels inside
 * the {@link TabStyle} a host supplies, rather than being fixed by whichever renderer happens to draw the
 * row - a host that moves to a different screen changes its look, not its wiring.
 *
 * <p>It names the surface and nothing else. Both chromes read the same laid tab geometry and the same
 * resolved per-tab {@link TabLook} and {@link TabWash}, so a tab fades onto its hovered shade, pulses under
 * a click, and blinks for its bound key identically under either; what differs is only what those shades
 * are painted onto.
 */
public enum TabChrome {

    /** Abutting tabs parted by hairline seams over a grounded baseline - the sector map's own look. */
    STRIP,

    /** Each tab a framed button standing clear of its neighbours - the intel screen's look. */
    RAISED_BUTTON
}
