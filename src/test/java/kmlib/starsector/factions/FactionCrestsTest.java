package kmlib.starsector.factions;

import com.fs.starfarer.api.campaign.FactionAPI;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins {@link FactionCrests}'s absent-aware crest read - the single rule every crest-drawing surface
 * shares: an authored crest resolves to its trimmed path, while a null faction, a null crest, or a
 * blank one all collapse to the null path a caller draws around by showing the name alone.
 */
final class FactionCrestsTest {

    @Nested
    class ResolveCrestPath {

        @Test
        void resolveCrestPathReturnsThePathForACrestedFaction() {
            var factionMock = mock(FactionAPI.class);
            when(factionMock.getCrest()).thenReturn("graphics/hegemony_crest.png");

            assertThat(FactionCrests.resolveCrestPath(factionMock))
                    .isEqualTo("graphics/hegemony_crest.png");
        }

        @Test
        void resolveCrestPathIsNullForANullFaction() {
            assertThat(FactionCrests.resolveCrestPath(null)).isNull();
        }

        @Test
        void resolveCrestPathIsNullForANullCrest() {
            var factionMock = mock(FactionAPI.class);
            when(factionMock.getCrest()).thenReturn(null);

            assertThat(FactionCrests.resolveCrestPath(factionMock)).isNull();
        }

        @Test
        void resolveCrestPathIsNullForABlankCrest() {
            // An authored-but-empty crest string reads as no crest, so a whitespace path collapses to
            // null rather than pointing a caller at a missing sprite.
            var factionMock = mock(FactionAPI.class);
            when(factionMock.getCrest()).thenReturn("   ");

            assertThat(FactionCrests.resolveCrestPath(factionMock)).isNull();
        }
    }
}
