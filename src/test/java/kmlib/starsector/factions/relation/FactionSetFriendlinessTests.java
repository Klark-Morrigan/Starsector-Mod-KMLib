package kmlib.starsector.factions.relation;

import com.fs.starfarer.api.campaign.SectorAPI;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

/**
 * Pins the set-level reading of a faction-level disposition: unanimous across both sets or not
 * friendly at all, how much of a set falls short where it is not, and how much of the far set one
 * faction falls short of. The faction-level answer is a hand-built pair list, so what is
 * fixed here is how the pairs are composed rather than where the game's own threshold falls.
 *
 * <p>That list is read one way round on purpose. Every answer here takes its pairs from the faction
 * named first, which is what stops a faction being sorted by one reading and counted by another, so
 * the cases state a warm pair in one direction and leave the other cold.
 */
final class FactionSetFriendlinessTests {

    @Nested
    class CreateForSector {

        @Test
        void readsDispositionsThroughTheSectorsOwnReader() {
            // Bound to the reader the sector's relations answer through, so the rule and every other
            // surface composing dispositions read one pair the same way round.
            var sectorMock = mock(SectorAPI.class);
            BiPredicate<String, String> hegemonyFriendly = (factionId, otherFactionId) -> "hegemony".equals(factionId);

            try (var relationsMock = mockStatic(FactionRelations.class)) {

                relationsMock
                    .when(() -> FactionRelations.createDispositionReader(sectorMock))
                    .thenReturn(hegemonyFriendly);

                var friendliness = FactionSetFriendliness.createForSector(sectorMock);

                assertThat(friendliness.areSetsFriendly(Set.of("hegemony"), Set.of("tritachyon")))
                    .isTrue();
                assertThat(friendliness.areSetsFriendly(Set.of("tritachyon"), Set.of("hegemony")))
                    .isFalse();
            }
        }
    }

    @Nested
    class CountMembersAtOddsWith {

        @Test
        void countsNoneWhereEveryMemberIsFriendlyWithTheWholeOtherSet() {

            var friendliness = buildFriendlinessAboveNeutralOn(
                List.of("hegemony:tritachyon", "luddic_church:tritachyon"));

            assertThat(friendliness.countMembersAtOddsWith(
                    Set.of("hegemony", "luddic_church"),
                    Set.of("tritachyon")))
                .isZero();
        }

        @Test
        void countsAMemberOnceHoweverManyOfTheOtherSetItQuarrelsWith() {
            // Counted per member and not per pair: one faction at odds with both of the other set is
            // still one member of two.
            var friendliness = buildFriendlinessAboveNeutralOn(
                List.of("hegemony:tritachyon", "hegemony:persean"));

            assertThat(friendliness.countMembersAtOddsWith(
                    Set.of("hegemony", "luddic_church"),
                    Set.of("tritachyon", "persean")))
                .isOne();
        }

        @Test
        void countsEveryMemberWhereNoneIsFriendly() {

            var friendliness = buildFriendlinessAboveNeutralOn(List.of());

            assertThat(friendliness.countMembersAtOddsWith(
                    Set.of("hegemony", "luddic_church"),
                    Set.of("tritachyon")))
                .isEqualTo(2);
        }

        @Test
        void countsEveryMemberWhereTheOtherSetIsEmpty() {
            // The same positive statement the yes-or-no is: nobody is friendly with nobody, which is what
            // leaves that answer needing no guard of its own for this side.
            var friendliness = buildFriendlinessAboveNeutralOn(List.of("hegemony:tritachyon"));

            assertThat(friendliness.countMembersAtOddsWith(Set.of("hegemony"), Set.of()))
                .isOne();
        }
    }

    @Nested
    class CountFactionsAtOddsWith {

        @Test
        void countsEveryOneOfTheOtherSetOneFactionFallsShortOf() {
            // The far side of the same walk: how much of the other set one faction quarrels with.
            var friendliness = buildFriendlinessAboveNeutralOn(List.of("hegemony:tritachyon"));

            assertThat(friendliness.countFactionsAtOddsWith(
                    "hegemony",
                    Set.of("tritachyon", "persean", "luddic_church")))
                .isEqualTo(2);
        }

        @Test
        void countsNoneWhereTheFactionIsFriendlyWithAllOfThem() {

            var friendliness = buildFriendlinessAboveNeutralOn(
                List.of("hegemony:tritachyon", "hegemony:persean"));

            assertThat(friendliness.countFactionsAtOddsWith(
                    "hegemony",
                    Set.of("tritachyon", "persean")))
                .isZero();
        }

        @Test
        void readsThePairFromTheFactionItIsCountingFor() {
            // The direction every rule here shares. A table warm one way only leaves this counting
            // the faction's own view, so a faction sorted friendly cannot then be counted at odds -
            // which asking the pair from the other set's end would allow.
            var friendliness = buildFriendlinessAboveNeutralOn(List.of("hegemony:tritachyon"));

            assertThat(friendliness.countFactionsAtOddsWith("hegemony", Set.of("tritachyon")))
                .isZero();
            assertThat(friendliness.countFactionsAtOddsWith("tritachyon", Set.of("hegemony")))
                .isOne();
        }
    }

    // A rule whose faction-level answer is above neutral for exactly the named pairs, each written
    // "<faction>:<other faction>" so a case states its whole disposition table in one line.
    private static FactionSetFriendliness buildFriendlinessAboveNeutralOn(List<String> aboveNeutralPairs) {

        return new FactionSetFriendliness(
            (factionId, otherFactionId) ->
                aboveNeutralPairs.contains(factionId + ":" + otherFactionId));
    }
}
