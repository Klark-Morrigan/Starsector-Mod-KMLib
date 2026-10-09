package kmlib.starsector.commodities;

import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class CommodityBasePricesTests {

    private SettingsAPI settingsMock;

    @BeforeEach
    void setUp() {

        settingsMock = mock(SettingsAPI.class);
    }

    private void addSpec(String commodityId, float basePrice, String demandClass) {

        var specMock = mock(CommoditySpecAPI.class);

        when(specMock.getBasePrice())
            .thenReturn(basePrice);
        when(specMock.getDemandClass())
            .thenReturn(demandClass);
        when(settingsMock.getCommoditySpec(commodityId))
            .thenReturn(specMock);
    }

    @Nested
    class FindBasePrice {

        @Test
        void returnsTheCommoditysOwnPrice() {

            addSpec("ore", 10f, "ore");

            assertThat(CommodityBasePrices.findBasePrice(settingsMock, "ore"))
                .isEqualTo(10f);
        }

        @Test
        void returnsTheDemandClassPeersPriceWhenTheCommodityHasNone() {

            addSpec("lobster", 0f, "luxury_goods");
            addSpec("luxury_goods", 100f, "luxury_goods");

            assertThat(CommodityBasePrices.findBasePrice(settingsMock, "lobster"))
                .isEqualTo(100f);
        }

        @Test
        void returnsNoPriceWhenThePeerHasNoneEither() {

            addSpec("exotic", 0f, "exotic_class");
            addSpec("exotic_class", 0f, "exotic_class");

            assertThat(CommodityBasePrices.findBasePrice(settingsMock, "exotic"))
                .isEqualTo(0f);
        }

        @Test
        void returnsNoPriceWhenThePeerIsUnknown() {

            addSpec("exotic", 0f, "exotic_class");

            assertThat(CommodityBasePrices.findBasePrice(settingsMock, "exotic"))
                .isEqualTo(0f);
        }

        // A commodity naming itself as its class has no peer to fall back on.
        @Test
        void returnsNoPriceWhenTheCommodityIsItsOwnDemandClass() {

            addSpec("ore", 0f, "ore");

            assertThat(CommodityBasePrices.findBasePrice(settingsMock, "ore"))
                .isEqualTo(0f);
        }

        @Test
        void returnsNoPriceWhenTheCommodityHasNoDemandClass() {

            addSpec("ore", 0f, "");

            assertThat(CommodityBasePrices.findBasePrice(settingsMock, "ore"))
                .isEqualTo(0f);
        }

        @Test
        void returnsNoPriceForAnUnknownCommodity() {

            assertThat(CommodityBasePrices.findBasePrice(settingsMock, "phantom"))
                .isEqualTo(0f);
        }
    }
}
