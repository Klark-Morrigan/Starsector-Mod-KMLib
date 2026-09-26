package kmlib.testfixtures.starsector.intel;

import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins the intel manager a recording subject is exercised against: an add is held and read back, the game's two
 * refusals hold - an intel added twice is held once, an ended one not at all - and a read by class answers what is
 * assignable to it.
 */
final class IntelManagerFakeTest {

    @Nested
    class AddIntel {

        @Test
        void holdsEachAddInOrderWhicheverOverloadCarriedIt() {

            var managerFake = new IntelManagerFake();
            var firstMock = mock(IntelInfoPlugin.class);
            var secondMock = mock(IntelInfoPlugin.class);
            var thirdMock = mock(IntelInfoPlugin.class);

            managerFake.getIntelManager().addIntel(firstMock);
            managerFake.getIntelManager().addIntel(secondMock, true);
            managerFake.getIntelManager().addIntel(thirdMock, false, null);

            assertThat(managerFake.listHeldIntel())
                .containsExactly(firstMock, secondMock, thirdMock);
        }

        @Test
        void holdsAnIntelAddedTwiceOnce() {

            var managerFake = new IntelManagerFake();
            var intelMock = mock(IntelInfoPlugin.class);

            managerFake.getIntelManager().addIntel(intelMock);
            managerFake.getIntelManager().addIntel(intelMock);

            assertThat(managerFake.listHeldIntel())
                .containsExactly(intelMock);
        }

        @Test
        void refusesAnIntelAlreadyEnded() {

            var managerFake = new IntelManagerFake();
            var intelMock = mock(IntelInfoPlugin.class);

            when(intelMock.isEnded())
                .thenReturn(true);

            managerFake.getIntelManager().addIntel(intelMock);

            assertThat(managerFake.listHeldIntel())
                .isEmpty();
        }
    }

    @Nested
    class GetFirstIntel {

        @Test
        void answersNullWhereNothingOfTheClassIsHeld() {

            var managerFake = new IntelManagerFake();

            managerFake.storeIntel(mock(IntelInfoPlugin.class));

            assertThat(managerFake.getIntelManager().getFirstIntel(EventIntel.class))
                .isNull();
        }

        @Test
        void answersTheEarliestHeldIntelOfTheClass() {

            var managerFake = new IntelManagerFake();
            var firstMock = mock(EventIntel.class);

            managerFake.storeIntel(mock(IntelInfoPlugin.class));
            managerFake.storeIntel(firstMock);
            managerFake.storeIntel(mock(EventIntel.class));

            assertThat(managerFake.getIntelManager().getFirstIntel(EventIntel.class))
                .isSameAs(firstMock);
        }
    }

    @Nested
    class GetIntel {

        @Test
        void answersOnlyWhatIsAssignableToTheClassAsked() {

            var managerFake = new IntelManagerFake();
            var otherMock = mock(IntelInfoPlugin.class);
            var eventMock = mock(EventIntel.class);

            managerFake.storeIntel(otherMock);
            managerFake.storeIntel(eventMock);

            assertThat(managerFake.getIntelManager().getIntel(EventIntel.class))
                .containsExactly(eventMock);
            assertThat(managerFake.getIntelManager().getIntel())
                .containsExactly(otherMock, eventMock);
        }
    }

    @Nested
    class HasIntelOfClass {

        @Test
        void answersWhetherAnythingOfTheClassIsHeld() {

            var managerFake = new IntelManagerFake();

            managerFake.storeIntel(mock(IntelInfoPlugin.class));

            assertThat(managerFake.getIntelManager().hasIntelOfClass(IntelInfoPlugin.class))
                .isTrue();
            assertThat(managerFake.getIntelManager().hasIntelOfClass(EventIntel.class))
                .isFalse();
        }
    }

    @Nested
    class RemoveIntel {

        @Test
        void dropsTheIntelFromWhatIsHeld() {

            var managerFake = new IntelManagerFake();
            var intelMock = mock(IntelInfoPlugin.class);

            managerFake.storeIntel(intelMock);
            managerFake.getIntelManager().removeIntel(intelMock);

            assertThat(managerFake.listHeldIntel())
                .isEmpty();
            assertThat(managerFake.getIntelManager().hasIntel(intelMock))
                .isFalse();
        }
    }

    @Nested
    class StoreIntel {

        @Test
        void seedsAnIntelWhateverItsState() {

            // A save can hold an intel that has since ended; seeding stands for that, so it skips the add's refusal.
            var managerFake = new IntelManagerFake();
            var intelMock = mock(IntelInfoPlugin.class);

            when(intelMock.isEnded())
                .thenReturn(true);

            managerFake.storeIntel(intelMock);

            assertThat(managerFake.listHeldIntelOf(IntelInfoPlugin.class))
                .containsExactly(intelMock);
        }
    }

    // A narrower intel type, so a read by class has something held to leave out.
    private interface EventIntel extends IntelInfoPlugin {
    }
}
