package kmlib.starsector.ui.widgets.tabs.style;

import kmlib.starsector.ui.font.StarsectorFont;
import kmlib.starsector.ui.font.TextFace;

import java.awt.Color;

/**
 * Test-only builders for a {@link TabStyle} at a chosen band height. A style describes a strip end to
 * end, so even a test exercising nothing but the layout has to name a palette, a hotkey look, and a face;
 * these fill them all with fixed stand-ins, since no layout or geometry assertion reads a colour. They
 * are literal rather than {@link TabPalette#createMapTabPalette} /
 * {@link HotkeyStyle#createUnderlined()} because those resolve through the live engine palette, which a
 * unit test has no sector to supply.
 */
public final class TabStyles {
    // One flat shade in every role: the tests here assert dimensions and geometry, so which colour sits
    // in which role never reaches an assertion, and a single value keeps the fixture from reading as
    // though the roles mattered.
    private static final Color STAND_IN_SHADE = Color.GRAY;

    // No pulse lift in the momentary role: these tests draw nothing, so a lift would only suggest the
    // layout reserves something for one (it does not - a wash costs no room). A zero strength blends
    // nowhere, so which colour the lift names never shows.
    private static final TabWash STAND_IN_NO_LIFT = new TabWash(STAND_IN_SHADE, 0f);

    private static final TabPalette STAND_IN_PALETTE = new TabPalette(
        STAND_IN_SHADE,
        STAND_IN_SHADE,
        new TabLook(STAND_IN_SHADE, STAND_IN_SHADE),
        new TabLook(STAND_IN_SHADE, STAND_IN_SHADE),
        new TabHover.MeetingShade(new TabLook(STAND_IN_SHADE, STAND_IN_SHADE)),
        STAND_IN_NO_LIFT);

    // A plain key in the same stand-in shade: the tests here draw nothing, so how a bound key is
    // presented never reaches an assertion, and the un-emphasised look keeps the fixture from implying
    // the layout reserves room for a rule (it does not - the emphasis costs no width).
    private static final HotkeyStyle STAND_IN_HOTKEY = new HotkeyStyle(
        STAND_IN_SHADE,
        false,
        0f,
        0f);

    // The face the vanilla map tabs read in, at its own atlas size - a real face rather than an invented
    // one, so a test that does measure text measures against a size a host actually asks for.
    private static final TextFace STAND_IN_FACE = new TextFace(
        StarsectorFont.VANILLA_ORBITRON_20AA,
        StarsectorFont.VANILLA_ORBITRON_20AA.getNativeSize());

    private TabStyles() {
    }

    /**
     * Builds a tab style standing its band at the given height, with the stand-in palette, hotkey look,
     * and face.
     *
     * @param headerBandHeight how tall the tab band stands, passed through to the style unchanged (so a
     *                         negative value still exercises the style's own clamp)
     * @return the tab style at that band height
     */
    public static TabStyle buildAtBandHeight(float headerBandHeight) {
        return buildAtBandHeightInFace(headerBandHeight, STAND_IN_FACE);
    }

    /**
     * Builds a tab style standing its band at the given height and lettered in the given face - for a
     * test whose subject is the face itself, the size a row is measured at being the one thing a style's
     * face decides for the layout.
     *
     * @param headerBandHeight how tall the tab band stands, passed through as above
     * @param face             the face the tabs are measured and drawn in
     * @return the tab style at that band height and face
     */
    public static TabStyle buildAtBandHeightInFace(float headerBandHeight, TextFace face) {
        return buildAtBandHeightInFaceAndBox(headerBandHeight, face, TabBox.SNAPPED);
    }

    /**
     * Builds a tab style standing its band at the given height with the given tab box - for a test whose
     * subject is the box, a fixed one sizing the row without measuring a label at all.
     *
     * @param headerBandHeight how tall the tab band stands, passed through as above
     * @param tabBox           the box each tab stands in within that band
     * @return the tab style at that band height and box
     */
    public static TabStyle buildAtBandHeightInBox(float headerBandHeight, TabBox tabBox) {
        return buildAtBandHeightInFaceAndBox(headerBandHeight, STAND_IN_FACE, tabBox);
    }

    /**
     * Builds a tab style over the given palette - for a test whose subject is a shade, the flat stand-in
     * set above being deliberately unable to tell one role from another.
     *
     * @param headerBandHeight how tall the tab band stands, passed through as above
     * @param palette          the paint the row's looks are resolved from
     * @return the tab style at that band height over that palette
     */
    public static TabStyle buildAtBandHeightInPalette(float headerBandHeight, TabPalette palette) {
        return composeStyle(headerBandHeight, STAND_IN_FACE, TabBox.SNAPPED, palette);
    }

    /**
     * Builds a tab style standing its band at the given height, lettered in the given face and standing
     * its tabs in the given box - the one composition the narrower builders above both route through.
     *
     * @param headerBandHeight how tall the tab band stands, passed through as above
     * @param face             the face the tabs are measured and drawn in
     * @param tabBox           the box each tab stands in within that band
     * @return the tab style at that band height, face and box
     */
    public static TabStyle buildAtBandHeightInFaceAndBox(
            float headerBandHeight,
            TextFace face,
            TabBox tabBox) {

        return composeStyle(headerBandHeight, face, tabBox, STAND_IN_PALETTE);
    }

    // Every builder above lands here, so the roles no test reads - the chrome, the hotkey look, the ring,
    // the sharpness - are filled in one place rather than once per entry point.
    private static TabStyle composeStyle(
            float headerBandHeight,
            TextFace face,
            TabBox tabBox,
            TabPalette palette) {

        return new TabStyle(
            // The map's own chrome, which no test here draws: these assert dimensions and geometry, and a
            // chrome is read only at paint time.
            TabChrome.STRIP,
            headerBandHeight,
            tabBox,
            palette,
            STAND_IN_HOTKEY,
            face,
            // No ring, for the same reason the key is left plain: a halo costs no width, so a measured row
            // is the same either way, and the un-haloed look keeps the fixture from implying otherwise.
            TextHalo.NONE,
            // Fully hard, which no test here reads: sharpness reaches the paint pass alone and moves
            // nothing these fixtures measure.
            1f);
    }
}
