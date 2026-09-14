package kmlib.starsector.ui.render.gl.controls;

import kmlib.starsector.ui.widgets.PanelAlpha;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins which of a panel's two alpha channels each half of a control draws on: its chrome honours the
 * look's translucency as well as the moment, while its words honour the moment alone.
 *
 * <p>The distinction is invisible on a panel at rest and fully opaque, which is most frames, so nothing
 * about a wrong reading shows until a player turns the body down - and then it shows as text that has
 * quietly lost the colour it was carrying rather than as anything that looks like a fault. Read from
 * one paint here, since a paint answering both from the same number is exactly the mistake worth
 * catching.
 */
final class ControlPaintTest {

    private static final float TOLERANCE = 0.0001f;

    // A body turned well down, so a channel that wrongly honoured it lands nowhere near one that did not.
    private static final float BODY_OPACITY = 0.5f;

    // A panel partway out of the frame, which both channels honour.
    private static final float PANEL_FADE = 0.4f;

    // The look and the cell treatments a case is not about; neither channel is read off them.
    private static final ControlPaint PAINT = new ControlPaint(
        null,
        new PanelAlpha(BODY_OPACITY, PANEL_FADE),
        null);

    @Nested
    class ChromeOpacity {

        @Test
        void chromeOpacityHonoursTheLooksTranslucencyAndTheMoment() {
            // Chrome is the body: a see-through panel's fills and frames are what the player asked to
            // see through, so they take both channels compounded.
            assertThat(PAINT.chromeOpacity())
                .isCloseTo(0.2f, within(TOLERANCE));
        }
    }

    @Nested
    class TextOpacity {

        @Test
        void textOpacityHonoursTheMomentAloneSoAReadingKeepsItsColour() {
            // Words opt out of the look's translucency: the cost of fading them falls hardest on text
            // carrying a colour of its own, which gives up that colour toward the backdrop while the
            // greys it is meant to be told apart from barely move.
            assertThat(PAINT.textOpacity())
                .isCloseTo(0.4f, within(TOLERANCE));
        }

        @Test
        void textOpacityStillLeavesWithThePanel() {
            // The half it must not drop. Text held at full strength over a dissolving body would hang
            // there, which reads worse than not fading at all.
            var leaving = new ControlPaint(null, new PanelAlpha(1f, 0f), null);

            assertThat(leaving.textOpacity())
                .isCloseTo(0f, within(TOLERANCE));
        }

        @Test
        void textOpacityMatchesTheChromeOnAnOpaqueBody() {
            // The frames that hide a wrong reading: with nothing to be see-through about, both channels
            // answer the same number, which is why a case at rest proves nothing on its own.
            var opaqueBody = new ControlPaint(null, new PanelAlpha(1f, PANEL_FADE), null);

            assertThat(opaqueBody.textOpacity())
                .isCloseTo(opaqueBody.chromeOpacity(), within(TOLERANCE));
        }
    }
}
