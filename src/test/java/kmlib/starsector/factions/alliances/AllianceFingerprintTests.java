package kmlib.starsector.factions.alliances;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins {@link AllianceFingerprint}: the token is stable for a fixed membership and moves only when
 * an alliance forms, dissolves, gains or loses a member, or changes its lead. Order must not
 * matter - neither the order a source reports alliances in nor the market-size order of the members
 * below the lead - so a pure reshuffle reads as no change. Built on hand-made records so the token is
 * proven without a live alliance source.
 */
class AllianceFingerprintTests {

    @Nested
    class ComputeFingerprint {

        @Test
        void yieldsTheSameTokenForAnUnchangedSet() {

            assertThat(AllianceFingerprint.computeFingerprint(List.of(buildAlliedPowers())))
                .isEqualTo(AllianceFingerprint.computeFingerprint(List.of(buildAlliedPowers())));
        }

        @Test
        void ignoresReorderOfMembersBelowTheLead() {
            // Same lead (hegemony), the two trailing members swapped: neither who is allied nor
            // the lead changed, so the token must not move.
            var oneOrder = new AllianceRecord(
                "alliance-1",
                "Allied Powers",
                List.of("hegemony", "astral_armada", "diktat"));

            var otherOrder = new AllianceRecord(
                "alliance-1",
                "Allied Powers",
                List.of("hegemony", "diktat", "astral_armada"));

            assertThat(AllianceFingerprint.computeFingerprint(List.of(otherOrder)))
                .isEqualTo(AllianceFingerprint.computeFingerprint(List.of(oneOrder)));
        }

        @Test
        void movesWhenTheLeadMemberSwaps() {
            // The same two members, a different dominant (element 0): the lead swapped, so the
            // token must move even though the membership set is identical.
            var relead = new AllianceRecord(
                "alliance-1",
                "Allied Powers",
                List.of("astral_armada", "hegemony"));

            assertThat(AllianceFingerprint.computeFingerprint(List.of(relead)))
                .isNotEqualTo(AllianceFingerprint.computeFingerprint(List.of(buildAlliedPowers())));
        }

        @Test
        void ignoresTheOrderAlliancesAreReportedIn() {

            var first = List.of(buildAlliedPowers(), buildRivalBloc());
            var second = List.of(buildRivalBloc(), buildAlliedPowers());

            assertThat(AllianceFingerprint.computeFingerprint(second))
                .isEqualTo(AllianceFingerprint.computeFingerprint(first));
        }

        @Test
        void movesWhenAnAllianceGainsAMember() {

            var grown = new AllianceRecord(
                "alliance-1",
                "Allied Powers",
                List.of("hegemony", "astral_armada", "diktat"));

            assertThat(AllianceFingerprint.computeFingerprint(List.of(grown)))
                .isNotEqualTo(AllianceFingerprint.computeFingerprint(List.of(buildAlliedPowers())));
        }

        @Test
        void movesWhenAnAllianceLosesAMember() {

            var shrunk = new AllianceRecord(
                "alliance-1",
                "Allied Powers",
                List.of("hegemony"));

            assertThat(AllianceFingerprint.computeFingerprint(List.of(shrunk)))
                .isNotEqualTo(AllianceFingerprint.computeFingerprint(List.of(buildAlliedPowers())));
        }

        @Test
        void movesWhenAnAllianceForms() {

            assertThat(AllianceFingerprint.computeFingerprint(List.of(buildAlliedPowers(), buildRivalBloc())))
                .isNotEqualTo(AllianceFingerprint.computeFingerprint(List.of(buildAlliedPowers())));
        }

        @Test
        void movesWhenAnAllianceDissolves() {

            assertThat(AllianceFingerprint.computeFingerprint(List.of()))
                .isNotEqualTo(AllianceFingerprint.computeFingerprint(List.of(buildAlliedPowers())));
        }
    }

    // Two members ranked hegemony-first; reused across the steady-token and membership-move
    // assertions.
    private static AllianceRecord buildAlliedPowers() {

        return new AllianceRecord(
            "alliance-1",
            "Allied Powers",
            List.of("hegemony", "astral_armada"));
    }

    // A second, distinct alliance, so forming and reordering can be probed against
    // alliedPowers.
    private static AllianceRecord buildRivalBloc() {

        return new AllianceRecord(
            "alliance-2",
            "Rival Bloc",
            List.of("tritachyon", "luddic_church"));
    }
}
