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
     * The light to lay over the finished tab at that same point of the fade, for a rule that brightens by
     * adding rather than by settling on another shade.
     *
     * <p>Two answers rather than one because the two rules reach the screen differently, not because they
     * differ in amount: a shade is the tab's own surface and has to be resolved before it is painted, where
     * light lands on whatever the surface turned out to be - the fill, the text, and anything showing
     * through an unpainted interior. A rule that names a shade adds no light, and one that adds light
     * leaves the settled look alone, so a tab is never brightened twice over.
     *
     * @param hoverFraction how far the fade has run, 0 fully off and 1 fully on
     * @return the light to add, or {@link TabLight#NONE} where the rule brightens by shade
     */
    TabLight computeAddedLight(float hoverFraction);

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

        @Override
        public TabLight computeAddedLight(float hoverFraction) {
            return TabLight.NONE;
        }
    }

    /**
     * The rule vanilla's raised buttons answer by: the pointer adds light to whatever the button already
     * wears, so the shown button and an unshown one light by the same amount from different places and
     * never meet. Added rather than travelled toward, because that is what the engine's own buttons do -
     * solving a sampled pointed-at button against the shade it was resting at gives one consistent weight
     * of added accent on every channel, and no consistent weight at all as a blend.
     *
     * <p>The light is laid over the finished button rather than mixed into its surface, which is what makes
     * an unshown button answer the pointer at all: its interior is unpainted, so a shade mixed in arrives
     * diluted by however little of it is painted - a fraction of the step the shown button takes, where the
     * engine's own two move by the same one. Added over the top, both do, and the label the light crosses
     * brightens with the surface under it because one pass covers the whole button.
     *
     * @param glowColour the light added, whichever colour a chrome's own counterpart is lit by
     * @param glowAmount how much of it a fully hovered button takes
     */
    record AddedGlow(
        Color glowColour,
        float glowAmount) implements TabHover {

        @Override
        public TabLook computeHoveredLook(TabLook settledLook, float hoverFraction) {
            return settledLook;
        }

        @Override
        public TabLight computeAddedLight(float hoverFraction) {
            return new TabLight(glowColour, glowAmount * hoverFraction);
        }
    }
}
