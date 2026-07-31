package kmlib.starsector.ui.widgets.tabs;

import kmlib.math.geometry.Rectangle;
import kmlib.math.geometry.Rectangles;
import kmlib.starsector.ui.font.LineWidthMeasurer;
import kmlib.starsector.ui.widgets.segments.SegmentSpec;
import kmlib.text.KmlibStrings;

import java.util.ArrayList;
import java.util.List;

/**
 * The geometry of a vanilla-styled tab row: lays each tab out snapped to its label-plus-shortcut
 * width and resolves which tab a point falls in. Substrate-independent - it produces {@link
 * VanillaTab} models and renders nothing - so a GL or a UI-API renderer can paint the row against it.
 * Derived from the base {@link TabStrip} for the snapping math; it composes each tab's display string
 * ("Label  (K)") so the layout measures exactly the text the paint draws. The raw-GL paint lives in
 * {@link kmlib.starsector.ui.render.gl.VanillaTabStripRenderer}.
 */
public final class VanillaTabStrip {
    /**
     * The opening delimiter wrapped around a shortcut key in the display string - a square bracket, the
     * way vanilla brackets a hotkey (the map's own {@code Sector [O]} / {@code System [W]} tabs). Public
     * so the paint pass draws the delimiter as its own text segment - in the label colour, apart from the
     * gold key - off the same literal the display string is measured with.
     */
    public static final String SHORTCUT_OPEN_DELIMITER = "[";

    /**
     * The closing delimiter wrapped around a shortcut key in the display string, the pair to
     * {@link #SHORTCUT_OPEN_DELIMITER}.
     */
    public static final String SHORTCUT_CLOSE_DELIMITER = "]";

    // The layout approximates the label-to-shortcut gap with two spaces in the measured display
    // string; the tab padding absorbs the small difference against the paint pass, so it never clips.
    private static final String SHORTCUT_GAP_TEXT = "  ";

    private VanillaTabStrip() {
    }

    /**
     * Lays the tab row out, each tab snapped to its label-plus-shortcut width, by delegating to
     * the base {@link TabStrip} over each tab's composed display string.
     *
     * @param originX   the row's left edge, in UI coordinates
     * @param rowTopY   the row's top edge, in UI coordinates
     * @param tabHeight the height every tab shares
     * @param spec      the tab-sizing rule (padding, minimum, font size, SNAPPED)
     * @param contents  the tabs' labels and optional shortcuts, in row order
     * @param measurer  measures each display string's rendered width
     * @return one {@link VanillaTab} per content, in the same order
     */
    public static List<VanillaTab> layoutTabs(
            float originX,
            float rowTopY,
            float tabHeight,
            SegmentSpec spec,
            List<VanillaTabContent> contents,
            LineWidthMeasurer measurer) {

        var laidOut = TabStrip.layoutTabs(
            originX,
            rowTopY,
            tabHeight,
            spec,
            composeDisplays(contents),
            measurer);

        var bounds = new ArrayList<Rectangle>(laidOut.size());

        for (var tab : laidOut) {
            bounds.add(tab.bounds());
        }
        return zipTabs(contents, bounds);
    }

    /**
     * Pairs each tab content with its laid-out box, in order, into {@link VanillaTab}s. The single source
     * for that pairing, so the strip's own layout and a consumer holding the boxes separately (a control
     * strip stores a tabs row's per-tab segments as bare rectangles and re-pairs them at paint time) build
     * the same tabs. Zips to the shorter of the two lists, so a caller whose contents and boxes fall out of
     * step pairs only the tabs it can back with both rather than reading past either end.
     *
     * @param contents the tabs' contents, in row order
     * @param bounds   each tab's box, in the same order as {@code contents}
     * @return one {@link VanillaTab} per paired (content, box), in order
     */
    public static List<VanillaTab> zipTabs(
            List<VanillaTabContent> contents,
            List<Rectangle> bounds) {

        var count = Math.min(contents.size(), bounds.size());
        var tabs = new ArrayList<VanillaTab>(count);

        for (var index = 0; index < count; index++) {
            tabs.add(new VanillaTab(
                contents.get(index),
                bounds.get(index)));
        }
        return List.copyOf(tabs);
    }

    /**
     * The width the whole tab row spans, each tab snapped to its label-plus-shortcut width, by composing
     * each content's display string and delegating to the base {@link TabStrip#measureRowWidth}. Mirrors
     * {@link #layoutTabs}, which composes the same displays and lays the tabs out, so a host measures the
     * row through the same snap the layout later applies and the two cannot drift.
     *
     * @param contents the tabs' labels and optional shortcuts, in row order
     * @param spec     the tab-sizing rule, matching {@link #layoutTabs}
     * @param measurer measures each display string's rendered width
     * @return the summed snapped width of the row, or 0 for no contents
     */
    public static float measureRowWidth(
            List<VanillaTabContent> contents,
            SegmentSpec spec,
            LineWidthMeasurer measurer) {

        return TabStrip.measureRowWidth(
            composeDisplays(contents),
            spec,
            measurer);
    }

    /**
     * Each content's display string, in row order. The one place a run of tabs is turned into the text the
     * strip works against, so measuring a row and laying it out cannot disagree about what is being sized.
     *
     * @param contents the tabs' labels and optional shortcuts, in row order
     * @return one display string per content, in the same order
     */
    public static List<String> composeDisplays(List<VanillaTabContent> contents) {
        var displays = new ArrayList<String>(contents.size());
        for (var content : contents) {
            displays.add(composeDisplay(content));
        }
        return List.copyOf(displays);
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
    public static int findTabIndexAt(
            List<VanillaTab> tabs,
            float pointX,
            float pointY) {
                
        return Rectangles.findIndexContaining(
            tabs,
            VanillaTab::bounds,
            pointX,
            pointY);
    }

    /**
     * A tab's display string - "Label  (K)" when it has a shortcut, else just the label - so the
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
        return content.label()
            + SHORTCUT_GAP_TEXT
            + wrapShortcut(content.shortcut());
    }

    /**
     * A shortcut wrapped in its delimiters - "[K]" - the form the layout measures, kept here as the
     * single source so the measured display string and the paint pass never drift on the delimiter
     * convention. The paint pass draws the same three pieces ({@link #SHORTCUT_OPEN_DELIMITER}, the
     * key, {@link #SHORTCUT_CLOSE_DELIMITER}) as separate coloured segments rather than this one
     * string, so it builds off the same delimiters this composes from.
     *
     * @param shortcut the raw shortcut key name
     * @return the delimited shortcut
     */
    public static String wrapShortcut(String shortcut) {
        return SHORTCUT_OPEN_DELIMITER + shortcut + SHORTCUT_CLOSE_DELIMITER;
    }
}
