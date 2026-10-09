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
}
