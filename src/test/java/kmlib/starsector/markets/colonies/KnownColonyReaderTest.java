package kmlib.starsector.markets.colonies;

import com.fs.starfarer.api.campaign.econ.MarketAPI;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Pins the one answer {@link KnownColonyReader} states for itself: the port a caller was given
 * none of. Every other answer is the consumer's, which is the whole reason this is a port.
 * The cases live in a {@link Nested} group so the suite reports as a per-member tree.
 */
final class KnownColonyReaderTest {

    @Nested
    class NothingKnown {

        @Test
        void names_no_colony_of_a_populated_place() {
            // The conservative answer, and the reason it is the one stated here: a reader handed
            // no port must not name a colony the caller never said could be named.
            var colonies = new Colonies(List.of(
                new Colony(mock(MarketAPI.class), true),
                new Colony(mock(MarketAPI.class), false)));

            assertThat(KnownColonyReader.NOTHING_KNOWN.readKnownColonies(colonies))
                .isEmpty();
        }

        @Test
        void names_nothing_of_a_place_that_could_not_be_read() {

            assertThat(KnownColonyReader.NOTHING_KNOWN.readKnownColonies(Colonies.NONE))
                .isEmpty();
        }
    }
}
