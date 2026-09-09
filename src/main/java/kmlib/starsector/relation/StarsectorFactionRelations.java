package kmlib.starsector.relation;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.SectorAPI;

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
 * <p>The predicate is named for the test rather than for whatever word a heading above it uses, so
 * nothing reading this concludes the answer means the single {@link RepLevel#FRIENDLY} level. It is
 * answered of a faction the caller already holds, or over a pair of ids bound to one sector - the
 * same rule at the two shapes callers ask it in, so a surface composing dispositions takes the pair
 * form rather than writing the lookup itself.
 *
 * <p>Absence is carried by handing back no relation rather than by a reputation of nought. Nought is
 * not a spare value to signal it with: it sits in the middle of the band the scale calls
 * indifference, which is where every faction with no history to speak of reads - so a pair nobody
 * can look up and a pair that does not care would arrive as the same fact. A caller folding
 * relations across several factions has to be able to skip the first without dragging the scale's
 * centre into its answer.
 *
 * <p>A pair the observer answers for is never absent, however little it answers. A faction naming no
 * level still reports the raw relationship, and the scale covers the whole float range - so the level
 * is reconstructed from the number rather than read as no relation at all.
 *
 * <p>Reads are bare: any {@link RuntimeException} from a modded {@link FactionAPI} propagates to
 * the caller rather than degrading silently. Stateless - every entry point is a static method, no
 * instance needed.
 */
public final class StarsectorFactionRelations {

    private StarsectorFactionRelations() {
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
     * @param subjectId the faction it is measured against; an id with no text names nobody to hold
     *                  a relation with
     * @return the relation, or none where there is no pair to read one from
     */
    public static Optional<FactionRelation> readRelation(FactionAPI observer, String subjectId) {

        if (observer == null || !KmlibStrings.hasText(subjectId)) {
            return Optional.empty();
        }
        var relationship = observer.getRelationship(subjectId);

        return Optional.of(new FactionRelation(
            resolveRelationLevel(observer, subjectId, relationship),
            RepLevel.getRepInt(relationship),
            StarsectorRelationColours.resolveRelationColour(observer, subjectId, relationship)));
    }

    /**
     * Whether one faction is disposed above neutral toward another.
     *
     * <p>Asked of the level alone, not by testing the relation {@link #readRelation} composes.
     * Routing it through that read would spell the cut once, but it would also resolve a colour to
     * answer a predicate that paints nothing - and resolving one reaches the game's settings, so a
     * pair test that reads a number today would stop answering wherever those are not up. The two
     * spellings are both {@link RepLevel#isPositive()}, the engine's own predicate rather than a
     * threshold either side picks, so there is no rule here to drift from the one on
     * {@link FactionRelation#isAboveNeutral()}.
     *
     * <p>Goodwill is a positive claim: a pair that names nobody answers false rather than taking the
     * benefit of the doubt, so nothing reports warmth it never read.
     *
     * @param faction        the faction whose disposition is read; nothing is read of no faction,
     *                       so it is not above neutral with anyone
     * @param otherFactionId the faction it is disposed toward; an id with no text names nobody to
     *                       be disposed toward
     * @return true where the relation is {@link RepLevel#FAVORABLE} or better
     */
    public static boolean isDispositionAboveNeutral(FactionAPI faction, String otherFactionId) {

        if (faction == null || !KmlibStrings.hasText(otherFactionId)) {
            return false;
        }
        return resolveRelationLevel(faction, otherFactionId, faction.getRelationship(otherFactionId))
            .isPositive();
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
     * captured as a faction, since a reader outlives the relations it is asked about.
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

    // The level the observer holds the pair at, taken from the faction where it names one and
    // reconstructed from the raw relationship where it does not. The one statement of how a level is
    // read, so the whole-relation read and the disposition predicate cannot answer a pair
    // differently - held apart, one would report goodwill the other declined to see.
    private static RepLevel resolveRelationLevel(
            FactionAPI observer, String subjectId, float relationship) {

        var level = observer.getRelationshipLevel(subjectId);

        return level == null
            ? RepLevel.getLevelFor(relationship)
            : level;
    }
}
