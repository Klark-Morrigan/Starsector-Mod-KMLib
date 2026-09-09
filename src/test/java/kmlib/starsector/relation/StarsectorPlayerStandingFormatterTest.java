package kmlib.starsector.relation;

import com.fs.starfarer.api.campaign.RepLevel;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static kmlib.starsector.relation.StarsectorPlayerStandingFormatter.formatPlayerStanding;

import static org.assertj.core.api.Assertions.assertThat;

class StarsectorPlayerStandingFormatterTest {

    private static final Color RED = new Color(200, 50, 50);

    @Nested
    class FormatPlayerStanding {

        @Test
        void namesTheLevelAndPrintsTheReputationAgainstTheScale() {

            var standing = new FactionRelation(RepLevel.VENGEFUL, -100, RED);

            assertThat(formatPlayerStanding(standing))
                .isEqualTo("Vengeful (-100 / 100)");
        }

        @Test
        void printsAPositiveReputationWithoutASign() {

            var standing = new FactionRelation(RepLevel.COOPERATIVE, 85, RED);

            assertThat(formatPlayerStanding(standing))
                .isEqualTo("Cooperative (85 / 100)");
        }

        @Test
        void printsTheCentreOfTheScaleAsNought() {

            var standing = new FactionRelation(RepLevel.NEUTRAL, 0, RED);

            assertThat(formatPlayerStanding(standing))
                .isEqualTo("Neutral (0 / 100)");
        }
    }
}
