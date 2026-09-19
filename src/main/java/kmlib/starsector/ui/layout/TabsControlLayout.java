package kmlib.starsector.ui.layout;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.controls.Control;
import kmlib.starsector.ui.controls.specs.SegmentSizing;
import kmlib.starsector.ui.controls.specs.TabsSpec;
import kmlib.starsector.ui.font.LineWidthMeasurer;
import kmlib.starsector.ui.widgets.segments.SegmentSpec;
import kmlib.starsector.ui.widgets.tabs.VanillaTabContent;
import kmlib.starsector.ui.widgets.tabs.VanillaTabStrip;
import kmlib.starsector.ui.widgets.tabs.style.TabBox;
import kmlib.starsector.ui.widgets.tabs.style.TabStyle;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link TabsSpec} row seen as the vanilla tab strip it is drawn as: how wide the row comes
 * out, which rectangle each tab is hit in, and where the row hangs when a panel flies it as a header.
 * It stands on its own because a tabs row is the one control whose dimensions come from somewhere else
 * entirely - every other control sizes to a body-font label inside a fixed-height row, while a tab is
 * sized in a larger face and stands a tab band tall, and may not be sized by its label at all. So
 * whatever stacks a strip of controls stacks this row like any other and asks here how big it is.
 *
 * <p>All of it routes through {@link VanillaTabStrip}, so the tabs a KM panel lays out are laid out by
 * the same geometry as the ones it draws. What this adds is the reading of a {@link TabsSpec}:
 * the strip geometry knows about tab contents and widths, not about the control that carries them, and
 * that gap is this class's whole contribution.
 *
 * <p>A tabs row reaches the screen two ways, and both come through here so a tab is hit exactly where it
 * is drawn either way. In a strip <strong>body</strong> it is an ordinary stacked control, measured and
 * split like the rest ({@link #measureRowWidth}, {@link #splitIntoSegments}). As a panel
 * <strong>header</strong> it is laid flush at the interior top, at the band height and in the face and box
 * an injected {@link TabStyle} states ({@link #layoutHeaderControl}). The two differ in where the band
 * hangs, how tall it stands, what it is measured in, and - since a header may state a box of its own -
 * whether its tabs are sized by their labels at all. What they never differ in is the rule applied: both
 * compose one {@link SegmentSpec} and hand that same value to the measurement and to the split, so a row
 * cannot be measured under one rule and cut under another.
 *
 * <p>UI coordinates throughout (origin bottom-left, y grows up); text snapping runs through the injected
 * {@link LineWidthMeasurer}, so this stays a pure computation like the rest of the package.
 */
public final class TabsControlLayout {

    // Tabs-row geometry: a TABS row stands one tab-height tall (taller than a body row, since the tab
    // face is larger), each tab snapped to its label-plus-shortcut width with slack so text does not
    // touch the edges and a floor so a short tab still gives a clickable box. Public so a host framing
    // chrome around a tab row, or drawing into one, reads the same values the tabs were snapped to. The
    // height and the font size are both baselines for the unstyled body row - a styled header snaps to
    // the band and the face its own style names - so a body TABS row, which sizes itself and takes no
    // injected style, matches an unstyled header band.
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
    public static List<VanillaTabContent> buildTabContents(TabsSpec tabs) {
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
            TabsSpec tabs,
            float originX,
            float topY,
            TabStyle tabStyle,
            LineWidthMeasurer measurer) {

        // Sized at the size and in the box the style states, not at the baselines below: a header wears
        // its host's look, so two hosts differing in either size their tabs to what each will actually
        // draw. The measurement handed in is already bound to that style's face; taking the size off the
        // same value is what stops a tab being sized in one face and lettered in another.
        //
        // Built once and handed to both passes rather than composed in each: the row's width and its
        // per-tab split are the same rule read twice, and two builds are two chances for them to differ.
        var sizing = buildSegmentSpec(tabStyle.face().size(), tabStyle.tabBox());
        var bandHeight = tabStyle.headerBandHeight();
        var bounds = new Rectangle(
            originX,
            topY - bandHeight,
            measureRowWidth(tabs, measurer, sizing),
            bandHeight);

        return new Control(
            tabs,
            bounds,
            splitIntoSegments(tabs, bounds, measurer, sizing, tabStyle.tabBox()));
    }

    /**
     * The width a tabs row needs: its tabs laid side by side, each snapped to its label-plus-shortcut
     * width through the shared {@link VanillaTabStrip} geometry - the same snap {@link #splitIntoSegments}
     * later applies - so the measured row is exactly as wide as the drawn tabs.
     *
     * <p>Measured at the baseline tab size and snapped to its labels, this being the body path: a tabs
     * row stacked inside a strip carries no style, so it has nothing to name a size or a box of its own
     * with - the same reason it stands at {@link #TAB_HEIGHT}.
     *
     * @param tabs     the tabs control, its labels and per-tab shortcuts in row order
     * @param measurer measures each tab label's rendered width for snapping
     * @return the row width the tabs occupy side by side
     */
    static float measureRowWidth(TabsSpec tabs, LineWidthMeasurer measurer) {
        return measureRowWidth(tabs, measurer, buildSegmentSpec(TAB_FONT_SIZE, TabBox.SNAPPED));
    }

    /**
     * The per-tab hit segments of a tabs row, each snapped to its label-plus-shortcut width via the
     * shared {@link VanillaTabStrip} geometry, so the tabs are hit exactly where they are drawn. The tabs
     * fill the row they were laid into rather than a fixed height, so a header band standing at an
     * injected height splits into tabs of that height instead of segments floating loose in a taller or
     * shorter band.
     *
     * <p>Snapped at the baseline tab size, and filling its row rather than standing a box of its own, for
     * the same reason {@link #measureRowWidth} is - this is the body path, which carries no style.
     *
     * @param tabs     the tabs control, its labels and per-tab shortcuts in row order
     * @param row      the row the tabs were laid into - a stacked strip row, or a header band
     * @param measurer measures each tab label's rendered width for snapping
     * @return one hit rectangle per tab, left to right
     */
    static List<Rectangle> splitIntoSegments(
            TabsSpec tabs,
            Rectangle row,
            LineWidthMeasurer measurer) {
        return splitIntoSegments(
            tabs,
            row,
            measurer,
            buildSegmentSpec(TAB_FONT_SIZE, TabBox.SNAPPED),
            TabBox.SNAPPED);
    }

    // The segment-sizing rule for a tabs row: the tab padding, minimum, and the size the labels are
    // measured at, under whichever width rule the box states. A box with a width of its own reads FIXED
    // and measures no label; a snapped box keeps the label-plus-shortcut snap a body row has always
    // taken. One rule behind both the width measurement and the segment split, so the two cannot size a
    // tab differently.
    private static SegmentSpec buildSegmentSpec(double fontSize, TabBox tabBox) {
        return new SegmentSpec(
            TAB_TEXT_PADDING,
            MIN_TAB_WIDTH,
            fontSize,
            tabBox.isFixedWidth() ? SegmentSizing.FIXED : SegmentSizing.SNAPPED,
            tabBox.width(),
            tabBox.neighbourGap());
    }

    // The row width under a given sizing rule. The rule is a parameter rather than a constant because a
    // header takes its host's face and box while a body row takes the baseline and snaps; both go through
    // this one call, so a header and a body row cannot come to size a tab differently.
    private static float measureRowWidth(
            TabsSpec tabs,
            LineWidthMeasurer measurer,
            SegmentSpec sizing) {

        return VanillaTabStrip.measureRowWidth(
            buildTabContents(tabs),
            sizing,
            measurer);
    }

    // The hit segments under the same sizing rule - the split half of the measurement above, handed the
    // very rule that measured so a row is split exactly as it was sized.
    //
    // The box comes alongside for its height alone, the widths being the sizing's: tabs hang from the
    // band's top at the box's own height, so a box shorter than its band leaves the remainder below it -
    // the pixel the engine's own map row keeps for the line its tabs stand on.
    private static List<Rectangle> splitIntoSegments(
            TabsSpec tabs,
            Rectangle row,
            LineWidthMeasurer measurer,
            SegmentSpec sizing,
            TabBox tabBox) {

        var laidOut = VanillaTabStrip.layoutTabs(
            row.x(),
            row.y() + row.height(),
            tabBox.resolveTabHeight(row.height()),
            sizing,
            buildTabContents(tabs),
            measurer);

        var segments = new ArrayList<Rectangle>(laidOut.size());
        for (var tab : laidOut) {
            segments.add(tab.bounds());
        }
        return List.copyOf(segments);
    }
}
