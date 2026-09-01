package kmlib.starsector.relation;

import com.fs.starfarer.api.campaign.RepLevel;

import java.awt.Color;

/**
 * Where a faction stands with the player, as the three facets the game itself keeps of that one
 * relation: the level it names the standing, the reputation it counts it at, and the colour it
 * paints both in.
 *
 * <p>Carried as one value because all three come off a single lookup. Read separately, a surface
 * wanting the number and a surface wanting the colour each walk the relationship again, and the two
 * walks are free to disagree - which is the drift a shared value exists to close.
 *
 * <p>Holds no absent case of its own: a faction that answers no standing is reported by handing
 * back no standing, which is what keeps "nothing was read" off the scale this value sits on.
 */
public record PlayerStanding(
    RepLevel level,
    int reputation,
    Color colour) {
}
