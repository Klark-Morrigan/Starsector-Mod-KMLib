package kmlib.starsector.ui.widgets.tabs;

import kmlib.starsector.ui.widgets.tabs.style.TabLook;
import kmlib.starsector.ui.widgets.tabs.style.TabPalette;

/**
 * Where a tab strip's paint pass gets each tab's settled look from: asked per tab index, one already resolved
 * {@link TabLook} per answer. The seam exists so a renderer stays a chrome pass - it paints a finished fill
 * and label, never learning which state named them or how far a fade toward another look has run.
 *
 * <p>The look channel and the {@link TabWashSource} lift channel are two seams rather than one combined
 * value, because they resolve in an order: a look settles first and a wash lifts whatever it yields. Folding
 * them together would leave a click over a half-faded hover no way to say which was applied to which.
 */
@FunctionalInterface
public interface TabLookSource {

    /**
     * Binds a palette to a row's selection and its hover progress: each tab settles on the look its selection
     * names, blended toward the palette's hovered shade by however far that tab's fade has run. Held here
     * rather than at each caller so the map strip and the raised-button chrome resolve a look identically.
     *
     * @param palette       the strip's paint - the settled looks and the hovered shade they meet at
     * @param selectedIndex the active tab's index, or a value outside the row when none is
     * @param hovers        how far each tab has travelled onto the hovered shade
     * @return a source answering the resolved look for any index in the row
     */
    static TabLookSource createHoverFadedLookSource(
            TabPalette palette,
            int selectedIndex,
            TabHoverSource hovers) {

        return tabIndex -> palette.resolveLookAtHoverFraction(
            tabIndex == selectedIndex,
            hovers.resolveHoverFractionAt(tabIndex));
    }

    /**
     * The look the tab at {@code tabIndex} currently wears, before any momentary lift.
     *
     * @param tabIndex the tab's index in row order
     * @return its resolved fill and label colour
     */
    TabLook resolveLookAt(int tabIndex);
}
