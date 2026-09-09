package kmlib.starsector.factions.relation;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.SectorAPI;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static kmlib.starsector.factions.relation.StarsectorFactionRelations.createDispositionReader;
import static kmlib.starsector.factions.relation.StarsectorFactionRelations.isDispositionAboveNeutral;
import static kmlib.starsector.factions.relation.StarsectorFactionRelations.readRelation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pins where the threshold falls: {@code FAVORABLE} is above neutral and {@code NEUTRAL} itself is
 * not, which is the whole of what a caller sorting factions by disposition relies on. The level is
 * stubbed rather than the answer, so the step from a relation to "above neutral" is the thing being
 * fixed here.
 *
 * <p>The pair form is pinned on the two things it adds over that threshold and nothing else: which
 * side of the pair is looked up, and that the sector is read per call.
 *
 * <p>The whole-relation read is pinned on what it composes: which facets come off the faction
 * directly, which are reconstructed when it answers nothing, and that an unreadable pair is absent
 * rather than indifferent. The reconstruction is pinned from both sides, since the two entry points
 * read a level through one rule and a case proving only the indifferent half would hold whether or
 * not they did.
 */
class StarsectorFactionRelationsTest {

    private static final Color GREEN = new Color(60, 180, 60);
    private static final Color RED = new Color(200, 50, 50);

    @Nested
    class IsDispositionAboveNeutral {

        @Test
        void readsFavourableAsAboveNeutral() {
            // The first level warmer than indifference, so it is the threshold itself.
            assertThat(isDispositionAboveNeutral(buildFactionAt(RepLevel.FAVORABLE), "tritachyon"))
                .isTrue();
        }

        @Test
        void readsCooperativeAsAboveNeutral() {

            assertThat(isDispositionAboveNeutral(buildFactionAt(RepLevel.COOPERATIVE), "tritachyon"))
                .isTrue();
        }

        @Test
        void readsNeutralAsNotAboveNeutral() {
            // The level the scale stops describing goodwill at. Indifference is not warmth, so the
            // landmark is exclusive and this is the case that says so.
            assertThat(isDispositionAboveNeutral(buildFactionAt(RepLevel.NEUTRAL), "tritachyon"))
                .isFalse();
        }

        @Test
        void readsSuspiciousAsNotAboveNeutral() {

            assertThat(isDispositionAboveNeutral(buildFactionAt(RepLevel.SUSPICIOUS), "tritachyon"))
                .isFalse();
        }

        @Test
        void readsVengefulAsNotAboveNeutral() {

            assertThat(isDispositionAboveNeutral(buildFactionAt(RepLevel.VENGEFUL), "tritachyon"))
                .isFalse();
        }

        @Test
        void readsAFactionAnsweringNoLevelByTheIndifferentNumberItReports() {
            // A modded faction can name no level and still report the raw relationship, so the level
            // is reconstructed from the number. This one reports nought, which is indifference.
            assertThat(isDispositionAboveNeutral(buildFactionAt(null), "tritachyon"))
                .isFalse();
        }

        @Test
        void readsAFactionAnsweringNoLevelByTheFavourableNumberItReports() {
            // The half the reconstruction is actually for, and the case that pins this predicate to
            // the same reading readRelation makes: goodwill the faction reports only as a number is
            // still goodwill, and reading the absent level as "not above neutral" would deny it.
            var factionMock = buildFactionAt(null);

            when(factionMock.getRelationship("tritachyon"))
                .thenReturn(0.6f);

            assertThat(isDispositionAboveNeutral(factionMock, "tritachyon"))
                .isTrue();
        }

        @Test
        void readsNoFactionAtAllAsNotAboveNeutral() {

            assertThat(isDispositionAboveNeutral(null, "tritachyon"))
                .isFalse();
        }

        @Test
        void readsAnUnnamedOtherFactionAsNotAboveNeutralWithoutAskingTheFaction() {
            // Nobody named on the other side, so there is nobody to be disposed toward. Pinned as
            // "never asked" rather than on the answer alone: a faction holding no relation for an
            // unnamed id reads false either way, so the answer cannot fail if the guard goes and
            // the unnamed id is put to a live faction that faults on one.
            var factionMock = buildFactionAt(RepLevel.COOPERATIVE);

            assertThat(isDispositionAboveNeutral(factionMock, null))
                .isFalse();
            assertThat(isDispositionAboveNeutral(factionMock, " "))
                .isFalse();

            // Matched with nullable rather than any: the overload has to be named by type, and a
            // plain typed matcher stands for no null - which would let exactly the call this guards
            // against past the verification.
            verify(factionMock, never())
                .getRelationshipLevel(nullable(String.class));
        }
    }

    @Nested
    class CreateDispositionReader {

