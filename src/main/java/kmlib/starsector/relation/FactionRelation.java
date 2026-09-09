package kmlib.starsector.relation;

import com.fs.starfarer.api.campaign.RepLevel;

import java.awt.Color;

/**
 * Where one faction stands with another, as the three facets the game itself keeps of that one
 * relation: the level it names the standing, the reputation it counts it at, and the colour it
 * paints both in.
 *
 * <p>Carried as one value because all three come off a single lookup. Read separately, a surface
 * wanting the number and a surface wanting the colour each walk the relation again, and the two
 * walks are free to disagree - which is the drift a shared value exists to close.
 *
 * <p>Names neither side of the pair. Which two factions were measured is fixed by whoever read the
 * relation, so a surface asking where a faction stands with the player and one asking where it
 * stands with anybody else hold the same value and neither has to convert.
 *
 * <p>Holds no absent case of its own: a faction that answers no relation is reported by handing
 * back none, which is what keeps "nothing was read" off the scale this value sits on.
 */
public record FactionRelation(
    RepLevel level,
    int reputation,
    Color colour) {

    /**
     * Whether this relation clears the scale's own step from indifference to goodwill -
     * {@link RepLevel#FAVORABLE} or better, which is a reputation past +9.
     *
     * <p>Asked of the level rather than of the number, so the cut is the one the game shows the
     * player on every faction screen rather than a threshold this code picks. The distinction
     * matters at the bottom of the band: a reputation of +3 reads as "Neutral" everywhere else in
     * the interface, and a surface calling that goodwill would contradict the rest of the game.
     *
     * @return true where the relation is {@link RepLevel#FAVORABLE} or better
     */
    public boolean isAboveNeutral() {
        return level.isPositive();
    }
}
