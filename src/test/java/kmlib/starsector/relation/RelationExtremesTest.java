package kmlib.starsector.relation;

import com.fs.starfarer.api.campaign.RepLevel;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RelationExtremesTest {

    private static final Color BLUE = new Color(50, 50, 200);
    private static final Color GREY = new Color(125, 125, 125);
    private static final Color RED = new Color(200, 50, 50);

    @Nested
    class ResolveExtreme {

        @Test
        void resolvesTheLeastFriendlyUnderTheHostileDirection() {

            var relations = createRelationsAt(40, -70, 5);

            assertThat(RelationExtremes.resolveExtreme(RelationDirection.MOST_HOSTILE, relations))
                .hasValueSatisfying(relation -> assertThat(relation.reputation()).isEqualTo(-70));
        }

        @Test
        void resolvesTheFriendliestUnderTheFriendlyDirection() {

            var relations = createRelationsAt(40, -70, 5);

            assertThat(RelationExtremes.resolveExtreme(RelationDirection.FRIENDLIEST, relations))
                .hasValueSatisfying(relation -> assertThat(relation.reputation()).isEqualTo(40));
        }

        @Test
        void resolvesTheOnlyRelationOfASetOfOne() {

            var only = new FactionRelation(RepLevel.WELCOMING, 33, BLUE);

            assertThat(RelationExtremes.resolveExtreme(RelationDirection.MOST_HOSTILE, List.of(only)))
                .contains(only);
        }

        @Test
        void resolvesTheFirstOfTwoEqualReadings() {

            // Equal readings are equally true, so the collection's own order is what settles them -
            // stated here so a later fold cannot quietly start keeping the last instead.
            var first = new FactionRelation(RepLevel.HOSTILE, -50, RED);
            var second = new FactionRelation(RepLevel.HOSTILE, -50, BLUE);

            assertThat(RelationExtremes.resolveExtreme(
                    RelationDirection.MOST_HOSTILE,
                    List.of(first, second)))
                .containsSame(first);
        }

        @Test
        void resolvesNothingFromAnEmptySet() {

            assertThat(RelationExtremes.resolveExtreme(RelationDirection.MOST_HOSTILE, List.of()))
                .isEmpty();
        }

        @Test
        void resolvesNothingWithoutASet() {

            assertThat(RelationExtremes.resolveExtreme(RelationDirection.MOST_HOSTILE, null))
                .isEmpty();
        }

        @Test
        void resolvesNothingWithoutADirection() {

            assertThat(RelationExtremes.resolveExtreme(null, createRelationsAt(40, -70)))
                .isEmpty();
        }

        @Test
        void passesOverEntriesHoldingNoRelation() {

            var relations = new ArrayList<FactionRelation>(
                Arrays.asList(null, new FactionRelation(RepLevel.HOSTILE, -60, RED)));

            assertThat(RelationExtremes.resolveExtreme(RelationDirection.MOST_HOSTILE, relations))
                .hasValueSatisfying(relation -> assertThat(relation.reputation()).isEqualTo(-60));
        }
    }

    @Nested
    class ResolveBest {

        @Test
        void resolvesTheLeastHostileOfAWhollyHostileSet() {

            // The friendly end of a set with no friend in it is still a real reading: it is the
            // best on offer, not an absence.
            assertThat(RelationExtremes.resolveBest(createRelationsAt(-80, -20, -55)))
                .hasValueSatisfying(relation -> assertThat(relation.reputation()).isEqualTo(-20));
        }

        @Test
        void resolvesTheWarmestOfAWhollyFriendlySet() {

            assertThat(RelationExtremes.resolveBest(createRelationsAt(15, 90, 60)))
                .hasValueSatisfying(relation -> assertThat(relation.reputation()).isEqualTo(90));
        }

        @Test
        void resolvesNothingFromAnEmptySet() {

            assertThat(RelationExtremes.resolveBest(List.of()))
                .isEmpty();
        }
    }

    @Nested
    class ResolveWorst {

        @Test
        void resolvesTheDeepestOfAWhollyHostileSet() {

            assertThat(RelationExtremes.resolveWorst(createRelationsAt(-80, -20, -55)))
                .hasValueSatisfying(relation -> assertThat(relation.reputation()).isEqualTo(-80));
        }

        @Test
        void resolvesTheCoolestOfAWhollyFriendlySet() {

            // Nothing hostile is present, so the hostile end reports the nearest thing to it - the
            // read answers where a set stands, not whether anybody in it is an enemy.
            assertThat(RelationExtremes.resolveWorst(createRelationsAt(15, 90, 60)))
                .hasValueSatisfying(relation -> assertThat(relation.reputation()).isEqualTo(15));
        }

        @Test
        void resolvesNothingFromAnEmptySet() {

            assertThat(RelationExtremes.resolveWorst(List.of()))
                .isEmpty();
        }
    }

    // Relations distinguished only by the number the end of a set is taken over, the level and the
    // shade being nothing this class reads.
    private static List<FactionRelation> createRelationsAt(int... reputations) {

        var relations = new ArrayList<FactionRelation>();

        for (var reputation : reputations) {
            relations.add(new FactionRelation(RepLevel.NEUTRAL, reputation, GREY));
        }
        return relations;
    }
}
