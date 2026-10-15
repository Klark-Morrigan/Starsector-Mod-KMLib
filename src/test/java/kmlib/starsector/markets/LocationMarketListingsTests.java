package kmlib.starsector.markets;

import com.fs.starfarer.api.campaign.econ.MarketAPI;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Pins the fixed-set guarantee a {@link LocationMarketListings} makes, and the order its two halves
 * are joined in. Each method's cases live in a {@link Nested} group so the suite reports as a
 * per-method tree.
 *
 * <p>Which markets land in which half is the reader's business and is pinned by
 * {@link LocationMarketsTests}.
 */
final class LocationMarketListingsTests {

    @Nested
    class Construct {

        @Test
        void readsAbsentListsAsEmpty() {
            assertThat(new LocationMarketListings(null, null))
                .isEqualTo(LocationMarketListings.NONE);
        }

        @Test
        void keepsTheMarketsItWasBuiltWithWhenTheSourceListChangesLater() {
            // The economy's list is the game's own and goes on changing; a listing handed to several
            // readers in one pass has to say the same thing to each of them.
            var listed = new ArrayList<MarketAPI>();
            listed.add(mock(MarketAPI.class));

            var listings = new LocationMarketListings(listed, List.of());

            listed.clear();

            assertThat(listings.listedByEconomy())
                .hasSize(1);
        }

        @Test
        void rejectsAnAttemptToChangeAListing() {

            var listings = new LocationMarketListings(List.of(mock(MarketAPI.class)), List.of());

            assertThatThrownBy(() -> listings.listedByEconomy().clear())
                .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    class ListPresentMarkets {

        @Test
        void yieldsTheEconomysMarketsAheadOfTheUnlistedOnes() {
            // Economy order is load-bearing for a caller mirroring vanilla's tie rule, so the listed
            // half comes first and in its own order, with the unlisted half after it.
            var ancyraMock = mock(MarketAPI.class);
            var tibicenaMock = mock(MarketAPI.class);
            var academyMock = mock(MarketAPI.class);

            var listings = new LocationMarketListings(
                List.of(ancyraMock, tibicenaMock),
                List.of(academyMock));

            assertThat(listings.listPresentMarkets())
                .containsExactly(ancyraMock, tibicenaMock, academyMock);
        }

        @Test
        void yieldsNothingForAPlaceHoldingNoMarket() {
            assertThat(LocationMarketListings.NONE.listPresentMarkets())
                .isEmpty();
        }
    }
}
