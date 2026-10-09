package kmlib.starsector.factions.relation;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.SectorAPI;

import kmlib.starsector.factions.SectorFactions;
import kmlib.text.KmlibStrings;

import java.util.Optional;
import java.util.function.BiPredicate;

/**
 * Reads how one faction stands with another: the whole relation as a {@link FactionRelation}, and
 * the one question a surface asks of it often enough to be worth a predicate of its own - whether
 * that relation is above the base game's own neutral, which is
 * {@link FactionRelation#isAboveNeutral()} answered straight off a pair. Where the cut falls and why
 * it is the game's rather than ours is stated there, on the value that holds it.
 *
 * <p>A pair nobody can look up is handed back as no relation rather than as a reputation of nought,
 * for the reason {@link FactionRelation} gives.
 *
 * <p>A pair the observer answers for is never absent, however little it answers. A faction naming no
 * level still reports the raw relationship, and the scale covers the whole float range - so the level
 * is reconstructed from the number rather than read as no relation at all.
 *
 * <p>Reads are bare: any {@link RuntimeException} from a modded {@link FactionAPI} propagates to
 * the caller rather than degrading silently.
 */
public final class StarsectorFactionRelations {

    private StarsectorFactionRelations() {
    }

    /**
     * {@link #isDispositionAboveNeutral} over a pair of faction IDs, bound to one sector: whether the
     * first is disposed above neutral toward the second.
     *
     * <p>Offered because a rule composing dispositions takes the faction-level answer as a plain
     * predicate over IDs, and every such caller would otherwise write the same lookup-and-ask by
     * hand. Two copies of it are two bindings of one relation, free to drift in which side of the
     * pair is looked up - and a surface reporting one relation two ways is the fault composing them
     * centrally exists to prevent.
     *
     * <p>Bound to the sector handed in rather than to the game's current one, so a caller drawing
     * over a second sector reports that sector's relations. The sector is read per call, not
     * captured as a faction, since a reader outlives the relations it is asked about.
     *
     * @param sector the sector the factions are looked up in; nothing is read of no sector, so no
     *               pair is above neutral
     * @return the pair test, answering false wherever either side names nobody the sector knows
     */
    public static BiPredicate<String, String> createDispositionReader(SectorAPI sector) {

        // No sector, or a first ID naming nobody, finds no faction - and no faction is above
        // neutral with anyone, so the lookup's null needs no branch of its own here.
        return (factionId, otherFactionId) -> isDispositionAboveNeutral(
            SectorFactions.findFaction(sector, factionId),
            otherFactionId);
    }

    /**
     * Whether one faction is disposed above neutral toward another.
     *
     * <p>Asked of the level alone rather than through {@link #readRelation}, because that read also
     * resolves a colour, and resolving one reaches the game's settings - which a predicate that
     * paints nothing must not depend on. Both spellings rest on {@link RepLevel#isPositive()}, so
     * the cut cannot drift from {@link FactionRelation#isAboveNeutral()}.
     *
     * <p>Goodwill is a positive claim: a pair that names nobody answers false rather than taking the
     * benefit of the doubt, so nothing reports warmth it never read.
     *
     * @param faction        the faction whose disposition is read; nothing is read of no faction,
     *                       so it is not above neutral with anyone
     * @param otherFactionId the faction it is disposed toward; an ID with no text names nobody to
     *                       be disposed toward
     * @return true where the relation is {@link RepLevel#FAVORABLE} or better
     */
    public static boolean isDispositionAboveNeutral(FactionAPI faction, String otherFactionId) {

        if (!hasRelationPair(faction, otherFactionId)) {
            return false;
        }

        return resolveRelationLevel(faction, otherFactionId, faction.getRelationship(otherFactionId))
            .isPositive();
    }

    /**
     * Resolves how one faction stands with another.
     *
     * <p>The general form of the read: a surface measuring a whole sector against one chosen
     * faction asks the same question of every pair, and the number, the level and the colour all
     * come off the one lookup here rather than being walked separately by whoever wants each.
     *
     * @param observer  the faction whose relation is read; nothing is read of no faction, so it
     *                  holds no relation
     * @param subjectId the faction it is measured against; an ID with no text names nobody to hold
     *                  a relation with
     * @return the relation, or none where there is no pair to read one from
     */
    public static Optional<FactionRelation> readRelation(FactionAPI observer, String subjectId) {

        if (!hasRelationPair(observer, subjectId)) {
            return Optional.empty();
        }

        var relationship = observer.getRelationship(subjectId);

        return Optional.of(new FactionRelation(
            resolveRelationLevel(observer, subjectId, relationship),
            RepLevel.getRepInt(relationship),
            StarsectorRelationColours.resolveRelationColour(observer, subjectId, relationship)));
    }

    // Whether there is a pair to read at all. Stated once, so the whole-relation read and the
    // disposition predicate cannot disagree on which pairs name nobody.
    private static boolean hasRelationPair(FactionAPI observer, String subjectId) {

        return observer != null
            && KmlibStrings.hasText(subjectId);
    }

    // The level the observer holds the pair at, taken from the faction where it names one and
    // reconstructed from the raw relationship where it does not. The one statement of how a level is
    // read, so the whole-relation read and the disposition predicate cannot answer a pair
    // differently - held apart, one would report goodwill the other declined to see.
    private static RepLevel resolveRelationLevel(
            FactionAPI observer,
            String subjectId,
            float relationship) {

        var level = observer.getRelationshipLevel(subjectId);

        return level == null
            ? RepLevel.getLevelFor(relationship)
            : level;
    }
}
