package kmlib.starsector.salvage;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.impl.campaign.procgen.SalvageEntityGenDataSpec.DropData;

import kmlib.testfixtures.starsector.salvage.SalvageEntityMock;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

final class SalvageDropsTests {

    @Nested
    class BuildRandomDrop {

        @Test
        void carriesTheGroupAndWeightAtFullValue() {

            var drop = SalvageDrops.buildRandomDrop("weapons2", 5);

            assertThat(drop.group)
                .isEqualTo("weapons2");
            assertThat(drop.chances)
                .isEqualTo(5);
            assertThat(drop.value)
                .isEqualTo(0);
            assertThat(drop.valueMult)
                .isEqualTo(1f);
        }

        @Test
        void carriesTheValueMultiplierItIsGiven() {

            var drop = SalvageDrops.buildRandomDrop("rare_tech_low", 1, 0.1f);

            assertThat(drop.chances)
                .isEqualTo(1);
            assertThat(drop.valueMult)
                .isEqualTo(0.1f);
        }
    }

    @Nested
    class BuildValueDrop {

        @Test
        void carriesTheGroupAndBudgetWithNoWeight() {

            var drop = SalvageDrops.buildValueDrop("basic", 10_000);

            assertThat(drop.group)
                .isEqualTo("basic");
            assertThat(drop.value)
                .isEqualTo(10_000);
            assertThat(drop.chances)
                .isEqualTo(0);
            assertThat(drop.valueMult)
                .isEqualTo(1f);
        }
    }

    @Nested
    class RollDrops {

        private SalvageEntityMock salvageMock;

        @BeforeEach
        void setUp() {

            salvageMock = SalvageEntityMock.install();
        }

        @AfterEach
        void tearDown() {

            salvageMock.close();
        }

        @Test
        void handsTheRollerTheOverallMultiplierWithTheOtherScalarsNeutral() {

            SalvageDrops.rollDrops(new Random(), 0.7f, List.of(), List.of());

            var scalars = salvageMock.captureScalars();

            assertThat(scalars.valueMult())
                .isEqualTo(1f);
            assertThat(scalars.randomMult())
                .isEqualTo(1f);
            assertThat(scalars.overallMult())
                .isEqualTo(0.7f);
            assertThat(scalars.fuelMult())
                .isEqualTo(1f);
        }

        @Test
        void handsTheRollerTheListsAndRandomItIsGiven() {

            var random = new Random();
            var valueDrops = List.of(new DropData());
            var randomDrops = List.of(new DropData());

            SalvageDrops.rollDrops(random, 1f, valueDrops, randomDrops);

            var drops = salvageMock.captureDropLists();

            assertThat(drops.valueDrops())
                .isSameAs(valueDrops);
            assertThat(drops.randomDrops())
                .isSameAs(randomDrops);
            assertThat(salvageMock.captureRandoms())
                .containsExactly(random);
        }

        @Test
        void returnsTheRolledCargo() {

            var cargoMock = mock(CargoAPI.class);

            salvageMock.stubRoll(cargoMock);

            assertThat(SalvageDrops.rollDrops(new Random(), 1f, List.of(), List.of()))
                .isSameAs(cargoMock);
        }
    }
}
