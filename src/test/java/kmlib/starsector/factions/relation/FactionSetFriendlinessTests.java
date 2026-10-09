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
}
