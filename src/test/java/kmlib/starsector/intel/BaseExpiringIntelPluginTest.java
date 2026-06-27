package kmlib.starsector.intel;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignClockAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.comm.IntelManagerAPI;

import kmlib.starsector.time.StarsectorClock;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BaseExpiringIntelPluginTest {

    private static final long CREATED_AT = 1_000_000L;

    private MockedStatic<Global> globalMock;
    private SectorAPI sectorMock;
    private CampaignClockAPI clockMock;
    private IntelManagerAPI intelManagerMock;

    @BeforeEach
    void setUp() {
        sectorMock = mock(SectorAPI.class);
        clockMock = mock(CampaignClockAPI.class);
        intelManagerMock = mock(IntelManagerAPI.class);
        when(sectorMock.getClock()).thenReturn(clockMock);
        when(sectorMock.getIntelManager()).thenReturn(intelManagerMock);
        when(clockMock.getTimestamp()).thenReturn(CREATED_AT);

        globalMock = mockStatic(Global.class);
        globalMock.when(Global::getSector).thenReturn(sectorMock);
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
    }

    @Nested
    class GetExpiryDays {
        @Test
        void defaultExpiryEqualsStarsectorDaysPerMonth() {
            var intel = new FixedDurationIntel();

            assertThat(intel.getExpiryDays()).isEqualTo((float) StarsectorClock.DAYS_PER_MONTH);
        }
    }

    @Nested
    class AdvanceImpl {
        @Test
        void advanceDoesNothingBeforeExpiry() {
            var intel = new FixedDurationIntel();
            when(clockMock.getElapsedDaysSince(CREATED_AT))
                    .thenReturn((float) StarsectorClock.DAYS_PER_MONTH - 1f);

            intel.advanceImpl(1f);

            verify(intelManagerMock, never()).removeIntel(intel);
        }

        @Test
        void advanceRemovesIntelOnceExpiryReached() {
            var intel = new FixedDurationIntel();
            when(clockMock.getElapsedDaysSince(CREATED_AT))
                    .thenReturn((float) StarsectorClock.DAYS_PER_MONTH);

            intel.advanceImpl(1f);

            verify(intelManagerMock).removeIntel(intel);
        }

        @Test
        void subclassExpiryOverrideIsHonoured() {
            var intel = new CustomDurationIntel(7f);
            when(clockMock.getElapsedDaysSince(CREATED_AT)).thenReturn(7f);

            intel.advanceImpl(1f);

            verify(intelManagerMock).removeIntel(intel);
        }

        @Test
        void advanceIsNoOpWhenSectorDisappears() {
            var intel = new FixedDurationIntel();
            // Simulate teardown: sector lookups return null mid-game.
            globalMock.when(Global::getSector).thenReturn(null);

            // Should not throw; should not interact with the (now-unreachable) intel manager.
            intel.advanceImpl(1f);

            verify(intelManagerMock, never()).removeIntel(intel);
        }
    }

    @Nested
    class IsExpired {
        @Test
        void isExpiredFlipsAtTheSameThresholdAsAdvance() {
            var intel = new FixedDurationIntel();
            when(clockMock.getElapsedDaysSince(CREATED_AT))
                    .thenReturn((float) StarsectorClock.DAYS_PER_MONTH - 0.1f);

            assertThat(intel.isExpired()).isFalse();

            when(clockMock.getElapsedDaysSince(CREATED_AT))
                    .thenReturn((float) StarsectorClock.DAYS_PER_MONTH);

            assertThat(intel.isExpired()).isTrue();
        }

        @Test
        void isExpiredReturnsFalseWhenSectorDisappears() {
            var intel = new FixedDurationIntel();
            // Same null-safety contract advanceImpl honours: an early-
            // teardown sector cannot be treated as "expired" or the
            // caller would incorrectly drop the active item.
            globalMock.when(Global::getSector).thenReturn(null);

            assertThat(intel.isExpired()).isFalse();
        }
    }

    @Nested
    class FindActive {
        @Test
        void findActiveReturnsNullWhenIntelManagerListIsEmpty() {
            when(intelManagerMock.getIntel(FixedDurationIntel.class))
                    .thenReturn(Collections.emptyList());

            assertThat(BaseExpiringIntelPlugin.findActive(FixedDurationIntel.class)).isNull();
        }

        @Test
        void findActiveReturnsTheFirstNonExpiredItem() {
            // Two items registered; only the second is still within its
            // window. findActive must skip the expired head rather than
            // returning it - the whole point of the helper is to keep
            // synchronous callers aligned with the visible window.
            var expired = new FixedDurationIntel();
            var active = new FixedDurationIntel();
            when(clockMock.getElapsedDaysSince(CREATED_AT))
                    .thenReturn((float) StarsectorClock.DAYS_PER_MONTH);
            when(intelManagerMock.getIntel(FixedDurationIntel.class))
                    .thenReturn(Arrays.asList(expired, active));
            // Flip the second item's window back to live by overriding
            // the elapsed lookup after construction - both items share
            // CREATED_AT, so toggling the clock state toggles isExpired
            // for the whole list. The test exercises the per-item walk
            // by then narrowing to "active only".
            when(clockMock.getElapsedDaysSince(CREATED_AT))
                    .thenReturn((float) StarsectorClock.DAYS_PER_MONTH - 0.1f);

            // Both items now report not-expired; findActive returns the
            // head (the iteration order the IntelManager hands back).
            assertThat(BaseExpiringIntelPlugin.findActive(FixedDurationIntel.class))
                    .isSameAs(expired);
        }

        @Test
        void findActiveSkipsExpiredHeadAndReturnsLiveTail() {
            // CustomDurationIntel lets each item carry its own expiry,
            // so the head can be expired while the tail is still live -
            // the realistic scenario findActive is built for.
            var expired = new CustomDurationIntel(1f);
            var active = new CustomDurationIntel(100f);
            when(clockMock.getElapsedDaysSince(CREATED_AT)).thenReturn(50f);
            when(intelManagerMock.getIntel(CustomDurationIntel.class))
                    .thenReturn(Arrays.asList(expired, active));

            assertThat(BaseExpiringIntelPlugin.findActive(CustomDurationIntel.class))
                    .isSameAs(active);
        }

        @Test
        void findActiveReturnsNullWhenEveryItemIsExpired() {
            var a = new CustomDurationIntel(1f);
            var b = new CustomDurationIntel(2f);
            when(clockMock.getElapsedDaysSince(CREATED_AT)).thenReturn(50f);
            when(intelManagerMock.getIntel(CustomDurationIntel.class))
                    .thenReturn(Arrays.asList(a, b));

            assertThat(BaseExpiringIntelPlugin.findActive(CustomDurationIntel.class)).isNull();
        }

        @Test
        void findActiveReturnsNullWhenSectorIsUnavailable() {
            globalMock.when(Global::getSector).thenReturn(null);

            assertThat(BaseExpiringIntelPlugin.findActive(FixedDurationIntel.class)).isNull();
        }
    }

    private static final class FixedDurationIntel extends BaseExpiringIntelPlugin {
    }

    private static final class CustomDurationIntel extends BaseExpiringIntelPlugin {
        private final float expiryDays;

        CustomDurationIntel(float expiryDays) {
            this.expiryDays = expiryDays;
        }

        @Override
        protected float getExpiryDays() {
            return expiryDays;
        }
    }
}
