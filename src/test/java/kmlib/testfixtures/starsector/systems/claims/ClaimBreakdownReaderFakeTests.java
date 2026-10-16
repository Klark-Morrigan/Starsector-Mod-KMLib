package kmlib.testfixtures.starsector.systems.claims;

import com.fs.starfarer.api.campaign.StarSystemAPI;

import kmlib.starsector.systems.claims.SystemClaimBreakdown;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins the claim breakdown reader a claim-explaining subject is exercised against: a stored
 * breakdown is read back by system ID, an unreadable system reads as nothing, and the claimant and
 * decree reads answer off the stored breakdown so a test cannot set them to disagree.
 */
final class ClaimBreakdownReaderFakeTests {

    private static final SystemClaimBreakdown DECREED_CONTEST = new SystemClaimBreakdown(
        "luddic_church",
        "luddic_church",
        List.of(ClaimStandingFixture.buildStandingOnOneMarket("hegemony", 6, true)));

    private final ClaimBreakdownReaderFake readerFake = new ClaimBreakdownReaderFake();

    @Nested
    class ReadBreakdown {

        @Test
        void answersTheBreakdownStoredForTheSystem() {

            readerFake.setBreakdown("eos", DECREED_CONTEST);

            assertThat(readerFake.readBreakdown(buildSystem("eos")))
                .isSameAs(DECREED_CONTEST);
        }

        @Test
        void answersNoneForAnUnmappedSystem() {

            assertThat(readerFake.readBreakdown(buildSystem("eos")))
                .isSameAs(SystemClaimBreakdown.NONE);
        }

        @Test
        void answersNoneForANullSystemOrOneWithNoId() {

            assertThat(readerFake.readBreakdown(null))
                .isSameAs(SystemClaimBreakdown.NONE);
            assertThat(readerFake.readBreakdown(buildSystem(null)))
                .isSameAs(SystemClaimBreakdown.NONE);
        }
    }

    @Nested
    class ReadClaimingFactionId {

        @Test
        void answersTheStoredBreakdownsClaimant() {

            readerFake.setBreakdown("eos", new SystemClaimBreakdown(null, "hegemony", null));

            assertThat(readerFake.readClaimingFactionId(buildSystem("eos")))
                .isEqualTo("hegemony");
        }

        @Test
        void answersUnclaimedForAnUnmappedSystem() {

            assertThat(readerFake.readClaimingFactionId(buildSystem("eos")))
                .isNull();
        }
    }

    @Nested
    class ReadDecreedFactionId {

        @Test
        void answersTheStoredBreakdownsDecree() {

            readerFake.setBreakdown("eos", DECREED_CONTEST);

            assertThat(readerFake.readDecreedFactionId(buildSystem("eos")))
                .isEqualTo("luddic_church");
        }

        @Test
        void answersNoDecreeForAnUnmappedSystem() {

            assertThat(readerFake.readDecreedFactionId(buildSystem("eos")))
                .isNull();
        }
    }

    private static StarSystemAPI buildSystem(String systemId) {

        var systemMock = mock(StarSystemAPI.class);

        when(systemMock.getId())
            .thenReturn(systemId);

        return systemMock;
    }
}
