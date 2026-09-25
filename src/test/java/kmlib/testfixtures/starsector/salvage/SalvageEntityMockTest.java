package kmlib.testfixtures.starsector.salvage;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.impl.campaign.procgen.SalvageEntityGenDataSpec.DropData;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.SalvageEntity;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Pins what a case relies on the stand-in for: that a roll never reaches vanilla, that each capture
 * reads the roll it names, and that a capture fails when the roll count is not the one the case
 * expected.
 */
final class SalvageEntityMockTest {

    private static final float FIRST_VALUE_MULT = 1f;
    private static final float FIRST_RANDOM_MULT = 2f;
    private static final float FIRST_OVERALL_MULT = 3f;
    private static final float FIRST_FUEL_MULT = 4f;
    private static final float SECOND_VALUE_MULT = 5f;

    private SalvageEntityMock salvageMock;

    @BeforeEach
    void installRoller() {

        salvageMock = SalvageEntityMock.install();
    }

    @AfterEach
    void closeRoller() {

        salvageMock.close();
    }

    private static CargoAPI rollWith(Random random, float valueMult, List<DropData> valueDrops) {

        return SalvageEntity.generateSalvage(
            random, valueMult, FIRST_RANDOM_MULT, FIRST_OVERALL_MULT, FIRST_FUEL_MULT, valueDrops, List.of());
    }

    @Nested
    class Install {

        @Test
        void answersARollWithACargo() {

            assertThat(rollWith(new Random(), FIRST_VALUE_MULT, List.of()))
                .isNotNull();
        }
    }

    @Nested
    class StubRoll {

        @Test
        void answersEveryRollWithTheGivenCargo() {

            var cargoMock = mock(CargoAPI.class);

            salvageMock.stubRoll(cargoMock);

            assertThat(rollWith(new Random(), FIRST_VALUE_MULT, List.of()))
                .isSameAs(cargoMock);
        }
    }

    @Nested
    class CaptureScalars {

        @Test
        void readsTheFourMultipliersInTheRollersOrder() {

            rollWith(new Random(), FIRST_VALUE_MULT, List.of());

            assertThat(salvageMock.captureScalars())
                .isEqualTo(new SalvageEntityMock.Scalars(
                    FIRST_VALUE_MULT, FIRST_RANDOM_MULT, FIRST_OVERALL_MULT, FIRST_FUEL_MULT));
        }

        @Test
        void readsTheRollAtTheGivenIndex() {

            rollWith(new Random(), FIRST_VALUE_MULT, List.of());
            rollWith(new Random(), SECOND_VALUE_MULT, List.of());

            assertThat(salvageMock.captureScalars(2, 1).valueMult())
                .isEqualTo(SECOND_VALUE_MULT);
        }

        @Test
        void failsWhenTheCodeRolledMoreOftenThanExpected() {

            rollWith(new Random(), FIRST_VALUE_MULT, List.of());
            rollWith(new Random(), SECOND_VALUE_MULT, List.of());

            assertThatThrownBy(() -> salvageMock.captureScalars())
                .isInstanceOf(AssertionError.class);
        }
    }

    @Nested
    class CaptureDropLists {

        @Test
        void readsTheListsTheRollWasGiven() {

            var valueDrops = List.of(new DropData());

            rollWith(new Random(), FIRST_VALUE_MULT, valueDrops);

            assertThat(salvageMock.captureDropLists())
                .isEqualTo(new SalvageEntityMock.DropLists(valueDrops, List.of()));
        }
    }

    @Nested
    class CaptureRandoms {

        @Test
        void readsEveryRandomSourceInCallOrder() {

            var firstRandom = new Random();
            var secondRandom = new Random();

            rollWith(firstRandom, FIRST_VALUE_MULT, List.of());
            rollWith(secondRandom, FIRST_VALUE_MULT, List.of());

            assertThat(salvageMock.captureRandoms(2))
                .containsExactly(firstRandom, secondRandom);
        }
    }

    @Nested
    class VerifyNoRoll {

        @Test
        void passesWhenNothingRolled() {

            assertThatCode(() -> salvageMock.verifyNoRoll())
                .doesNotThrowAnyException();
        }

        @Test
        void failsWhenTheRollerWasReached() {

            rollWith(new Random(), FIRST_VALUE_MULT, List.of());

            assertThatThrownBy(() -> salvageMock.verifyNoRoll())
                .isInstanceOf(AssertionError.class);
        }
    }
}
