package kmlib.starsector.relation;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.RepLevel;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static kmlib.starsector.relation.StarsectorFactionRelations.isDispositionAboveNeutral;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins where the threshold falls: {@code FAVORABLE} is above neutral and {@code NEUTRAL} itself is
 * not, which is the whole of what a caller sorting factions by disposition relies on. The level is
 * stubbed rather than the answer, so the step from a standing to "above neutral" is the thing being
 * fixed here.
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
        void readsAnUnnamedOtherFactionAsNotAboveNeutral() {
            // Nobody named on the other side, so there is nobody to be disposed toward - answered
            // rather than put to a faction that would read it as an unknown id.
            assertThat(isDispositionAboveNeutral(buildFactionAt(RepLevel.COOPERATIVE), null))
                .isFalse();
            assertThat(isDispositionAboveNeutral(buildFactionAt(RepLevel.COOPERATIVE), " "))
                .isFalse();
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
