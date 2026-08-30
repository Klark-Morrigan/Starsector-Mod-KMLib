package kmlib.starsector.relation;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.SectorAPI;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static kmlib.starsector.relation.StarsectorFactionRelations.createDispositionReader;
import static kmlib.starsector.relation.StarsectorFactionRelations.isDispositionAboveNeutral;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pins where the threshold falls: {@code FAVORABLE} is above neutral and {@code NEUTRAL} itself is
 * not, which is the whole of what a caller sorting factions by disposition relies on. The level is
 * stubbed rather than the answer, so the step from a standing to "above neutral" is the thing being
 * fixed here.
 *
 * <p>The pair form is pinned on the two things it adds over that threshold and nothing else: which
 * side of the pair is looked up, and that the sector is read per call.
 */
class StarsectorFactionRelationsTest {

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
        void readsAFactionAnsweringNoLevelAsNotAboveNeutral() {
            // A modded faction can answer nothing at all, and goodwill is a positive claim - the
            // absent answer must not be read as warmth.
            assertThat(isDispositionAboveNeutral(buildFactionAt(null), "tritachyon"))
                .isFalse();
        }

        @Test
        void readsNoFactionAtAllAsNotAboveNeutral() {
            assertThat(isDispositionAboveNeutral(null, "tritachyon"))
                .isFalse();
        }

        @Test
        void readsAnUnnamedOtherFactionAsNotAboveNeutralWithoutAskingTheFaction() {
            // Nobody named on the other side, so there is nobody to be disposed toward. Pinned as
            // "never asked" rather than on the answer alone: a faction holding no standing for an
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
            verify(factionMock, never()).getRelationshipLevel(nullable(String.class));
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
            // A reader outlives the standings it is asked about - a hover box holds one for a whole
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

    // A faction standing at one level toward tritachyon and holding no other standing, so a suite
    // states the disposition under examination in a line.
    private static FactionAPI buildFactionAt(RepLevel level) {

        var factionMock = mock(FactionAPI.class);

        when(factionMock.getRelationshipLevel("tritachyon"))
            .thenReturn(level);

        return factionMock;
    }
}
