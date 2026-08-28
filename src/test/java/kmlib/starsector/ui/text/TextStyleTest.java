package kmlib.starsector.ui.text;

import com.fs.starfarer.api.util.Misc;

import kmlib.starsector.ui.font.StarsectorFont;
import kmlib.testfixtures.starsector.settings.StarsectorSettingsFake;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.awt.Color;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins {@link TextStyle}'s look model: the style a caller gets from a bare face carries the whole
 * baseline, and each refinement changes exactly its own part of it. The parts a caller never states are
 * the contract worth fixing - construction says only what differs, so every unstated part has to come out
 * of the baseline rather than out of whichever refinement happened to run last.
 */
class TextStyleTest {

    private static final StarsectorFont FACE = StarsectorFont.VANILLA_INSIGNIA_15;
    private static final Color BODY_TEXT_COLOUR = new Color(11, 22, 33);
    private static final Color OVERRIDE_COLOUR = new Color(200, 150, 50);
    private static final double OVERRIDE_SIZE = 24d;
    private static final double TOLERANCE = 0.001d;

    // The palette resolves from the running game, so the body-text colour every baseline style picks up
    // has to be stubbed rather than assumed. Installed for the whole class because the baseline is what
    // every refinement is measured against, so no group can build its subject without it.
    private MockedStatic<Misc> miscMock;

    private static TextStyle buildBaselineStyle() {
        return TextStyle.createStyle(FACE);
    }

    @BeforeEach
    void setUp() {

        // Misc.<clinit> reads from Global.getSettings(), so the no-op proxy must be in place before
        // Mockito's instrumentation triggers that class init.
        StarsectorSettingsFake.installSettings();

        miscMock = Mockito.mockStatic(Misc.class);
        miscMock
            .when(Misc::getTextColor)
            .thenReturn(BODY_TEXT_COLOUR);
    }

    @AfterEach
    void tearDown() {

        miscMock.close();
        StarsectorSettingsFake.clearSettings();
    }

    @Nested
    class CreateStyle {

        @Test
        void createStyleDrawsTheFaceAtItsNativeSize() {
            // A bitmap atlas is crisp at exactly one size, so naming a face alone has to land on that
            // size rather than on some style-local default the atlas would be scaled to.
            var style = buildBaselineStyle();

            assertThat(style.face().font())
                .isEqualTo(FACE);
            assertThat(style.face().size())
                .isCloseTo(FACE.getNativeSize(), within(TOLERANCE));
        }

        @Test
        void createStyleTakesTheBaselineForEveryPartTheCallerDidNotState() {

            var style = buildBaselineStyle();

            assertThat(style.colour())
                .isEqualTo(BODY_TEXT_COLOUR);
            assertThat(style.alignment())
                .isEqualTo(TextAlignment.TOP_LEFT);
            assertThat(style.isUpperCased())
                .isFalse();
        }

        @Test
        void createStyleResolvesTheColourFromTheLivePalette() {
            // Styles are built per paint precisely so a palette change - a player faction's recolour, a
            // theme swap - reaches the next style built rather than being frozen at class-load.
            var beforeRecolour = buildBaselineStyle();

            miscMock
                .when(Misc::getTextColor)
                .thenReturn(OVERRIDE_COLOUR);

            assertThat(beforeRecolour.colour())
                .isEqualTo(BODY_TEXT_COLOUR);
            assertThat(buildBaselineStyle().colour())
                .isEqualTo(OVERRIDE_COLOUR);
        }
    }

    @Nested
    class SizedAt {

        @Test
        void sizedAtChangesOnlyTheSize() {

            var style = buildBaselineStyle().sizedAt(OVERRIDE_SIZE);

            assertThat(style.face().size())
                .isCloseTo(OVERRIDE_SIZE, within(TOLERANCE));
            assertThat(style.face().font())
                .isEqualTo(FACE);
            assertThat(style.colour())
                .isEqualTo(BODY_TEXT_COLOUR);
            assertThat(style.alignment())
                .isEqualTo(TextAlignment.TOP_LEFT);
            assertThat(style.isUpperCased())
                .isFalse();
        }
    }

    @Nested
    class InColour {

        @Test
        void inColourChangesOnlyTheColour() {

            var style = buildBaselineStyle().inColour(OVERRIDE_COLOUR);

            assertThat(style.colour())
                .isEqualTo(OVERRIDE_COLOUR);
            assertThat(style.face())
                .isEqualTo(buildBaselineStyle().face());
            assertThat(style.alignment())
                .isEqualTo(TextAlignment.TOP_LEFT);
            assertThat(style.isUpperCased())
                .isFalse();
        }
    }

    @Nested
    class AlignedTo {

        @Test
        void alignedToChangesOnlyTheAlignment() {

            var style = buildBaselineStyle().alignedTo(TextAlignment.CENTER_RIGHT);

            assertThat(style.alignment())
                .isEqualTo(TextAlignment.CENTER_RIGHT);
            assertThat(style.face())
                .isEqualTo(buildBaselineStyle().face());
            assertThat(style.colour())
                .isEqualTo(BODY_TEXT_COLOUR);
            assertThat(style.isUpperCased())
                .isFalse();
        }
    }

    @Nested
    class InUpperCase {

        @Test
        void inUpperCaseChangesOnlyTheCasing() {

            var style = buildBaselineStyle().inUpperCase();

            assertThat(style.isUpperCased())
                .isTrue();
            assertThat(style.face())
                .isEqualTo(buildBaselineStyle().face());
            assertThat(style.colour())
                .isEqualTo(BODY_TEXT_COLOUR);
            assertThat(style.alignment())
                .isEqualTo(TextAlignment.TOP_LEFT);
        }

        @Test
        void inUpperCaseKeepsTheRefinementsMadeBeforeIt() {
            // The point of composing refinements: a heading built face-first cannot lose the size,
            // colour, and anchor it was already given by the one that shouts it.
            var style = buildBaselineStyle()
                .sizedAt(OVERRIDE_SIZE)
                .inColour(OVERRIDE_COLOUR)
                .alignedTo(TextAlignment.CENTER)
                .inUpperCase();

            assertThat(style.face().size())
                .isCloseTo(OVERRIDE_SIZE, within(TOLERANCE));
            assertThat(style.colour())
                .isEqualTo(OVERRIDE_COLOUR);
            assertThat(style.alignment())
                .isEqualTo(TextAlignment.CENTER);
            assertThat(style.isUpperCased())
                .isTrue();
        }

        @Test
        void inUpperCaseLeavesTheStyleItWasDerivedFromUnchanged() {
            // Role baselines are shared and derived from per call, so a caller that shouts one heading
            // must not have quietly shouted every other run built off the same baseline.
            var baseline = buildBaselineStyle();

            baseline.inUpperCase();

            assertThat(baseline.isUpperCased())
                .isFalse();
        }
    }

    @Nested
    class ResolveDisplayText {

        @Test
        void resolveDisplayTextReturnsTheTextAsAuthoredWhenNotUpperCased() {

            assertThat(buildBaselineStyle().resolveDisplayText("Contested by"))
                .isEqualTo("Contested by");
        }

        @Test
        void resolveDisplayTextShoutsWhenUpperCased() {

            assertThat(buildBaselineStyle().inUpperCase().resolveDisplayText("Contested by"))
                .isEqualTo("CONTESTED BY");
        }
    }
}
