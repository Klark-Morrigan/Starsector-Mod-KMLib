package kmlib.starsector.ui.widgets.tabs;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.controls.Control;
import kmlib.starsector.ui.controls.ControlAction;
import kmlib.starsector.ui.controls.ControlSpec;
import kmlib.starsector.ui.text.ImageSpan;
import kmlib.starsector.ui.widgets.tabs.style.TabHover;
import kmlib.starsector.ui.widgets.tabs.style.TabLook;
import kmlib.starsector.ui.widgets.tabs.style.TabPalette;
import kmlib.starsector.ui.widgets.tabs.style.TabStyles;
import kmlib.starsector.ui.widgets.tabs.style.TabWash;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the shade a band button's mark is drawn in. The mark fills the button's box, so the chrome that
 * would otherwise show the pointer is covered by it - which makes this colour the whole of what the
 * control has to answer with, and a button whose mark stood still under the pointer would sound and show
 * nothing.
 *
 * <p>Both hover rules are asked, since the two chromes answer the pointer differently - one travels to a
 * named shade and the other lights from where it stands - and a mark reading only the first would sit
 * inert on every panel wearing the second.
 */
final class BandButtonPlacementTest {

    private static final float UNHOVERED = 0f;
    private static final float FULLY_HOVERED = 1f;

    // Widely-spaced, single-channel shades, so a resolved colour names the role it came from rather than
    // being a number two roles could both have produced.
    private static final Color SETTLED_LABEL = new Color(60, 0, 0);
    private static final Color HOVERED_LABEL = new Color(200, 0, 0);

    // The shown look, which a band button never wears: it opens something rather than selecting anything,
    // so a mark arriving at this shade is a look channel that read the button as a tab.
    private static final Color UNREACHABLE_SHOWN_LABEL = new Color(0, 255, 0);

    // Any fill: the mark covers it, so no assertion here reads one.
    private static final Color UNREAD_FILL = Color.BLACK;

    // The momentary lift, at no strength: a band button carries no pulse, so a lift of any strength here
    // would be colouring these assertions with a channel the button never runs.
    private static final TabWash NO_LIFT = new TabWash(Color.WHITE, 0f);

    private static final Color STAND_IN_ACCENT = new Color(0, 0, 255);
    private static final Color STAND_IN_BACKING = Color.BLACK;

    // The strip's rule: every tab under the pointer travels to one named shade.
    private static final TabPalette MEETING_SHADE_PALETTE = new TabPalette(
        STAND_IN_ACCENT,
        STAND_IN_BACKING,
        new TabLook(UNREAD_FILL, SETTLED_LABEL),
        new TabLook(UNREAD_FILL, UNREACHABLE_SHOWN_LABEL),
        new TabHover.MeetingShade(new TabLook(UNREAD_FILL, HOVERED_LABEL)),
        NO_LIFT);

    // The raised button's rule: the pointer adds light to whatever the button already wears, leaving its
    // settled look where it stood. Half strength, so a mark reading the light at all reads it in full.
    private static final Color GLOW_COLOUR = new Color(200, 0, 0);
    private static final float GLOW_AMOUNT = 0.5f;

    private static final TabPalette ADDED_GLOW_PALETTE = new TabPalette(
        STAND_IN_ACCENT,
        STAND_IN_BACKING,
        new TabLook(UNREAD_FILL, new Color(0, 0, 100)),
        new TabLook(UNREAD_FILL, UNREACHABLE_SHOWN_LABEL),
        new TabHover.AddedGlow(GLOW_COLOUR, GLOW_AMOUNT),
        NO_LIFT);

    private static final float BAND_HEIGHT = 19f;

    private static final Rectangle BUTTON_BOX = new Rectangle(0f, 0f, 20f, BAND_HEIGHT);

    private static final ImageSpan UNTINTED_ICON = new ImageSpan("graphics/icons/x.png");

    @Nested
    class ResolveIconTint {

        @Test
        void resolveIconTintLeavesTheMarkOnTheSettledShadeUnhovered() {
            // The resting shade rather than the shown one: the button states no selection, so a mark that
            // read it as the lit cell would stand permanently lit beside tabs it is not one of.
            assertThat(buildPlacement(MEETING_SHADE_PALETTE, UNTINTED_ICON).resolveIconTint(UNHOVERED))
                .isEqualTo(new Color(60, 0, 0));
        }

        @Test
        void resolveIconTintBringsTheMarkToTheHoveredShade() {

            assertThat(buildPlacement(MEETING_SHADE_PALETTE, UNTINTED_ICON).resolveIconTint(FULLY_HOVERED))
                .isEqualTo(new Color(200, 0, 0));
        }

        @Test
        void resolveIconTintLightsTheMarkOnAPaletteThatGlowsRatherThanTravels() {
            // The rule the intel screen's buttons answer by. It leaves the settled look exactly where it
            // stood and reports its light separately, so a mark reading the look alone would be a button
            // that never lights on half the panels this is drawn on. 100 = 0 + 200 * 0.5.
            assertThat(buildPlacement(ADDED_GLOW_PALETTE, UNTINTED_ICON).resolveIconTint(FULLY_HOVERED))
                .isEqualTo(new Color(100, 0, 100));
        }

        @Test
        void resolveIconTintLeavesAGlowingPalettesMarkAloneUnhovered() {

            assertThat(buildPlacement(ADDED_GLOW_PALETTE, UNTINTED_ICON).resolveIconTint(UNHOVERED))
                .isEqualTo(new Color(0, 0, 100));
        }

        @Test
        void resolveIconTintWashesAnImageStatingItsOwnColourRatherThanRepaintingIt() {
            // An asset authored in its own colours is dimmed and lit through them rather than replaced by
            // the shade, so a crest under this stays a crest. 100 = 200 * 128/255.
            var tintedIcon = new ImageSpan("graphics/icons/x.png", new Color(128, 128, 128));

            assertThat(buildPlacement(MEETING_SHADE_PALETTE, tintedIcon).resolveIconTint(FULLY_HOVERED))
                .isEqualTo(new Color(100, 0, 0));
        }
    }

    // A band button standing in the given palette and carrying the given mark. Its box never reaches an
    // assertion here - the tint is resolved from the look and the image alone - so one is stated once.
    private static BandButtonPlacement buildPlacement(TabPalette palette, ImageSpan icon) {
        return new BandButtonPlacement(
            new Control(
                new ControlSpec.Tabs(
                    List.of(""),
                    List.of(),
                    ControlSpec.NO_SELECTION,
                    ControlAction.NONE),
                BUTTON_BOX,
                List.of()),
            TabStyles.buildAtBandHeightInPalette(BAND_HEIGHT, palette),
            icon);
    }
}
