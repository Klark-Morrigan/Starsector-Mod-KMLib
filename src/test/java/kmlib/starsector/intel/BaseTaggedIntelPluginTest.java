package kmlib.starsector.intel;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignClockAPI;
import com.fs.starfarer.api.campaign.SectorAPI;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Pins the tag-merge contract on {@link BaseTaggedIntelPlugin}: the
 * vanilla tag derivation flows through unchanged, the
 * constructor-supplied tags get mixed in on top, and the zero-tag
 * case is a pure pass-through so the {@link BaseExpiringIntelPlugin}
 * untagged path stays viable.
 *
 * <p>Calls {@code getIntelTags(null)} throughout - that skips
 * {@code BaseIntelPlugin}'s "Local" branch, which otherwise needs
 * {@code Global.getSettings} and a player fleet mocked. The
 * {@code BaseExpiringIntelPlugin} suite already mocks the sector
 * for the expiring chain; this file isolates the tag mix-in.</p>
 */
class BaseTaggedIntelPluginTest {

    private MockedStatic<Global> globalMock;

    @BeforeEach
    void setUp() {
        // BaseIntelPlugin.getIntelTags reads Global.getSector().getClock()
        // for the "New" derivation. Stub a zero-timestamp clock so the
        // tag-merge path under test can call through super without NPEing.
        var clockMock = mock(CampaignClockAPI.class);
        when(clockMock.getTimestamp()).thenReturn(0L);
        var sectorMock = mock(SectorAPI.class);
        when(sectorMock.getClock()).thenReturn(clockMock);

        globalMock = mockStatic(Global.class);
        globalMock.when(Global::getSector).thenReturn(sectorMock);
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
    }

    @Nested
    class GetIntelTags {
        @Test
        void zeroTagsLeavesVanillaTagSetUntouched() {
            var intel = new UntaggedIntel();

            var tags = intel.getIntelTags(null);

            assertThat(tags).doesNotContain(TAG_A, TAG_B);
        }

        @Test
        void singleConstructorTagAppearsInResult() {
            var intel = new SingleTagIntel();

            assertThat(intel.getIntelTags(null)).contains(TAG_A);
        }

        @Test
        void multipleConstructorTagsAllAppearInResult() {
            var intel = new MultiTagIntel();

            assertThat(intel.getIntelTags(null)).contains(TAG_A, TAG_B);
        }

        @Test
        void importantFlagStillPropagatesProvingSuperIsCalled() {
            // Regression guard: if the override stopped calling super,
            // vanilla's "Important" tag would silently disappear. Set the
            // flag and assert the tag still surfaces alongside the
            // constructor-supplied tag.
            var intel = new SingleTagIntel();
            intel.setImportant(true);

            assertThat(intel.getIntelTags(null)).contains("Important", TAG_A);
        }
    }

    private static final String TAG_A = "test_tag_a";
    private static final String TAG_B = "test_tag_b";

    private static final class UntaggedIntel extends BaseTaggedIntelPlugin {
    }

    private static final class SingleTagIntel extends BaseTaggedIntelPlugin {
        SingleTagIntel() {
            super(TAG_A);
        }
    }

    private static final class MultiTagIntel extends BaseTaggedIntelPlugin {
        MultiTagIntel() {
            super(TAG_A, TAG_B);
        }
    }
}
