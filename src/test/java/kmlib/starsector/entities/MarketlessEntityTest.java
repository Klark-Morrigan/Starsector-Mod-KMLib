package kmlib.starsector.entities;

import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins the contracts of {@link MarketlessEntity#readOwnerId}, {@link MarketlessEntity#readTypeId}
 * and {@link MarketlessEntity#isDiscoveredByPlayer}, plus what the record refuses to be built on -
 * the facts one answers about itself off its own entity rather than storing beside it. Each
 * method's cases live in a {@link Nested} group so the suite reports as a per-method tree; the
 * shared builders stay on the outer class.
 */
final class MarketlessEntityTest {

    @Nested
    class Construct {

        @Test
        void refusesAnEntityCarryingAMarket() {
            // Such an entity is a colony's, and its owner and discovery are the market's. Refusing
            // it here fails where the mistake is, rather than three reads downstream each free to
            // answer differently from the colony holding the same entity.
            var entityMock = buildCustomEntity("station_side03");

            when(entityMock.getMarket())
                .thenReturn(mock(MarketAPI.class));

            assertThatThrownBy(() -> new MarketlessEntity(entityMock))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("station_side03");
        }

        @Test
        void refusesAReadingWithNoEntity() {

            assertThatThrownBy(() -> new MarketlessEntity(null))
                .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class ReadOwnerId {

        @Test
        void reportsTheFactionIdTheEntityCarries() {
            // Live and unconcealed. A reading that substituted a neutral owner for a distant
            // player would leave a caller unable to tell a genuinely unowned derelict from a
            // withheld one.
            var factionMock = mock(FactionAPI.class);

            when(factionMock.getId())
                .thenReturn("remnant");

            var entityMock = buildCustomEntity("station_research");

            when(entityMock.getFaction())
                .thenReturn(factionMock);

            assertThat(new MarketlessEntity(entityMock).readOwnerId())
                .isEqualTo("remnant");
        }

        @Test
        void reportsNoOwnerWhereTheEntityNamesNoFaction() {
            // Absorbed rather than refused: an owner nobody can name is compared against whatever
            // a caller compares owners for, and there is nothing here to fail on.
            var marketlessEntity = new MarketlessEntity(buildCustomEntity("station_research"));

            assertThat(marketlessEntity.readOwnerId())
                .isNull();
        }
    }

    @Nested
    class ReadTypeId {

        @Test
        void reportsTheTypeTheEntityWasBuiltFrom() {
            // The ID rather than the spec's name, that being what a classification keyed on entity
            // type matches against.
            assertThat(new MarketlessEntity(buildCustomEntity("station_mining")).readTypeId())
                .isEqualTo("station_mining");
        }

        @Test
        void reportsNoTypeWhereTheEntityCarriesNone() {

            assertThat(new MarketlessEntity(buildCustomEntity(null)).readTypeId())
                .isNull();
        }
    }

    @Nested
    class IsDiscoveredByPlayer {

        @Test
        void reportsAFoundEntityAsDiscovered() {
            // An entity stops being discoverable once found, so the inclusion gate is the
            // negation of that flag rather than a reading of its own.
            var marketlessEntity = new MarketlessEntity(buildCustomEntity("station_research"));

            assertThat(marketlessEntity.isDiscoveredByPlayer())
                .isTrue();
        }

        @Test
        void reportsAnUnfoundEntityAsUndiscovered() {

            var entityMock = buildCustomEntity("station_research");

            when(entityMock.isDiscoverable())
                .thenReturn(true);

            assertThat(new MarketlessEntity(entityMock).isDiscoveredByPlayer())
                .isFalse();
        }
    }

    // A found, market-less entity owned by nobody - the state every case that is about one axis
    // poses its entity in, then stubs the one thing it is about on top.
    private static CustomCampaignEntityAPI buildCustomEntity(String entityType) {

        var entityMock = mock(CustomCampaignEntityAPI.class);

        when(entityMock.getCustomEntityType())
            .thenReturn(entityType);

        return entityMock;
    }
}
