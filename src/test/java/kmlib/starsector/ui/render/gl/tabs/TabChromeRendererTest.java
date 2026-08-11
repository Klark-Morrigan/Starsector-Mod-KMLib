package kmlib.starsector.ui.render.gl.tabs;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.widgets.tabs.TabLightSource;
import kmlib.starsector.ui.widgets.tabs.TabLookSource;
import kmlib.starsector.ui.widgets.tabs.TabPaintSources;
import kmlib.starsector.ui.widgets.tabs.TabWashSource;
import kmlib.starsector.ui.widgets.tabs.VanillaTab;
import kmlib.starsector.ui.widgets.tabs.VanillaTabContent;
import kmlib.starsector.ui.widgets.tabs.style.TabChrome;
import kmlib.starsector.ui.widgets.tabs.style.TabLight;
import kmlib.starsector.ui.widgets.tabs.style.TabLook;
import kmlib.starsector.ui.widgets.tabs.style.TabPaint;
import kmlib.starsector.ui.widgets.tabs.style.TabWash;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the two things {@link TabChromeRenderer} decides for every chrome alike, neither of which reaches
 * the screen: which painter a {@link TabChrome} binds to, and the order the two paint channels compose in.
 * The chromes themselves are GL passthrough and exercised in-engine; what is testable here is exactly what
 * they were built not to decide for themselves.
 */
final class TabChromeRendererTest {

    // Two resting looks and a lift target, all distinct and none a blend of any other, so a tab painted
    // from the wrong channel or the wrong index cannot land on an expected value by coincidence.
    private static final Color FIRST_FILL = new Color(10, 20, 30);
    private static final Color FIRST_LABEL = new Color(40, 50, 60);
    private static final Color SECOND_FILL = new Color(70, 80, 90);
    private static final Color SECOND_LABEL = new Color(100, 110, 120);
    private static final Color LIFT_TARGET = new Color(200, 210, 220);

    private static final TabLook FIRST_LOOK = new TabLook(FIRST_FILL, FIRST_LABEL);
    private static final TabLook SECOND_LOOK = new TabLook(SECOND_FILL, SECOND_LABEL);

    // A lift that moves nothing and one that arrives fully at its target: the two ends, so an assertion
    // reads a named colour rather than a blend the test would have to work out for itself.
    private static final TabWash NO_LIFT = new TabWash(LIFT_TARGET, 0f);
    private static final TabWash FULL_LIFT = new TabWash(LIFT_TARGET, 1f);

    private static final List<VanillaTab> TWO_TABS = List.of(
        new VanillaTab(new VanillaTabContent("First", null), new Rectangle(0f, 0f, 40f, 19f)),
        new VanillaTab(new VanillaTabContent("Second", null), new Rectangle(40f, 0f, 40f, 19f)));

    // The looks the row reports, by index, so a walk out of order paints a tab in its neighbour's shade.
    private static final TabLookSource TWO_LOOKS =
        tabIndex -> tabIndex == 0 ? FIRST_LOOK : SECOND_LOOK;

    @Nested
    class PaintEachTab {

        @Test
        void TabChromeRenderer_paintEachTab_paintsEachTabInTheLookItsSourceReports() {
            // The resting case: no tab is lifted, so every tab has to arrive at exactly the shade its look
            // source named rather than at something a zero-strength blend nudged.
            var painted = paintAll(TWO_LOOKS, tabIndex -> NO_LIFT);

            assertThat(painted.get(0).paint().look())
                .isEqualTo(FIRST_LOOK);
            assertThat(painted.get(1).paint().look())
                .isEqualTo(SECOND_LOOK);
        }

        @Test
        void TabChromeRenderer_paintEachTab_liftsTheSettledLookRatherThanReplacingIt() {
            // The order the whole seam exists to fix: the look settles first and the lift is layered over
            // what it yields. At full depth both of a tab's colours land on the lift's target, and fill and
            // label move together - a lift that moved only one would part the text from the tab under it.
            var painted = paintAll(TWO_LOOKS, tabIndex -> FULL_LIFT);

            assertThat(painted.get(0).paint().look())
                .isEqualTo(new TabLook(LIFT_TARGET, LIFT_TARGET));
        }

        @Test
        void TabChromeRenderer_paintEachTab_walksTheRowInOrder() {
            // A chrome draws into the box it is handed, so a walk that reordered the row would paint each
            // tab's chrome over its neighbour's while the hit boxes stayed put.
            var painted = paintAll(TWO_LOOKS, tabIndex -> NO_LIFT);

            assertThat(painted)
                .extracting(painting -> painting.tab().content().label())
                .containsExactly("First", "Second");
        }

        @Test
        void TabChromeRenderer_paintEachTab_handsEachTabItsPlaceInTheRow() {
            // A chrome can place a button from its position - the raised-button row parts every button from
            // the one on its left and exempts the leading one - so the position handed over has to be the
            // tab's own place counted from the left rather than any other numbering.
            var painted = paintAll(TWO_LOOKS, tabIndex -> NO_LIFT);

            assertThat(painted)
                .extracting(Painting::rowIndex)
                .containsExactly(0, 1);
        }

        @Test
        void TabChromeRenderer_paintEachTab_paintsNothingForAnEmptyRow() {
            // A panel can carry a bandless header, and a chrome handed no tabs must draw no chrome rather
            // than reaching for a first tab that is not there.
            assertThat(paintAll(List.of(), TWO_LOOKS, tabIndex -> NO_LIFT))
                .isEmpty();
        }
    }

