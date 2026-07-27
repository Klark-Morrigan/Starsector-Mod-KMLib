package kmlib.starsector.ui.widgets.tabs;

import kmlib.starsector.ui.font.StarsectorFont;
import kmlib.starsector.ui.font.TextFace;

import java.awt.Color;

/**
 * Test-only builders for a {@link TabStyle} at a chosen band height. A style describes a strip end to
 * end, so even a test exercising nothing but the layout has to name a palette and a face; these fill both
 * with fixed stand-ins, since no layout or geometry assertion reads a colour. The palette is literal
 * rather than {@link VanillaTabColors#mapTabs()} because that resolves through the live engine palette,
 * which a unit test has no sector to supply.
 */
public final class TabStyles {
    // One flat shade in every role: the tests here assert dimensions and geometry, so which colour sits
    // in which role never reaches an assertion, and a single value keeps the fixture from reading as
    // though the roles mattered.
    private static final Color STAND_IN_SHADE = Color.GRAY;

    private static final VanillaTabColors STAND_IN_COLORS = new VanillaTabColors(
            STAND_IN_SHADE,
            STAND_IN_SHADE,
            STAND_IN_SHADE,
            STAND_IN_SHADE,
            STAND_IN_SHADE,
            STAND_IN_SHADE,
            STAND_IN_SHADE);

    // The face the vanilla map tabs read in, at its own atlas size - a real face rather than an invented
    // one, so a test that does measure text measures against a size a host actually asks for.
    private static final TextFace STAND_IN_FACE = new TextFace(
            StarsectorFont.VANILLA_ORBITRON_20AA,
            StarsectorFont.VANILLA_ORBITRON_20AA.getNativeSize());

    private TabStyles() {
    }

    /**
     * Builds a tab style standing its band at the given height, with the stand-in palette and face.
     *
     * @param headerBandHeight how tall the tab band stands, passed through to the style unchanged (so a
     *                         negative value still exercises the style's own clamp)
     * @return the tab style at that band height
     */
    public static TabStyle buildAtBandHeight(float headerBandHeight) {
        return new TabStyle(headerBandHeight, STAND_IN_COLORS, STAND_IN_FACE);
    }
}
