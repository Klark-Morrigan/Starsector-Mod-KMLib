package kmlib.starsector.ui.render.gl.style;

import kmlib.starsector.ui.colour.AccentColours;
import kmlib.starsector.ui.font.StarsectorFont;
import kmlib.starsector.ui.sound.UiSoundScheme;
import kmlib.starsector.ui.widgets.tabs.style.TabStyle;
import kmlib.starsector.ui.widgets.tabs.style.TabStyles;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins {@link WidgetStyle}'s one refinement. A look is eight things a host resolved separately, so the
 * value of restating one of them in a method is that the other seven cannot be lost on the way - which is
 * exactly what a hand-written rebuild at a call site would eventually do, and what nothing but the screen
 * would report.
 */
final class WidgetStyleTest {

    // Shades far enough apart to read as themselves in a failure message. Nothing here is blended - the
    // refinement carries values over rather than combining them - so the colours only have to differ.
    private static final WidgetStyle STYLE = new WidgetStyle(
        new BoxColours(new Color(10, 0, 0), new Color(20, 0, 0)),
        new AccentColours(new Color(30, 0, 0), new Color(40, 0, 0), new Color(50, 0, 0)),
        new ControlHoverWash(new Color(60, 0, 0), 0.25f),
        new ControlPressLight(new Color(70, 0, 0), 0.5f),
        StarsectorFont.VANILLA_INSIGNIA_15,
        TabStyles.buildAtBandHeight(19f),
        new NotchColours(new Color(80, 0, 0), new Color(90, 0, 0)),
        UiSoundScheme.createSilentSoundScheme());

    // A band unlike the style's own, so a refinement that quietly kept the original reads as a failure
    // rather than as a value that happens to match.
    private static final TabStyle OTHER_TAB_STYLE = TabStyles.buildAtBandHeight(31f);

    @Nested
    class WithTabStyle {

        @Test
        void withTabStyleDrawsTabsInTheGivenStyle() {

            assertThat(STYLE.withTabStyle(OTHER_TAB_STYLE).tabStyle())
                .isEqualTo(OTHER_TAB_STYLE);
        }

        @Test
        void withTabStyleCarriesEverythingElseOver() {
            // What a panel drawing two tab-shaped things relies on: its band button is drawn through this
            // one look with only the tab style swapped, so anything else moving here would pitch the button
            // apart from the panel it belongs to.
            assertThat(STYLE.withTabStyle(OTHER_TAB_STYLE))
                .usingRecursiveComparison()
                .ignoringFields("tabStyle")
                .isEqualTo(STYLE);
        }

        @Test
        void withTabStyleLeavesTheStyleItWasAskedOfUntouched() {
            // A look is handed around a frame and read by several passes, so a refinement that edited in
            // place would change what every one of them was already holding.
            STYLE.withTabStyle(OTHER_TAB_STYLE);

            assertThat(STYLE.tabStyle())
                .isNotEqualTo(OTHER_TAB_STYLE);
        }
    }
}