    @Nested
    class ComputePaintedRegionFor {

        // A band with no round relationship between its corner and its extents, so a region grown on the
        // wrong side cannot land on the expected rectangle by coincidence.
        private static final Rectangle BAND = new Rectangle(100f, 50f, 63f, 18f);

        @Test
        void TabChromeRenderer_computePaintedRegionFor_keepsAStripWithinABandThatHousesItsBaseline() {
            // 17-tall tabs in this 18 band leave the row a pixel under them to rule its baseline in, so the
            // strip paints no further than the band it was given - growing it would let a fold's wipe leave
            // a sliver of row standing past the edge it wiped to.
            assertThat(TabChromeRenderer.computePaintedRegionFor(TabChrome.STRIP, BAND, 17f))
                .isEqualTo(BAND);
        }

        @Test
        void TabChromeRenderer_computePaintedRegionFor_admitsAStripsBaselineBelowFullHeightTabs() {
            // Tabs filling their band leave nowhere inside it for the line they stand on, so the row reaches
            // one below. Clipped to the band alone the strip would come out with no baseline at all, which
            // reads as a row that forgot its anchor rather than as a clip that ate one.
            assertThat(TabChromeRenderer.computePaintedRegionFor(TabChrome.STRIP, BAND, 18f))
                .isEqualTo(new Rectangle(100f, 49f, 63f, 19f));
        }

        @Test
        void TabChromeRenderer_computePaintedRegionFor_admitsTheRaisedButtonsReachPastTheBand() {
            // The raised buttons lay their left and bottom borders on the lines the panel already draws,
            // which are a hairline outside the band; a caller clipping to the band would crop exactly those
            // two borders and nothing would report it. Its tab height is not asked of it - this chrome's
            // reach is its frame's, whatever height its buttons stand at.
            assertThat(TabChromeRenderer.computePaintedRegionFor(TabChrome.RAISED_BUTTON, BAND, 18f))
                .isEqualTo(new Rectangle(99f, 49f, 64f, 19f));
        }
    }

    @Nested
    class ResolvedLights {

        @Test
        void TabChromeRenderer_paintEachTab_handsEachTabTheLightItsSourceReports() {
            // The third channel, and the one drawn after everything else: a chrome cannot lay light over a
            // finished tab it was never handed, so the walk has to carry it per tab exactly as it carries
            // the look.
            var lit = new TabLight(LIFT_TARGET, 0.25f);
            var painted = paintAll(
                TWO_TABS,
                new TabPaintSources(TWO_LOOKS, tabIndex -> NO_LIFT, tabIndex -> lit));

            assertThat(painted)
                .extracting(painting -> painting.paint().light())
                .containsExactly(lit, lit);
        }

        @Test
        void TabChromeRenderer_paintEachTab_leavesTheLookAloneWhereTheLightIsTheChannelMoving() {
            // The two channels do not compose: light is added over what the look painted, so a lit tab's
            // look has to arrive exactly as its source reported it. Blending the two here would put the
            // light on twice, once mixed in and once added.
            var painted = paintAll(
                TWO_TABS,
                new TabPaintSources(TWO_LOOKS, tabIndex -> NO_LIFT, tabIndex -> new TabLight(LIFT_TARGET, 1f)));

            assertThat(painted.get(0).paint().look())
                .isEqualTo(FIRST_LOOK);
        }
    }

    @Nested
    class ResolveRendererFor {

        @Test
        void TabChromeRenderer_resolveRendererFor_bindsEveryChromeToAPainterOfItsOwn() {
            // The one place a look is bound to a pass, and a copy-pasted case would silently draw the
            // intel screen's tabs as the map's - a mismatch nothing else in the build would catch.
            var strip = TabChromeRenderer.resolveRendererFor(TabChrome.STRIP);
            var raisedButton = TabChromeRenderer.resolveRendererFor(TabChrome.RAISED_BUTTON);

            assertThat(strip)
                .isNotNull();
            assertThat(raisedButton)
                .isNotNull()
                .isNotSameAs(strip);
        }
    }

    // Runs the walk over the standing two-tab row, recording what each tab was handed.
    private static List<Painting> paintAll(TabLookSource looks, TabWashSource washes) {
        return paintAll(TWO_TABS, looks, washes);
    }

    // The same, over a caller's own row, for the cases that are about the row rather than the looks.
    private static List<Painting> paintAll(
            List<VanillaTab> tabs,
            TabLookSource looks,
            TabWashSource washes) {

        return paintAll(tabs, new TabPaintSources(looks, washes, TabLightSource.createUnlitSource()));
    }

    // The same again over a caller's own sources, for the cases about a channel other than the look.
    private static List<Painting> paintAll(List<VanillaTab> tabs, TabPaintSources sources) {

        var painted = new ArrayList<Painting>();
        TabChromeRenderer.paintEachTab(
            tabs,
            sources,
            (rowIndex, tab, paint) -> painted.add(new Painting(rowIndex, tab, paint)));
        return painted;
    }

    // One call the walk made: which tab was handed over, where in the row it stood, and the paint it took.
    private record Painting(int rowIndex, VanillaTab tab, TabPaint paint) {
    }
}
