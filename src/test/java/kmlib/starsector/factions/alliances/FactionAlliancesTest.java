package kmlib.starsector.factions.alliances;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the one question the alliance set answers - whether two factions stand together - the fold
 * that builds it out of plain records, and the direction each errs in where it has nothing to say.
 *
 * <p>The erring is the half worth pinning. A caller spends these answers on whether one faction
 * would act for another, so a set that guessed would have it act on a partnership nobody formed.
 *
 * <p>The fold is read both ways round throughout. It keys on the faction and the question is asked
 * of a pair, so a mapping that had lost one direction would answer differently depending on which
 * faction reached it first.
 */
final class FactionAlliancesTest {

    private static final String ASTRAL_ARMADA = "astral_armada";
    private static final String HEGEMONY = "hegemony";
    private static final String LUDDIC_CHURCH = "luddic_church";
    private static final String PERSEAN_LEAGUE = "persean_league";
    private static final String PIRATES = "pirates";
    private static final String TRITACHYON = "tritachyon";

    private static final String LEAGUE_ALLIANCE = "alliance_league";
    private static final String RIVAL_ALLIANCE = "alliance_rival";

    @Nested
    class AreFactionsAllied {

        @Test
        void readsTwoMembersOfOneAllianceAsAllied() {

            var alliances = new FactionAlliances(Map.of(
                HEGEMONY, LEAGUE_ALLIANCE,
                PERSEAN_LEAGUE, LEAGUE_ALLIANCE));

            assertThat(alliances.areFactionsAllied(HEGEMONY, PERSEAN_LEAGUE))
                .isTrue();
        }

        @Test
        void readsMembersOfTwoAlliancesAsUnallied() {
            // Being in an alliance is not being in this one, which is the whole of what the rule
            // above turns on.
            var alliances = new FactionAlliances(Map.of(
                HEGEMONY, LEAGUE_ALLIANCE,
                PIRATES, RIVAL_ALLIANCE));

            assertThat(alliances.areFactionsAllied(HEGEMONY, PIRATES))
                .isFalse();
        }

        @Test
        void readsAFactionInNoAllianceAsUnallied() {

            var alliances = new FactionAlliances(Map.of(HEGEMONY, LEAGUE_ALLIANCE));

            assertThat(alliances.areFactionsAllied(HEGEMONY, PIRATES))
                .isFalse();
        }

        @Test
        void readsAFactionInNoAllianceAsUnalliedWithItself() {
            // Standing alone is not a relationship, so a faction nothing names answers no
            // differently when asked about itself. Whether two names are the same faction is the
            // caller's own comparison, which this is never asked to stand in for.
            assertThat(FactionAlliances.NONE.areFactionsAllied(PIRATES, PIRATES))
                .isFalse();
        }

        @Test
        void readsASubjectNoFactionHoldsAsUnallied() {
            // What something owned by nobody reaches this with. The map the answer is read out of
            // refuses a null key outright, so the question is turned away before it is asked.
            var alliances = new FactionAlliances(Map.of(HEGEMONY, LEAGUE_ALLIANCE));

            assertThat(alliances.areFactionsAllied(HEGEMONY, null))
                .isFalse();

            assertThat(alliances.areFactionsAllied(null, HEGEMONY))
                .isFalse();
        }

        @Test
        void readsEveryFactionAsUnalliedWhereNoAlliancesWereStated() {
            // What an installation with nothing wired answers, which is a rule reading this as it
            // stood before alliances were read at all.
            assertThat(new FactionAlliances(null).areFactionsAllied(HEGEMONY, PERSEAN_LEAGUE))
                .isFalse();
        }
    }

    @Nested
    class BuildFrom {

        @Test
        void readsTwoMembersOfOneRecordAsAllied() {

            var alliances = FactionAlliances.buildFrom(List.of(buildAlliedPowers()));

            assertThat(alliances.areFactionsAllied(HEGEMONY, ASTRAL_ARMADA))
                .isTrue();
            assertThat(alliances.areFactionsAllied(ASTRAL_ARMADA, HEGEMONY))
                .isTrue();
        }

