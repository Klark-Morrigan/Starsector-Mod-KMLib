package kmlib.starsector.relation;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.RepLevel;

import kmlib.text.KmlibStrings;

/**
 * Reads whether one faction's standing with another is above the base game's own neutral:
 * {@link RepLevel#FAVORABLE} or better, which is every disposition warmer than indifference and no
 * others.
 *
 * <p>A landmark the game itself names, shows the player on every faction screen, and stops
 * describing goodwill below - which is what makes a surface sorting factions by disposition
 * explicable. A cut taken anywhere else in the scale would be one the player is never shown.
 *
 * <p>Named for the test rather than for whatever word a heading above it uses, so nothing reading
 * this concludes the answer means the single {@link RepLevel#FRIENDLY} level.
 *
 * <p>Reads are bare: any {@link RuntimeException} from a modded {@link FactionAPI} propagates to
 * the caller rather than degrading silently. Stateless - the single entry point is a static method,
 * no instance needed.
 */
public final class StarsectorFactionRelations {

    private StarsectorFactionRelations() {
    }

    /**
     * Whether one faction is disposed above neutral toward another.
     *
     * <p>Asked of the standing itself rather than of the raw reputation float, so the threshold is
     * the scale's own step from indifference to goodwill rather than a number this class picks.
     *
     * @param faction        the faction whose disposition is read; nothing is read of no faction,
     *                       so it is not above neutral with anyone
     * @param otherFactionId the faction it is disposed toward; an id with no text names nobody to
     *                       be disposed toward
     * @return true where the standing is {@link RepLevel#FAVORABLE} or better
     */
    public static boolean isDispositionAboveNeutral(FactionAPI faction, String otherFactionId) {

        if (faction == null || !KmlibStrings.hasText(otherFactionId)) {
            return false;
        }
        var level = faction.getRelationshipLevel(otherFactionId);

        // A faction that answers no level at all has no standing to be above anything, and goodwill
        // is a positive claim: the absent answer is "not above neutral" rather than the benefit of
        // the doubt, so nothing reports warmth it never read.
        return level != null
            && level.isAtWorst(RepLevel.FAVORABLE);
    }
}
