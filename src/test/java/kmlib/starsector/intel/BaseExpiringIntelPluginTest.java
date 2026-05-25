package kmlib.starsector.intel;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignClockAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.comm.IntelManagerAPI;
import kmlib.starsector.time.StarsectorClock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BaseExpiringIntelPluginTest {

    private static final long CREATED_AT = 1_000_000L;

    private MockedStatic<Global> globalStatic;
    private SectorAPI sector;
    private CampaignClockAPI clock;
    private IntelManagerAPI intelManager;

    @BeforeEach
    void setUp() {
        sector = mock(SectorAPI.class);
        clock = mock(CampaignClockAPI.class);
        intelManager = mock(IntelManagerAPI.class);
        when(sector.getClock()).thenReturn(clock);
        when(sector.getIntelManager()).thenReturn(intelManager);
        when(clock.getTimestamp()).thenReturn(CREATED_AT);

        globalStatic = mockStatic(Global.class);
        globalStatic.when(Global::getSector).thenReturn(sector);
    }

    @AfterEach
    void tearDown() {
        globalStatic.close();
    }

    @Test
    void defaultExpiryEqualsStarsectorDaysPerMonth() {
        FixedDurationIntel intel = new FixedDurationIntel();

        assertThat(intel.getExpiryDays()).isEqualTo((float) StarsectorClock.DAYS_PER_MONTH);
    }

    @Test
    void advanceDoesNothingBeforeExpiry() {
        FixedDurationIntel intel = new FixedDurationIntel();
        when(clock.getElapsedDaysSince(CREATED_AT))
                .thenReturn((float) StarsectorClock.DAYS_PER_MONTH - 1f);

        intel.advanceImpl(1f);

        verify(intelManager, never()).removeIntel(intel);
    }

    @Test
    void advanceRemovesIntelOnceExpiryReached() {
        FixedDurationIntel intel = new FixedDurationIntel();
        when(clock.getElapsedDaysSince(CREATED_AT))
                .thenReturn((float) StarsectorClock.DAYS_PER_MONTH);

        intel.advanceImpl(1f);

        verify(intelManager).removeIntel(intel);
    }

    @Test
    void subclassExpiryOverrideIsHonoured() {
        CustomDurationIntel intel = new CustomDurationIntel(7f);
        when(clock.getElapsedDaysSince(CREATED_AT)).thenReturn(7f);

        intel.advanceImpl(1f);

        verify(intelManager).removeIntel(intel);
    }

    @Test
    void advanceIsNoOpWhenSectorDisappears() {
        FixedDurationIntel intel = new FixedDurationIntel();
        // Simulate teardown: sector lookups return null mid-game.
        globalStatic.when(Global::getSector).thenReturn(null);

        // Should not throw; should not interact with the (now-unreachable) intel manager.
        intel.advanceImpl(1f);

        verify(intelManager, never()).removeIntel(intel);
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