        @Test
        void readsAFactionNoRecordNamesAsUnallied() {

            var alliances = FactionAlliances.buildFrom(List.of(buildAlliedPowers()));

            assertThat(alliances.areFactionsAllied(HEGEMONY, TRITACHYON))
                .isFalse();
            assertThat(alliances.areFactionsAllied(TRITACHYON, HEGEMONY))
                .isFalse();
        }

        @Test
        void readsMembersOfTwoRecordsAsUnallied() {

            var alliances = FactionAlliances.buildFrom(List.of(
                buildAlliedPowers(),
                new AllianceRecord("alliance-2", "Rival Pact", List.of(LUDDIC_CHURCH))));

            assertThat(alliances.areFactionsAllied(HEGEMONY, LUDDIC_CHURCH))
                .isFalse();
        }

        @Test
        void readsTheLoneMemberOfARecordOfOneAsUnalliedWithAnybodyElse() {
            // An alliance whose partners have all left, which a live manager does hand over. It
            // folds like any other and leaves its last member standing with nobody.
            var alliances = FactionAlliances.buildFrom(List.of(
                new AllianceRecord("alliance-1", "Allied Powers", List.of(HEGEMONY))));

            assertThat(alliances.areFactionsAllied(HEGEMONY, ASTRAL_ARMADA))
                .isFalse();
            assertThat(alliances.areFactionsAllied(ASTRAL_ARMADA, HEGEMONY))
                .isFalse();
        }

        @Test
        void readsNobodyAsAlliedForAMemberlessRecord() {

            var alliances = FactionAlliances.buildFrom(List.of(
                new AllianceRecord("empty-alliance", "Empty Pact", List.of())));

            assertThat(alliances.areFactionsAllied(HEGEMONY, ASTRAL_ARMADA))
                .isFalse();
        }

        @Test
        void resolvesAFactionNamedByTwoRecordsToTheLaterOne() {
            // Visited in list order, so the fold stays total and deterministic per input rather
            // than leaving the faction in whichever record was reached first.
            var alliances = FactionAlliances.buildFrom(List.of(
                buildAlliedPowers(),
                new AllianceRecord(
                    "alliance-2", "Rival Pact", List.of(HEGEMONY, LUDDIC_CHURCH))));

            assertThat(alliances.areFactionsAllied(HEGEMONY, LUDDIC_CHURCH))
                .isTrue();
            assertThat(alliances.areFactionsAllied(HEGEMONY, ASTRAL_ARMADA))
                .isFalse();
        }

        @Test
        void readsNobodyAsAlliedForNoRecords() {

            assertThat(FactionAlliances.buildFrom(List.of()))
                .isEqualTo(FactionAlliances.NONE);
        }

        @Test
        void readsNobodyAsAlliedWhereNoRecordsWereReadAtAll() {

            assertThat(FactionAlliances.buildFrom(null))
                .isEqualTo(FactionAlliances.NONE);
        }

        @Test
        void skipsARecordTheGameNeverNamed() {
            // The memberships are keyed on the alliance's own ID, and one that is absent cannot be
            // compared against another - nor stored, the copy the result takes refusing it.
            var alliances = FactionAlliances.buildFrom(List.of(
                new AllianceRecord(null, "Unnamed Pact", List.of(HEGEMONY, ASTRAL_ARMADA))));

            assertThat(alliances.areFactionsAllied(HEGEMONY, ASTRAL_ARMADA))
                .isFalse();
        }
    }

    @Nested
    class AllianceIdByFactionId {

        @Test
        void keepsTheMembershipsItWasBuiltWithWhenTheSourceMapChangesLater() {
            // Folded once per pass and read by everything in it, so a caller still holding the map
            // must not be able to dissolve an alliance underneath them.
            var memberships = new HashMap<String, String>();

            memberships.put(HEGEMONY, LEAGUE_ALLIANCE);
            memberships.put(PERSEAN_LEAGUE, LEAGUE_ALLIANCE);

            var alliances = new FactionAlliances(memberships);

            memberships.clear();

            assertThat(alliances.areFactionsAllied(HEGEMONY, PERSEAN_LEAGUE))
                .isTrue();
        }
    }

    // Two members, as a live alliance of two arrives: ranked by market size, which this fold has no
    // use for and must not come to depend on.
    private static AllianceRecord buildAlliedPowers() {
        return new AllianceRecord("alliance-1", "Allied Powers", List.of(HEGEMONY, ASTRAL_ARMADA));
    }
}
