package kmlib.starsector.ui.widgets.tabs.style;

import java.awt.Color;

/**
 * What the pointer does to a tab, in the one form the two vanilla chromes disagree about. The engine's
 * tabs and its buttons answer a pointer by different rules, and the difference is not a number: a tab
 * lands on one shade whatever it was showing before, where a button brightens from wherever it already
 * stands and so keeps the shown one apart from the rest under the pointer as well as away from it.
 *
 * <p>Sealed over the two, so a palette states which rule it answers by and a caller cannot compose a
 * third by handing over the wrong kind of value. Both take a settled look and how far the fade has run
 * and give back the look to paint, so everything upstream - the fades, the bound key's blink, the
 * compositing order - is written once against the pair rather than per chrome.
 *
 * <p>Whichever rule applies, fill and label move together: a pointer answering the surface but not the
 * text on it would read as a tab sliding out from under its own label.
 */
public sealed interface TabHover {

    /**
     * The look a tab wears part-way onto its hovered state.
     *
     * @param settledLook   the look the tab rests at with no pointer on it
     * @param hoverFraction how far the fade has run, 0 fully off and 1 fully on
     * @return the look to paint at that point
     */
    TabLook computeHoveredLook(TabLook settledLook, float hoverFraction);

    /**
     * The rule vanilla's tab strips answer by: every tab under the pointer travels to one named shade,
     * whatever it was showing before. It is what lets the resting and the shown tab meet, which no lift
     * applied to each tab's own fill could produce - two starting colours moved by one fraction stay two
     * colours. The shown tab is told apart while hovered by the underline that marks it, not by its fill.
     *
     * @param shade the one look both the resting and the shown tab arrive at under the pointer
     */
    record MeetingShade(
        TabLook shade) implements TabHover {

        @Override
        public TabLook computeHoveredLook(TabLook settledLook, float hoverFraction) {
            return settledLook.computeBlendedLook(shade, hoverFraction);
        }
    }

    /**
     * The rule vanilla's raised buttons answer by: the pointer adds light to whatever the button already
     * wears, so the shown button and an unshown one light by the same amount from different places and
     * never meet. Added rather than travelled toward, because that is what the engine's own buttons do -
     * solving a sampled pointed-at button against the shade it was resting at gives one consistent weight
     * of added accent on every channel, and no consistent weight at all as a blend.
     *
     * <p>The light paints as well as brightens: an unshown button rests on an unpainted interior, and what
     * the pointer puts there is that light itself, as solid as the light is deep. Brightening the channels
     * of a surface still drawn at nothing would leave the button answering with its label alone.
     *
     * @param glowColour the light added, whichever colour a chrome's own counterpart is lit by
     * @param glowAmount how much of it a fully hovered button takes
     */
    record AddedGlow(
        Color glowColour,
        float glowAmount) implements TabHover {

        @Override
        public TabLook computeHoveredLook(TabLook settledLook, float hoverFraction) {
            return settledLook.computeGlowingLook(glowColour, glowAmount * hoverFraction);
        }
    }
}
