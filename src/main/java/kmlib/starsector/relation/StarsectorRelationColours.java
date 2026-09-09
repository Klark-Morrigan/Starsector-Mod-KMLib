package kmlib.starsector.relation;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.util.Misc;

import kmlib.colour.Colours;
import kmlib.starsector.factions.FactionPalette;

import java.awt.Color;

/**
 * What colour a relation is painted in: the continuous ramp from the negative highlight at -1,
 * through a neutral grey, to the positive highlight at +1.
 *
 * <p>Delegates to {@link Misc#getRelColor(float)} rather than interpolating between the two
 * highlights here, so three things stay the engine's rather than becoming ours - the floor that
 * keeps a barely-signed relationship from reading as plain grey, the clamp past the ends of the
 * scale, and the swap of the negative highlight to blue under colourblind mode. Reimplementing the
 * lerp is a handful of lines and silently drops all three.
 *
 * <p>Answers about a number rather than about a faction pair, which is what a surface wants once it
 * has decided which relation a thing is painted at - an aggregate over several factions, say, which
 * has no pair left to ask about. Where a pair is in hand, {@link StarsectorFactionRelations} is the
 * read: a faction may paint a particular pair in a shade of its own that the number alone cannot
 * report, and the relation it hands back carries that shade already resolved.
 *
 * <p>Stateless - every entry point is a static method, no instance needed.
 */
public final class StarsectorRelationColours {

    // The share of each colour channel a derived dark shade keeps. Vanilla dims an authored
    // interface colour into its darker companion at this factor (Misc.getDesignTypeColorDim), so a
    // pair derived here sits apart by about as much as a faction's own authored bright and dark
    // shades do - which is what lets relation shades drop into the slots faction shades came out of
    // without borders and partings reading flatter than they did.
    private static final float DARK_SHADE_FACTOR = 0.53f;

    private StarsectorRelationColours() {
    }

    /**
     * The ramp shade for one relationship value.
     *
     * @param relationship the raw relationship the engine keeps, -1 through 0 to +1 - what
     *                     {@link FactionAPI#getRelationship(String)} answers, not the signed hundred
     *                     {@link FactionRelation#reputation} counts
     * @return the shade, greyest at nought and strongest at either end
     */
    public static Color resolveRelationColour(float relationship) {
        return Misc.getRelColor(relationship);
    }

    /**
     * The bright and dark pair a surface paints a relationship in: the ramp shade as primary and a
     * darkened form of it as secondary.
     *
     * <p>Answered as the same pair a faction's own authored shades arrive in, so anything that
     * paints an owner in two shades - fills against their borders, cells against their partings -
     * takes relation shades without knowing they were derived rather than authored.
     *
     * @param relationship the raw relationship the engine keeps, -1 through 0 to +1
     * @return the two shades, the darker one scaled off the brighter
     */
    public static FactionPalette resolveRelationPalette(float relationship) {

        var relationColour = resolveRelationColour(relationship);

        return new FactionPalette(
            relationColour,
            Colours.darken(relationColour, DARK_SHADE_FACTOR));
    }

    /**
     * The shade the game would paint one faction pair in: the observer's own answer for that pair,
     * or the ramp where it has none.
     *
     * <p>Package-private, and takes its observer as read. The callers are the package's own reads,
     * which have already turned an absent faction into an absent relation before they reach here -
     * published, this would be the one entry point that could be handed nobody. It also has no
     * consumer outside: a caller holding a pair wants the relation, which carries this shade
     * already, and a caller holding a number wants the ramp above. It opens up when something needs
     * a pair's own shade and nothing else.
     *
     * @param observer     the faction whose palette is asked first
     * @param subjectId    the faction the colour is asked about
     * @param relationship the raw relationship the ramp falls back to
     * @return the shade the game would paint this pair in
     */
    static Color resolveRelationColour(FactionAPI observer, String subjectId, float relationship) {

        var factionColour = observer.getRelColor(subjectId);

        return factionColour == null
            ? resolveRelationColour(relationship)
            : factionColour;
    }
}
