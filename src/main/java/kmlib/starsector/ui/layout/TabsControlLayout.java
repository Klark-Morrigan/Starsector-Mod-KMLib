package kmlib.starsector.ui.layout;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.controls.Control;
import kmlib.starsector.ui.controls.ControlSpec;
import kmlib.starsector.ui.controls.SegmentSizing;
import kmlib.starsector.ui.font.LineWidthMeasurer;
import kmlib.starsector.ui.widgets.segments.SegmentSpec;
import kmlib.starsector.ui.widgets.tabs.TabStyle;
import kmlib.starsector.ui.widgets.tabs.VanillaTabContent;
import kmlib.starsector.ui.widgets.tabs.VanillaTabStrip;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link ControlSpec.Tabs} row seen as the vanilla tab strip it is drawn as: how wide the row comes
 * out, which rectangle each tab is hit in, and where the row hangs when a panel flies it as a header.
 * It stands on its own because a tabs row is the one control whose dimensions come from somewhere else
 * entirely - every other control sizes to a body-font label inside a fixed-height row, while a tab snaps
 * to its label plus its shortcut hint, in a larger face, standing a tab band tall. So whatever stacks a
 * strip of controls stacks this row like any other and asks here how big it is.
 *
 * <p>All of it routes through {@link VanillaTabStrip}, so the tabs a KM panel lays out are laid out by
 * the same geometry as the ones it draws. What this adds is the reading of a {@link ControlSpec.Tabs}:
 * the strip geometry knows about tab contents and widths, not about the control that carries them, and
 * that gap is this class's whole contribution.
 *
 * <p>A tabs row reaches the screen two ways, and both come through here so a tab is hit exactly where it
 * is drawn either way. In a strip <strong>body</strong> it is an ordinary stacked control, measured and
 * split like the rest ({@link #measureRowWidth}, {@link #splitIntoSegments}). As a panel
 * <strong>header</strong> it is laid flush at the interior top, at the band height an injected
 * {@link TabStyle} states ({@link #layoutHeaderControl}). The two differ only in where the band hangs
 * and how tall it stands - never in how a tab within it snaps.
 *
 * <p>UI coordinates throughout (origin bottom-left, y grows up); text snapping runs through the injected
 * {@link LineWidthMeasurer}, so this stays a pure computation like the rest of the package.
 */
public final class TabsControlLayout {

    // Tabs-row geometry: a TABS row stands one tab-height tall (taller than a body row, since the tab
    // face is larger), each tab snapped to its label-plus-shortcut width with slack so text does not
    // touch the edges and a floor so a short tab still gives a clickable box, and measured at the tab
    // face size. Public so a host framing chrome around a tab row, or drawing into one, reads the same
    // values the tabs were snapped to. The height reads off the baseline dimension rather than
    // repeating its literal, so a body TABS row - which sizes itself and takes no injected style -
    // matches an unstyled header band.
    public static final float TAB_HEIGHT = TabStyle.DEFAULT_HEADER_BAND_HEIGHT;
    public static final float TAB_TEXT_PADDING = 16f;
    public static final float MIN_TAB_WIDTH = 48f;
    public static final double TAB_FONT_SIZE = 15d;

    private TabsControlLayout() {
    }

    /**
     * Turns a tabs control's parallel label and shortcut lists into the tab contents the shared strip
     * geometry measures and lays out; an empty shortcut reads as no hint ({@link VanillaTabStrip} drops a
     * blank shortcut from the composed display). Public so the renderer pairs each laid-out tab segment
     * with the same content this measured and split it under, keeping one source for the pairing.
     *
     * @param tabs the tabs control, its labels and per-tab shortcuts in row order
     * @return one {@link VanillaTabContent} per tab, in row order
     */
    public static List<VanillaTabContent> buildTabContents(ControlSpec.Tabs tabs) {
        var contents = new ArrayList<VanillaTabContent>(tabs.labels().size());
        for (var index = 0; index < tabs.labels().size(); index++) {
            contents.add(new VanillaTabContent(
                tabs.labels().get(index),
                tabs.shortcutAt(index)));
        }
        return contents;
    }

    /**
     * Lays a tabs control flush as a panel header: the tabs snapped to their labels from
     * {@code (originX, topY)} down the style's header band, split into per-tab segments. It takes no
     * body inset, unlike a control stacked inside a strip - a header sits flush at the interior top - so
     * a tab panel frames it directly under the border. The tabs snap and split through the same
     * measurement a body tabs row uses, so a header tab is hit exactly where a body tab would be and the
     * header is not bespoke tab-strip framing.
     *
     * <p>The band height arrives with the call rather than as a fixed constant, so two panels sharing
     * this one layout can still stand their tab rows at different heights.
     *
     * @param tabs     the tabs control, its labels and per-tab shortcuts in row order
     * @param originX  the header's left edge (the content inset), in UI coordinates
     * @param topY     the header's top edge (the content top), in UI coordinates
     * @param tabStyle the tab dimensions to lay to; its band height is the height every tab shares
     * @param measurer measures each tab label's rendered width for snapping
     * @return the laid-out tabs control, its bounds the header band and its segments split per tab
     */
    public static Control layoutHeaderControl(
            ControlSpec.Tabs tabs,
            float originX,
            float topY,
            TabStyle tabStyle,
            LineWidthMeasurer measurer) {

        var bandHeight = tabStyle.headerBandHeight();
        var bounds = new Rectangle(
            originX,
            topY - bandHeight,
            measureRowWidth(tabs, measurer),
            bandHeight);

        return new Control(
            tabs,
            bounds,
            splitIntoSegments(tabs, bounds, measurer));
    }

    /**
     * The width a tabs row needs: its tabs laid side by side, each snapped to its label-plus-shortcut
     * width through the shared {@link VanillaTabStrip} geometry - the same snap {@link #splitIntoSegments}
     * later applies - so the measured row is exactly as wide as the drawn tabs.
     *
     * @param tabs     the tabs control, its labels and per-tab shortcuts in row order
     * @param measurer measures each tab label's rendered width for snapping
     * @return the row width the tabs occupy side by side
     */
    static float measureRowWidth(ControlSpec.Tabs tabs, LineWidthMeasurer measurer) {
        return VanillaTabStrip.measureRowWidth(
            buildTabContents(tabs),
            buildSegmentSpec(),
            measurer);
    }

    /**
     * The per-tab hit segments of a tabs row, each snapped to its label-plus-shortcut width via the
     * shared {@link VanillaTabStrip} geometry, so the tabs are hit exactly where they are drawn. The tabs
     * fill the row they were laid into rather than a fixed height, so a header band standing at an
     * injected height splits into tabs of that height instead of segments floating loose in a taller or
     * shorter band.
     *
     * @param tabs     the tabs control, its labels and per-tab shortcuts in row order
     * @param row      the row the tabs were laid into - a stacked strip row, or a header band
     * @param measurer measures each tab label's rendered width for snapping
     * @return one hit rectangle per tab, left to right
     */
    static List<Rectangle> splitIntoSegments(
            ControlSpec.Tabs tabs,
            Rectangle row,
            LineWidthMeasurer measurer) {

        var laidOut = VanillaTabStrip.layoutTabs(
            row.x(),
            row.y() + row.height(),
            row.height(),
            buildSegmentSpec(),
            buildTabContents(tabs),
            measurer);

        var segments = new ArrayList<Rectangle>(laidOut.size());
        for (var tab : laidOut) {
            segments.add(tab.bounds());
        }
        return List.copyOf(segments);
    }

    // The segment-sizing rule for a tabs row: the tab padding, minimum, and tab font. A tabs row always
    // snaps each tab to its own label-plus-shortcut width, so this reads SNAPPED. One rule behind both
    // the width measurement and the segment split, so the two cannot size a tab differently.
    private static SegmentSpec buildSegmentSpec() {
        return new SegmentSpec(
            TAB_TEXT_PADDING,
            MIN_TAB_WIDTH,
            TAB_FONT_SIZE,
            SegmentSizing.SNAPPED);
    }
}
