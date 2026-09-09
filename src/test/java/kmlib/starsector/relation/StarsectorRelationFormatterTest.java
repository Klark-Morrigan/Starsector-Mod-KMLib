package kmlib.starsector.relation;

import com.fs.starfarer.api.campaign.RepLevel;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static kmlib.starsector.relation.StarsectorRelationFormatter.formatRelation;

import static org.assertj.core.api.Assertions.assertThat;

class StarsectorRelationFormatterTest {

    private static final Color RED = new Color(200, 50, 50);

    @Nested
    class FormatRelation {

        @Test
        void namesTheLevelAndPrintsTheReputationAgainstTheScale() {

            var relation = new FactionRelation(RepLevel.VENGEFUL, -100, RED);

            assertThat(formatRelation(relation))
                .isEqualTo("Vengeful (-100 / 100)");
        }

        @Test
        void printsAPositiveReputationWithoutASign() {

            var relation = new FactionRelation(RepLevel.COOPERATIVE, 85, RED);

            assertThat(formatRelation(relation))
                .isEqualTo("Cooperative (85 / 100)");
        }

        @Test
        void printsTheCentreOfTheScaleAsNought() {

            var relation = new FactionRelation(RepLevel.NEUTRAL, 0, RED);

            assertThat(formatRelation(relation))
                .isEqualTo("Neutral (0 / 100)");
        }
    }
}
