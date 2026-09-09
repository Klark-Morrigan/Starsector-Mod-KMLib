package kmlib.starsector.relation;

import com.fs.starfarer.api.campaign.RepLevel;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.assertj.core.api.Assertions.assertThat;

class FactionRelationTest {

    private static final Color RED = new Color(200, 50, 50);

    @Nested
    class IsAboveNeutral {

        @Test
        void answersAboveNeutralAtTheFirstLevelOfGoodwill() {

            assertThat(new FactionRelation(RepLevel.FAVORABLE, 15, RED).isAboveNeutral())
                .isTrue();
        }

        @Test
        void answersAboveNeutralAtTheTopOfTheScale() {

            assertThat(new FactionRelation(RepLevel.COOPERATIVE, 100, RED).isAboveNeutral())
                .isTrue();
        }

        @Test
        void answersNotAboveNeutralAtIndifference() {

            assertThat(new FactionRelation(RepLevel.NEUTRAL, 0, RED).isAboveNeutral())
                .isFalse();
        }

        @Test
        void answersNotAboveNeutralAtTheFirstLevelOfIllWill() {

            assertThat(new FactionRelation(RepLevel.SUSPICIOUS, -15, RED).isAboveNeutral())
                .isFalse();
        }

        @Test
        void answersNotAboveNeutralAtTheTopOfTheNeutralBand() {

            // The band reaches +9, so a faintly positive reputation is still indifference - the cut
            // this method draws is the game's own, not the sign of the number.
            assertThat(createRelationAt(0.09f).isAboveNeutral())
                .isFalse();
        }

        @Test
        void answersAboveNeutralOneStepPastTheNeutralBand() {

            assertThat(createRelationAt(0.10f).isAboveNeutral())
                .isTrue();
        }
    }

    // The relation the game would report for a raw relationship value, so a boundary case is stated
    // as the number a save actually holds rather than as the level it is expected to land in.
    private static FactionRelation createRelationAt(float relationship) {

        return new FactionRelation(
            RepLevel.getLevelFor(relationship),
            RepLevel.getRepInt(relationship),
            RED);
    }
}
