package kmlib.starsector.ui.widgets;

import kmlib.math.geometry.Rectangles;
import kmlib.starsector.ui.font.LineWidthMeasurer;
import kmlib.text.KmlibStrings;

import java.util.ArrayList;
import java.util.List;

/**
 * The geometry of a vanilla-styled tab row: lays each tab out snapped to its label-plus-shortcut
 * width and resolves which tab a point falls in. Substrate-independent - it produces {@link
 * VanillaTab} models and renders nothing - so a GL or a UI-API renderer can paint the row against it.
 * Derived from the base {@link TabStrip} for the snapping math; it composes each tab's display string
 * ("Label  [K]") so the layout measures exactly the text the paint draws. The raw-GL paint lives in
 * {@link kmlib.starsector.ui.render.gl.VanillaTabStripRenderer}.
 */
public final class VanillaTabStrip {
    // The layout approximates the label-to-shortcut gap with two spaces in the measured display
    // string; the tab padding absorbs the small difference against the paint pass, so it never clips.
    private static final String SHORTCUT_GAP_TEXT = "  ";

    private VanillaTabStrip() {
    }

    /**
     * Lays the tab row out, each tab snapped to its label-plus-shortcut width, by delegating to
     * the base {@link TabStrip} over each tab's composed display string.
     *
     * @param originX       the row's left edge, in UI coordinates
     * @param rowTopY       the row's top edge, in UI coordinates
     * @param tabHeight     the height every tab shares
     * @param textPadding   slack added to each measured label so text does not touch the edges
     * @param minTabWidth   the narrowest a tab may be
     * @param fontSize      the size the labels are measured (and later drawn) at
     * @param contents      the tabs' labels and optional shortcuts, in row order
     * @param measurer      measures each display string's rendered width
     * @return one {@link VanillaTab} per content, in the same order
     */
    public static List<VanillaTab> layoutTabs(float originX, float rowTopY, float tabHeight,
            float textPadding, float minTabWidth, double fontSize, List<VanillaTabContent> contents,
            LineWidthMeasurer measurer) {
        var displays = new ArrayList<String>(contents.size());
        for (var content : contents) {
            displays.add(composeDisplay(content));
        }
        var laidOut = TabStrip.layoutTabs(originX, rowTopY, tabHeight, textPadding, minTabWidth,
                fontSize, displays, measurer);
        var tabs = new ArrayList<VanillaTab>(contents.size());
        for (var index = 0; index < contents.size(); index++) {
            tabs.add(new VanillaTab(contents.get(index), laidOut.get(index).bounds()));
        }
        return List.copyOf(tabs);
    }

    /**
     * The index of the tab containing {@code (pointX, pointY)}, or {@link TabStrip#NO_TAB} when
     * the point falls outside every tab.
     *
     * @param tabs   the laid-out tabs to test against
     * @param pointX the point's x, in UI coordinates
     * @param pointY the point's y, in UI coordinates
     * @return the containing tab's index, or {@link TabStrip#NO_TAB}
     */
    public static int findTabIndexAt(List<VanillaTab> tabs, float pointX, float pointY) {
        return Rectangles.findIndexContaining(tabs, VanillaTab::bounds, pointX, pointY);
    }

    /**
     * A tab's display string - "Label  [K]" when it has a shortcut, else just the label - so the
     * layout pass measures the same text the paint pass draws. Public so the renderer composes the
     * identical string rather than re-deriving the spacing convention.
     *
     * @param content the tab's label and optional shortcut
     * @return the composed display string
     */
    public static String composeDisplay(VanillaTabContent content) {
        if (!KmlibStrings.hasText(content.shortcut())) {
            return content.label();
        }
        return content.label() + SHORTCUT_GAP_TEXT + bracketShortcut(content.shortcut());
    }

    /**
     * A shortcut wrapped in brackets - "[K]" - the form both the measured display string and the
     * gold paint drawable use, kept here as the single source so the two never drift on the bracket
     * convention.
     *
     * @param shortcut the raw shortcut key name
     * @return the bracketed shortcut
     */
    public static String bracketShortcut(String shortcut) {
        return "[" + shortcut + "]";
    }
}
