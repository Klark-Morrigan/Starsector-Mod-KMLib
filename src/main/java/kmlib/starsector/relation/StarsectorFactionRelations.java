package kmlib.starsector.relation;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.SectorAPI;

import kmlib.text.KmlibStrings;

import java.util.function.BiPredicate;

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
 * <p>Answered of a faction the caller already holds, or over a pair of ids bound to one sector -
 * the same rule at the two shapes callers ask it in, so a surface composing dispositions takes the
 * pair form rather than writing the lookup itself.
 *
 * <p>Reads are bare: any {@link RuntimeException} from a modded {@link FactionAPI} propagates to
 * the caller rather than degrading silently. Stateless - every entry point is a static method, no
 * instance needed.
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

    /**
     * The same answer over a pair of faction ids, bound to one sector: whether the first is disposed
     * above neutral toward the second.
     *
     * <p>Offered because a rule composing dispositions takes the faction-level answer as a plain
     * predicate over ids, and every such caller would otherwise write the same lookup-and-ask by
     * hand. Two copies of it are two bindings of one relation, free to drift in which side of the
     * pair is looked up - and a surface reporting one relation two ways is the fault composing them
     * centrally exists to prevent.
     *
     * <p>Bound to the sector handed in rather than to the game's current one, so a caller drawing
     * over a second sector reports that sector's relations. The sector is read per call, not
     * captured as a faction, since a reader outlives the standings it is asked about.
     *
     * @param sector the sector the factions are looked up in; nothing is read of no sector, so no
     *               pair is above neutral
     * @return the pair test, answering false wherever either side names nobody the sector knows
     */
    public static BiPredicate<String, String> createDispositionReader(SectorAPI sector) {

        if (sector == null) {
            return (factionId, otherFactionId) -> false;
        }
        return (factionId, otherFactionId) -> isDispositionAboveNeutral(
            sector.getFaction(factionId),
            otherFactionId);
    }
}
