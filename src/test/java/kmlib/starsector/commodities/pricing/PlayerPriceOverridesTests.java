package kmlib.starsector.commodities.pricing;

import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.StatBonus;

import kmlib.starsector.commodities.PlayerTradeSide;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins the override against vanilla's own modifier arithmetic: the market quotes its raw price through a real
 * {@link StatBonus}, as the game does, so each case reads the price the player would see.
 */
final class PlayerPriceOverridesTests {

    private static final String COMMODITY_ID = "supplies";

    // What the market would quote before any player modifier: an out-of-economy market's near-floor price.
    private static final float RAW_UNIT_PRICE = 2f;

    private static final String SOURCE_KEY = "kmlib_test";

    private MarketAPI marketMock;
    private StatBonus supplyPriceMod;

    @BeforeEach
    void setUp() {

        supplyPriceMod = new StatBonus();

        var commodityMock = mock(CommodityOnMarketAPI.class);

        when(commodityMock.getPlayerSupplyPriceMod())
            .thenReturn(supplyPriceMod);

        marketMock = mock(MarketAPI.class);

        when(marketMock.getCommodityData(COMMODITY_ID))
            .thenReturn(commodityMock);
        when(marketMock.getSupplyPrice(anyString(), anyDouble(), eq(true)))
            .thenAnswer(invocation -> supplyPriceMod.computeEffective(RAW_UNIT_PRICE));
    }

    private float readQuotedUnitPrice() {

        return PlayerTradeSide.BUYING.readUnitPrice(marketMock, COMMODITY_ID);
    }

    @Nested
    class OverrideUnitPrice {

        @Test
        void quotesTheTargetPrice() {

            PlayerPriceOverrides.overrideUnitPrice(marketMock, COMMODITY_ID, PlayerTradeSide.BUYING, SOURCE_KEY, 100f);

            assertThat(readQuotedUnitPrice())
                .isCloseTo(100f, within(0.001f));
        }

        @Test
        void answersThePriceTheMarketQuotedOnItsOwn() {

            assertThat(PlayerPriceOverrides.overrideUnitPrice(
                    marketMock,
                    COMMODITY_ID,
                    PlayerTradeSide.BUYING,
                    SOURCE_KEY,
                    100f))
                .contains(2f);
        }

        @Test
        void replacesAnEarlierOverrideRatherThanStackingOnIt() {

            PlayerPriceOverrides.overrideUnitPrice(marketMock, COMMODITY_ID, PlayerTradeSide.BUYING, SOURCE_KEY, 100f);
            PlayerPriceOverrides.overrideUnitPrice(marketMock, COMMODITY_ID, PlayerTradeSide.BUYING, SOURCE_KEY, 60f);

            assertThat(readQuotedUnitPrice())
                .isCloseTo(60f, within(0.001f));
        }

        // Another mod's x2 stays applied, and the override still lands on the target through it.
        @Test
        void quotesTheTargetThroughAnotherModsMultiplier() {

            supplyPriceMod.modifyMult("other_mod", 2f);

            PlayerPriceOverrides.overrideUnitPrice(marketMock, COMMODITY_ID, PlayerTradeSide.BUYING, SOURCE_KEY, 100f);

            assertThat(readQuotedUnitPrice())
                .isCloseTo(100f, within(0.001f));
        }

        @Test
        void leavesAMarketThatMultipliesThePriceToNothingAlone() {

            supplyPriceMod.modifyMult("other_mod", 0f);

            assertThat(PlayerPriceOverrides.overrideUnitPrice(
                    marketMock,
                    COMMODITY_ID,
                    PlayerTradeSide.BUYING,
                    SOURCE_KEY,
                    100f))
                .isEmpty();
            assertThat(supplyPriceMod.getFlatBonus())
                .isEqualTo(0f);
        }

        @Test
        void leavesAMarketWithoutTheCommodityAlone() {

            assertThat(PlayerPriceOverrides.overrideUnitPrice(
                    marketMock,
                    "lobster",
                    PlayerTradeSide.BUYING,
                    SOURCE_KEY,
                    100f))
                .isEmpty();
        }

        @Test
        void leavesANullMarketAlone() {

            assertThat(PlayerPriceOverrides.overrideUnitPrice(
                    null,
                    COMMODITY_ID,
                    PlayerTradeSide.BUYING,
                    SOURCE_KEY,
                    100f))
                .isEmpty();
        }
    }

    @Nested
    class ClearUnitPriceOverride {

        @Test
        void restoresTheMarketsOwnPrice() {

            PlayerPriceOverrides.overrideUnitPrice(marketMock, COMMODITY_ID, PlayerTradeSide.BUYING, SOURCE_KEY, 100f);

            PlayerPriceOverrides.clearUnitPriceOverride(marketMock, COMMODITY_ID, PlayerTradeSide.BUYING, SOURCE_KEY);

            assertThat(readQuotedUnitPrice())
                .isEqualTo(2f);
        }

        @Test
        void leavesAnotherSourcesEntryStanding() {

            supplyPriceMod.modifyFlat("other_mod", 5f);

            PlayerPriceOverrides.clearUnitPriceOverride(marketMock, COMMODITY_ID, PlayerTradeSide.BUYING, SOURCE_KEY);

            assertThat(readQuotedUnitPrice())
                .isEqualTo(7f);
        }
    }
}
