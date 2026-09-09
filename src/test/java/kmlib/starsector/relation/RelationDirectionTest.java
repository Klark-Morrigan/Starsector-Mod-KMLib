package kmlib.starsector.relation;

import com.fs.starfarer.api.campaign.RepLevel;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RelationDirectionTest {

    private static final Color GREY = new Color(125, 125, 125);

    @Nested
    class IsWithinBand {

        @Test
        void placesIllWillInTheHostileBand() {

            assertThat(RelationDirection.MOST_HOSTILE.isWithinBand(createRelationAt(-0.50f)))
                .isTrue();
        }

        @Test
        void placesGoodwillOutsideTheHostileBand() {

            assertThat(RelationDirection.MOST_HOSTILE.isWithinBand(createRelationAt(0.50f)))
                .isFalse();
        }

        @Test
        void placesGoodwillInTheFriendlyBand() {

            assertThat(RelationDirection.FRIENDLIEST.isWithinBand(createRelationAt(0.50f)))
                .isTrue();
        }

        @Test
        void placesIllWillOutsideTheFriendlyBand() {

            assertThat(RelationDirection.FRIENDLIEST.isWithinBand(createRelationAt(-0.50f)))
                .isFalse();
        }

        @Test
        void placesIndifferenceInNeitherBand() {

            // The neutral band belongs to no direction, which is what keeps a set of wholly
            // indifferent factions from reading as uniform at both ends at once.
            var indifferent = createRelationAt(0.00f);

            assertThat(RelationDirection.MOST_HOSTILE.isWithinBand(indifferent)).isFalse();
            assertThat(RelationDirection.FRIENDLIEST.isWithinBand(indifferent)).isFalse();
        }

        @Test
        void placesTheTopOfTheNeutralBandOutsideTheFriendlyBand() {

            assertThat(RelationDirection.FRIENDLIEST.isWithinBand(createRelationAt(0.09f)))
                .isFalse();
        }

        @Test
        void placesOneStepPastTheNeutralBandInTheFriendlyBand() {

            assertThat(RelationDirection.FRIENDLIEST.isWithinBand(createRelationAt(0.10f)))
                .isTrue();
        }

        @Test
        void placesTheBottomOfTheNeutralBandOutsideTheHostileBand() {

            assertThat(RelationDirection.MOST_HOSTILE.isWithinBand(createRelationAt(-0.09f)))
                .isFalse();
        }

        @Test
        void placesOneStepPastTheNeutralBandInTheHostileBand() {

            assertThat(RelationDirection.MOST_HOSTILE.isWithinBand(createRelationAt(-0.10f)))
                .isTrue();
        }

        @Test
        void placesNoRelationInNoBand() {

            assertThat(RelationDirection.MOST_HOSTILE.isWithinBand(null)).isFalse();
            assertThat(RelationDirection.FRIENDLIEST.isWithinBand(null)).isFalse();
        }
    }

    @Nested
    class ResolveDecidingOrder {

        @Test
        void ranksTheLeastFriendlyFirstUnderTheHostileDirection() {

            var relations = createRelationsAt(40, -70, 5);

            relations.sort(RelationDirection.MOST_HOSTILE.resolveDecidingOrder());

            assertThat(relations)
                .extracting(FactionRelation::reputation)
                .containsExactly(-70, 5, 40);
        }

        @Test
        void ranksTheFriendliestFirstUnderTheFriendlyDirection() {

            var relations = createRelationsAt(40, -70, 5);

            relations.sort(RelationDirection.FRIENDLIEST.resolveDecidingOrder());

            assertThat(relations)
                .extracting(FactionRelation::reputation)
                .containsExactly(40, 5, -70);
        }

        @Test
        void ranksEqualReadingsAsEqual() {

            var order = RelationDirection.MOST_HOSTILE.resolveDecidingOrder();

            assertThat(order.compare(createRelationAt(-0.30f), createRelationAt(-0.30f)))
                .isZero();
        }
    }

    // A mutable list, since the ordering cases sort in place.
    private static List<FactionRelation> createRelationsAt(int... reputations) {

        var relations = new ArrayList<FactionRelation>();

        for (var reputation : reputations) {
            relations.add(new FactionRelation(RepLevel.NEUTRAL, reputation, GREY));
        }
        return relations;
    }

    // The relation the game would report for a raw relationship value, so a band boundary is stated
    // as the number a save actually holds rather than as the level it is expected to land in.
    private static FactionRelation createRelationAt(float relationship) {

        return new FactionRelation(
            RepLevel.getLevelFor(relationship),
            RepLevel.getRepInt(relationship),
            GREY);
    }
}
