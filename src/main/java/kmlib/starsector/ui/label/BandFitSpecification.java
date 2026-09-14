package kmlib.starsector.ui.label;

/**
 * The room {@link LabelBoxFitter} sizes a label within, beside the font clamp itself: how
 * far every band stays clear of a keep-out point, how far short of its clear span's ends
 * the text stops, and how closely the font search has to land on the tallest font that
 * band holds.
 *
 * <p>Gathered into one value because all three are properties of the space a label is
 * measured against rather than of the text measured into it - which is what separates them
 * from {@link NameFitSpecification}, where the per-line height and the line budget describe
 * the text. The search precision belongs on this side because what a halving costs is a
 * band measured against that same space, so it is priced in the same currency the two
 * distances are.
 *
 * @param keepOutClearance    how far a band stays clear of each keep-out point, world units
 * @param endInsetDistance    how far each end of the clear span pulls inward before text
 *                            may occupy it, world units
 * @param fontHeightTolerance how close the font search lands to the tallest font the band
 *                            holds, world units; must be positive, since halving closes an
 *                            interval towards zero without reaching it
 */
public record BandFitSpecification(
    double keepOutClearance,
    double endInsetDistance,
    double fontHeightTolerance) {
}
