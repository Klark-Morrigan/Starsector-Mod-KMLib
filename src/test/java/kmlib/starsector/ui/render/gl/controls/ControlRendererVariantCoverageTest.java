package kmlib.starsector.ui.render.gl.controls;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.colour.AccentColours;
import kmlib.starsector.ui.controls.Control;
import kmlib.starsector.ui.controls.ControlInteractionSources;
import kmlib.starsector.ui.controls.ControlSpecSamples;
import kmlib.starsector.ui.controls.specs.ControlSpec;
import kmlib.starsector.ui.font.StarsectorFont;
import kmlib.starsector.ui.font.StripTextMeasurers;
import kmlib.starsector.ui.layout.ControlStripLayout;
import kmlib.starsector.ui.render.gl.TriangleRenderer;
import kmlib.starsector.ui.render.gl.style.BoxColours;
import kmlib.starsector.ui.render.gl.style.ControlHoverWash;
import kmlib.starsector.ui.render.gl.style.ControlPressLight;
import kmlib.starsector.ui.render.gl.style.NotchColours;
import kmlib.starsector.ui.render.gl.style.WidgetStyle;
import kmlib.starsector.ui.render.gl.tabs.RaisedButtonTabStripRenderer;
import kmlib.starsector.ui.render.gl.tabs.VanillaTabStripRenderer;
import kmlib.starsector.ui.sound.UiSoundScheme;
import kmlib.starsector.ui.widgets.PanelAlpha;
import kmlib.starsector.ui.widgets.tabs.style.TabStyles;
import kmlib.testfixtures.starsector.ui.font.LineWidthMeasurerFake;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.stubbing.Answer;

import java.awt.Color;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

/**
 * Holds the one rule that must be true of every control the renderer can be handed: that being
 * handed it results in something being painted.
 *
 * <p>The paint pass answers each variant through a chain of type tests with no final branch, so a
 * variant nobody added to it is measured, placed, hit-tested and then silently not drawn. Nothing
 * fails, and the control is simply missing from the panel. This is what fails instead, and it fails
 * for a variant nobody wrote a case for, being stated over the sample catalogue rather than over a
 * list of kinds.
 *
 * <p>What counts as painting is any call reaching one of the widget passes the renderer composes,
 * each of which is stood in for here. Which pass a variant reaches is deliberately not asserted:
 * that is the renderer's own composition and is free to change, where "it drew something" is the
 * contract a player would find broken.
 *
 * <p>The tab chrome is reached through a resolver that answers with one of two passes, so both of
 * those are stood in rather than the resolver, which would otherwise answer with nothing to call.
 *
 * <p>Groups are left out. The layout expands one into the controls it holds, so the renderer is
 * never handed one, and a rule that a group paints nothing would pass for a group the renderer
 * forgot as readily as for one it never sees.
 */
final class ControlRendererVariantCoverageTest {

    private static final float WIDTH_PER_CHAR = 7f;
    private static final float TAB_WIDTH_PER_CHAR = 9f;
    private static final float TAB_BAND_HEIGHT = 19f;

    // A look built from plain values: nothing the paint passes are asked for here depends on which
    // shades they were handed, only on having been asked at all.
    private static final WidgetStyle STYLE = new WidgetStyle(

        new BoxColours(new Color(10, 0, 0), new Color(20, 0, 0)),
        new AccentColours(new Color(30, 0, 0), new Color(40, 0, 0), new Color(50, 0, 0)),
        new ControlHoverWash(new Color(60, 0, 0), 0.25f),
        new ControlPressLight(new Color(70, 0, 0), 0.5f),

        StarsectorFont.VANILLA_INSIGNIA_15,
        TabStyles.buildAtBandHeight(TAB_BAND_HEIGHT),

        new NotchColours(new Color(80, 0, 0), new Color(90, 0, 0)),

        UiSoundScheme.createSilentSoundScheme());

    private static final PanelAlpha FULLY_PRESENT = new PanelAlpha(1f, 1f);

    private final StripTextMeasurers measurersFake = new StripTextMeasurers(

        new LineWidthMeasurerFake(TAB_WIDTH_PER_CHAR),
        new LineWidthMeasurerFake(WIDTH_PER_CHAR));

    @Nested
    class Render {

        @Test
        void paintsSomethingForEveryVariantItCanBeHanded() {

            ControlSpecSamples.mapSamplesByVariant().forEach((variant, spec) -> {
                if (ControlSpecSamples.holdsChildSpecs(variant)) {
                    return;
                }
                assertThat(countPaintCallsFor(spec))
                    .as("%s paints something", variant.getSimpleName())
                    .isPositive();
            });
        }
    }

    // Lays the spec out by itself and draws it, with every widget pass the renderer composes stood in
    // and counting rather than drawing, so the count is what the pass would have put on screen.
    private int countPaintCallsFor(ControlSpec spec) {

        var control = layOutAlone(spec);
        var paintCalls = new int[1];
        Answer<Object> countThenAnswerAsUsual = invocation -> {
            paintCalls[0]++;
            return Answers.RETURNS_DEFAULTS.answer(invocation);
        };

        try (var checkboxMock = mockStatic(CheckboxRenderer.class, countThenAnswerAsUsual);
                var toggleMock = mockStatic(ToggleButton.class, countThenAnswerAsUsual);
                var dividerMock = mockStatic(DividerRenderer.class, countThenAnswerAsUsual);
                var radioRowMock = mockStatic(RadioRowRenderer.class, countThenAnswerAsUsual);
                var iconListMock = mockStatic(IconRadioListRenderer.class, countThenAnswerAsUsual);
                var labelMock = mockStatic(ControlLabelRenderer.class, countThenAnswerAsUsual);
                var triangleMock = mockStatic(TriangleRenderer.class, countThenAnswerAsUsual);
                var tabStripMock = mockStatic(VanillaTabStripRenderer.class, countThenAnswerAsUsual);
                var tabButtonsMock = mockStatic(RaisedButtonTabStripRenderer.class, countThenAnswerAsUsual)) {

            ControlRenderer.render(control, STYLE, FULLY_PRESENT, ControlInteractionSources.RESTING);
        }
        return paintCalls[0];
    }

    // One control laid out on its own, which is what the renderer is handed per control anyway. Placed
    // rather than built by hand so its segments are the ones the layout actually splits, a radio drawn
    // over segments it was never given being a different failure from the one under test.
    private Control layOutAlone(ControlSpec spec) {

        var measurement = ControlStripLayout.measureStrip(List.of(spec), measurersFake);
        var body = new Rectangle(0f, 0f, measurement.bodyWidth(), measurement.bodyHeight());

        return ControlStripLayout
            .layoutControls(body, List.of(spec), measurement, measurersFake)
            .get(0);
    }
}