        @Test
        void readsTheFirstOfThePairAsTheFactionDisposedTowardTheSecond() {
            // Which side of the pair is looked up is the whole of what a caller composing
            // dispositions relies on, and it is invisible in the answer wherever both factions
            // happen to agree - so the case stands up a sector the two disagree in.
            // Each faction is built before the sector is told about it: stubbing one mock inside
            // another stub's argument leaves Mockito mid-stubbing and fails the whole fixture.
            var wellDisposedMock = buildFactionAt(RepLevel.COOPERATIVE);
            var sourMock = buildFactionAt(RepLevel.VENGEFUL);
            var sectorMock = mock(SectorAPI.class);

            when(sectorMock.getFaction("hegemony"))
                .thenReturn(wellDisposedMock);
            when(sectorMock.getFaction("tritachyon"))
                .thenReturn(sourMock);

            var reader = createDispositionReader(sectorMock);

            assertThat(reader.test("hegemony", "tritachyon"))
                .isTrue();
            assertThat(reader.test("tritachyon", "tritachyon"))
                .isFalse();
        }

        @Test
        void readsAFactionTheSectorDoesNotKnowAsNotAboveNeutral() {
            // An id the sector answers nothing for is nobody to be disposed toward anyone, which is
            // the shape a caller composing over a stale membership arrives in.
            var sectorMock = mock(SectorAPI.class);

            assertThat(createDispositionReader(sectorMock).test("ghost_faction", "tritachyon"))
                .isFalse();
        }

        @Test
        void readsNoSectorAtAllAsNotAboveNeutral() {
            // Nothing to look either faction up in. Goodwill is a positive claim, so the reader
            // answers no rather than faulting on a caller that has no sector to hand.
            assertThat(createDispositionReader(null).test("hegemony", "tritachyon"))
                .isFalse();
        }

        @Test
        void readsTheSectorPerCallRatherThanCapturingWhatItAnsweredFirst() {
            // A reader outlives the relations it is asked about - a hover box holds one for a whole
            // paint while the game goes on running - so a faction re-registered between two calls
            // has to answer as it stands then, not as it stood when the reader was built.
            var sourMock = buildFactionAt(RepLevel.VENGEFUL);
            var wellDisposedMock = buildFactionAt(RepLevel.COOPERATIVE);
            var sectorMock = mock(SectorAPI.class);

            when(sectorMock.getFaction("hegemony"))
                .thenReturn(sourMock);

            var reader = createDispositionReader(sectorMock);

            assertThat(reader.test("hegemony", "tritachyon"))
                .isFalse();

            when(sectorMock.getFaction("hegemony"))
                .thenReturn(wellDisposedMock);

            assertThat(reader.test("hegemony", "tritachyon"))
                .isTrue();
        }
    }

    @Nested
    class ReadRelation {

        @Test
        void readsLevelReputationAndColourForThePair() {

            var factionMock = mock(FactionAPI.class);

            when(factionMock.getRelationship("tritachyon"))
                .thenReturn(0.6f);
            when(factionMock.getRelationshipLevel("tritachyon"))
                .thenReturn(RepLevel.FRIENDLY);
            when(factionMock.getRelColor("tritachyon"))
                .thenReturn(GREEN);

            assertThat(readRelation(factionMock, "tritachyon"))
                .contains(new FactionRelation(RepLevel.FRIENDLY, 60, GREEN));
        }

        @Test
        void reconstructsTheLevelFromTheRawRelationshipWhenTheFactionNamesNone() {
            // A modded faction can answer nothing at all for the level while still reporting the
            // number, and the scale covers the whole float range - so the relation comes back with a
            // level rather than with a hole in it.
            var factionMock = mock(FactionAPI.class);

            when(factionMock.getRelationship("tritachyon"))
                .thenReturn(-0.8f);
            when(factionMock.getRelColor("tritachyon"))
                .thenReturn(RED);

            assertThat(readRelation(factionMock, "tritachyon"))
                .contains(new FactionRelation(RepLevel.VENGEFUL, -80, RED));
        }

        @Test
        void readsIndifferenceAsARelationRatherThanAsAbsence() {
            // A pair with no history reports nought, which is a relation on the scale - only a pair
            // that cannot be looked up at all answers nothing, which is what lets a caller folding
            // several factions skip the unreadable ones without dragging the centre into its answer.
            var factionMock = mock(FactionAPI.class);

            when(factionMock.getRelColor("tritachyon"))
                .thenReturn(GREEN);

            assertThat(readRelation(factionMock, "tritachyon"))
                .contains(new FactionRelation(RepLevel.NEUTRAL, 0, GREEN));
        }

        @Test
        void readsNoFactionAtAllAsNoRelation() {

            assertThat(readRelation(null, "tritachyon"))
                .isEmpty();
        }

        @Test
        void readsAnUnnamedOtherFactionAsNoRelation() {
            // Nobody named on the other side, so there is nobody to hold a relation with - and an
            // indifferent-looking answer would be indistinguishable from a real relation at nought.
            assertThat(readRelation(mock(FactionAPI.class), " "))
                .isEmpty();
        }
    }

    // A faction standing at one level toward tritachyon and holding no other, so a suite
    // states the disposition under examination in a line.
    private static FactionAPI buildFactionAt(RepLevel level) {

        var factionMock = mock(FactionAPI.class);

        when(factionMock.getRelationshipLevel("tritachyon"))
            .thenReturn(level);

        return factionMock;
    }
}
