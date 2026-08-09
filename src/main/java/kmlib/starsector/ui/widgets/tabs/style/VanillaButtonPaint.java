package kmlib.starsector.ui.widgets.tabs.style;

import java.awt.Color;

/**
 * The two colours a vanilla raised button's interior is worked out from: the dark step it is filled with,
 * and the backing it stands over. Bundled for the reason {@link VanillaTabPaint} is - the pair only ever
 * travels together, so a button's states differ in what is done to one shade rather than in colours
 * re-threaded per state.
 *
 * <p>Apart from {@link VanillaTabPaint} rather than shared with it, because a button and a tab are lit
 * from different sources: a tab's light is a whitened form of its own label colour and so travels with
 * its paint, where a button's is plain light and belongs to the pointer rather than to the button - see
 * {@link VanillaButtonFills#POINTED_LIGHT}. That is why nothing here names a glow: a button's settled
 * shades take none.
 *
 * <p>Substrate-independent, like the rest of this package: colour values with nothing GL about them.
 *
 * @param fill     the dark accent step a lit button's interior is filled with, alpha and all - its
 *                 translucency is what the backing shows through, and so part of the shade
 * @param backdrop the surface the engine's own translucent fill composites against, taken as opaque -
 *                 what a sampled vanilla button is a measurement of, and so what reproducing its shade
 *                 is measured from. Not a claim about what a chrome lays under its own buttons: the
 *                 shades this yields are opaque, so whatever backs them is covered wherever one is
 *                 painted and shows through only where none is
 */
public record VanillaButtonPaint(
    Color fill,
    Color backdrop) {
}
