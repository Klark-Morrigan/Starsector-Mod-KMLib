package kmlib.starsector.ui.widgets.tabs.style;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.colour.StarsectorUiColour;

import java.awt.Color;
import java.util.List;

/**
 * Whether a row's text stands inside a ring of itself, and how wide and how strong that ring is. A small
 * bitmap face is hard-edged pixels with no anti-aliasing of its own, so over live content it reads thin
 * where a larger smooth face reads solid; a dark ring around every glyph gives the strokes an edge to sit
 * against whatever is behind them. A face with weight of its own wants none of it and reads muddier for
 * one, so the ring travels with a tab's look the way its face does.
 *
 * <p>A ring rather than an offset copy: a drop shadow falls to one side, which announces a light source
 * the flat chrome around a panel has none of, and it thins the very edge it leaves bare. The ring is the
 * same on all four sides, so no direction is implied and no side is left unbacked.
 *
 * <p>{@link #NONE} is a ring that draws nothing, which is a different thing from one of zero radius: a
 * zero radius still lays every run down four more times, exactly under the text they would be hidden by.
 * The flag is what a renderer reads to skip those passes entirely, and the radius and strength go unread
 * when it is false.
 *
 * <p>Substrate-independent like the rest of this package - an AWT colour, a dimension, and a unit
 * fraction - so the value travels inside the {@link TabStyle} a renderer paints from.
 *
 * @param colour      the shade the ring draws in, unread when no ring is drawn
 * @param isHaloDrawn whether the ring is drawn at all; false leaves the fields below unread
 * @param radius      how far each copy sits from the text it backs, in UI pixels
 * @param strength    how solid the ring is, 0..1, applied on top of whatever opacity the row is drawn at.
 *                    It is a knob rather than a constant because it answers to the face: a stroke a pixel
 *                    wide wants the full shade behind it, where a heavier face would read as blurred for
 *                    the same ring
 */
public record TextHalo(
    Color colour,
    boolean isHaloDrawn,
    float radius,
    float strength) {

    // A single pixel out, solid. Dialled against the real intel chrome rather than derived: the
    // engine's own text pass is behind obfuscated classes, and the face itself carries no edge to read
    // one off - every pixel in its atlas is either fully on or fully off. Solid for that same reason:
    // a face of hard-edged pixels backed by a partial shade reads as a grey smudge around the strokes
    // where the vanilla text it stands beside reads as strokes on black.
    private static final float HAIRLINE_HALO_RADIUS = 1f;
    private static final float HAIRLINE_HALO_STRENGTH = 1f;

    /**
     * No ring: the text stands on its own, which is what a smooth face drawn at size wants. A frozen
     * constant rather than a factory, since nothing about drawing nothing answers to the live palette.
     */
    public static final TextHalo NONE = new TextHalo(
        StarsectorUiColour.BLACK.resolve(),
        false,
        0f,
        0f);

    /**
     * A black hairline ring a pixel out from the glyphs: what a small pixel face wants when it is drawn
     * over live content, its strokes otherwise having nothing but that content to read against.
     *
     * @return a halo drawing the dark ring
     */
    public static TextHalo createBlackHairline() {
        return new TextHalo(
            StarsectorUiColour.BLACK.resolve(),
            true,
            HAIRLINE_HALO_RADIUS,
            HAIRLINE_HALO_STRENGTH);
    }

    /**
     * The boxes the ring's copies are laid out in: the text's own box shifted one radius to each side, so
     * whatever a renderer places inside that box - the runs, and any emphasis drawn under one of them -
     * moves as one piece rather than each piece being shifted at its own draw site. Empty for
     * {@link #NONE}, so a renderer walking this list draws nothing without testing the flag itself.
     *
     * <p>Four copies rather than eight: at a radius this small a diagonal copy lands within the pixels its
     * two neighbouring copies already cover, so the second four cost a pass over every run each and darken
     * nothing the first four left bare.
     *
     * @param textBox the box the text itself is centred in, in UI coordinates (origin bottom-left)
     * @return the boxes to centre the ring's copies in, in the same coordinates
     */
    public List<Rectangle> computeHaloBoxes(Rectangle textBox) {

        if (!isHaloDrawn) {
            return List.of();
        }
        return List.of(
            computeShiftedBox(textBox, -radius, 0f),
            computeShiftedBox(textBox, radius, 0f),
            computeShiftedBox(textBox, 0f, -radius),
            computeShiftedBox(textBox, 0f, radius));
    }

    // One copy's box: the text's own, moved bodily. Size is untouched - a box that grew or shrank would
    // slide the copy centred in it by half the difference on top of the shift asked for.
    private static Rectangle computeShiftedBox(Rectangle textBox, float shiftX, float shiftY) {
        return new Rectangle(
            textBox.x() + shiftX,
            textBox.y() + shiftY,
            textBox.width(),
            textBox.height());
    }
}
