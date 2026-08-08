package kmlib.starsector.ui.widgets.tabs.style;

import java.awt.Color;

/**
 * The three colours a vanilla raised button is painted from: the dark step its interior is filled with,
 * the accent the pointer brightens it by, and the backing it all stands over. Bundled for the reason
 * {@link VanillaTabPaint} is - the three only ever travel together, and a button's states then differ in
 * one amount rather than in three colours re-threaded per state.
 *
 * <p>Apart from {@link VanillaTabPaint} rather than shared with it, because a button and a tab brighten
 * from different sources: a tab's light is a whitened form of its own label colour, a button's is the
 * accent it was built with, added undiluted. One record with a field meaning the label on one chrome and
 * the accent on the other would read as the same value on both and be neither.
 *
 * <p>Substrate-independent, like the rest of this package: colour values with nothing GL about them.
 *
 * @param fill        the dark accent step a lit button's interior is filled with, alpha and all - its
 *                    translucency is what the backing shows through, and so part of the shade
 * @param glowColour  the accent the pointer adds over that fill - the engine hands a button its base
 *                    colour for exactly this, so it is added as it comes rather than whitened first
 * @param backdrop    the backing the button is drawn over, taken as opaque
 */
public record VanillaButtonPaint(
    Color fill,
    Color glowColour,
    Color backdrop) {
}
