package kmlib.starsector.ui.widgets.tabs.style;

import com.fs.starfarer.api.util.Misc;

import kmlib.math.geometry.Rectangle;
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
 * Pins {@link HotkeyStyle}: the two vanilla conventions differ where the hosts differ (one underlines
 * its key, the other does not) and the underline lands under the key it emphasises. The placement is the
 * part worth fixing - a line that misses its glyph reads as a stray mark rather than an emphasis, and
 * the style is the single source every renderer places it from.
 */
class HotkeyStyleTest {

    private static final Color GOLD = new Color(255, 200, 80);

    // A key glyph's drawn box: the standing numbers the placement is measured against, none of them
    // round, so an assertion cannot pass on a coincidence between two of them.
    private static final Rectangle KEY_BOX = new Rectangle(100f, 50f, 12f, 15f);
    private static final float TOLERANCE = 0.01f;

    // The settings key the factories light a bound key from - the role the engine's own buttons use for
    // their shortcuts, not the one it highlights prose with.
    private static final String SHORTCUT_COLOUR_KEY = "buttonShortcut";

    // What the stubbed settings answer that key with, swapped mid-test where a case is about the look
    // tracking a live palette rather than about its shape.
    private Color stubbedShortcutGold = GOLD;

    // The factories resolve the key's gold from the running game's palette, so it has to be stubbed
    // rather than assumed. Installed for the whole class: the geometry groups build their own literal
    // styles and simply do not read it.
    private MockedStatic<Misc> miscMock;

    @BeforeEach
    void setUp() {
        // Misc.<clinit> reads from Global.getSettings(), so the proxy must be in place before Mockito's
        // instrumentation triggers that class init.
        StarsectorSettingsFake.installSettings(
            StarsectorSettingsFake.EMPTY_STRINGS,
            key -> SHORTCUT_COLOUR_KEY.equals(key)
                ? stubbedShortcutGold
                : null);

        miscMock = Mockito.mockStatic(Misc.class);
    }

    @AfterEach
    void tearDown() {
        miscMock.close();
        StarsectorSettingsFake.clearSettings();
    }

    @Nested
    class CreateUnderlined {
        @Test
        void HotkeyStyle_createUnderlined_drawsALineUnderTheKeyInTheHighlightGold() {

            var style = HotkeyStyle.createUnderlined();

            assertThat(style.keyColour())
                .isEqualTo(GOLD);
            assertThat(style.isKeyUnderlined())
                .isTrue();
            assertThat(style.underlineThickness())
                .isGreaterThan(0f);
        }

        @Test
        void HotkeyStyle_createUnderlined_resolvesTheGoldFromTheLivePalette() {
            // The look is built per paint precisely so a palette change reaches the next style built
            // rather than being frozen at class-load.
            stubbedShortcutGold = new Color(10, 20, 30);

            assertThat(HotkeyStyle.createUnderlined().keyColour())
                .isEqualTo(stubbedShortcutGold);
        }
    }

    @Nested
    class CreatePlain {
        @Test
        void HotkeyStyle_createPlain_lightsTheKeyWithNoLineUnderIt() {
            // The intel screen's raised buttons leave their key bare, so this convention has to differ
            // from the map's in the emphasis alone - the gold is common to both.
            var style = HotkeyStyle.createPlain();

            assertThat(style.keyColour())
                .isEqualTo(GOLD);
            assertThat(style.isKeyUnderlined())
                .isFalse();
        }
    }

    @Nested
    class UnderlineThickness {
        @Test
        void HotkeyStyle_underlineThickness_floorsAtZeroWhenNegative() {
            // A negative thickness would grow the quad upward through the glyph it should sit under, so
            // it collapses to no line at all instead.
            var style = new HotkeyStyle(GOLD, true, -4f, 0f);

            assertThat(style.underlineThickness())
                .isCloseTo(0f, within(TOLERANCE));
        }
    }

    @Nested
    class ComputeUnderlineBox {
        @Test
        void HotkeyStyle_computeUnderlineBox_spansTheKeyBoxAndStandsTheStyledThickness() {
            // The line marks the key, so it takes the key's own span rather than the whole shortcut's -
            // the delimiters around it are label text and carry no emphasis.
            var box = new HotkeyStyle(GOLD, true, 2f, 3f)
                .computeUnderlineBox(KEY_BOX);

            assertThat(box.x())
                .isCloseTo(KEY_BOX.x(), within(TOLERANCE));
            assertThat(box.width())
                .isCloseTo(KEY_BOX.width(), within(TOLERANCE));
            assertThat(box.height())
                .isCloseTo(2f, within(TOLERANCE));
        }

        @Test
        void HotkeyStyle_computeUnderlineBox_hangsTheStyledGapBelowTheKeyBox() {
            // Measured downward from the box's bottom edge to the underline's top, so the whole line
            // clears the glyph rather than the gap being eaten by the underline's own thickness.
            var box = new HotkeyStyle(GOLD, true, 2f, 3f)
                .computeUnderlineBox(KEY_BOX);

            assertThat(box.y() + box.height())
                .isCloseTo(KEY_BOX.y() - 3f, within(TOLERANCE));
        }

        @Test
        void HotkeyStyle_computeUnderlineBox_liftsTheUnderlineIntoTheBoxOnANegativeGap() {
            // A drawn box hangs a descender below the baseline that an all-capitals key never reaches
            // into, so a negative gap is the honest way to tuck the underline up close under the glyph.
            var box = new HotkeyStyle(GOLD, true, 1f, -2f)
                .computeUnderlineBox(KEY_BOX);

            assertThat(box.y() + box.height())
                .isCloseTo(KEY_BOX.y() + 2f, within(TOLERANCE));
        }
    }
}
