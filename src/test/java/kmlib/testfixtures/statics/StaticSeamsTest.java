package kmlib.testfixtures.statics;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Pins what an arrangement relies on the registry for: every seam it holds is closed, innermost
 * first, exactly once - whether it opened the seam or was handed one opened elsewhere.
 */
final class StaticSeamsTest {

    private final StaticSeams seams = new StaticSeams();

    @Nested
    class CloseEverySeam {

        @Test
        void closesTheSeamsInReverseOfTheOrderTheyWereHeld() {

            MockedStatic<?> outerSeamMock = mock(MockedStatic.class);
            MockedStatic<?> innerSeamMock = mock(MockedStatic.class);

            seams.holdSeam(outerSeamMock);
            seams.holdSeam(innerSeamMock);
            seams.closeEverySeam();

            var closeOrder = inOrder(innerSeamMock, outerSeamMock);

            closeOrder.verify(innerSeamMock).close();
            closeOrder.verify(outerSeamMock).close();
        }

        @Test
        void closesNothingTwiceWhenRunAgain() {

            MockedStatic<?> seamMock = mock(MockedStatic.class);

            seams.holdSeam(seamMock);
            seams.closeEverySeam();
            seams.closeEverySeam();

            verify(seamMock, times(1))
                .close();
        }
    }

    @Nested
    class HoldSeam {

        @Test
        void handsBackTheSeamItWasHanded() {

            MockedStatic<?> seamMock = mock(MockedStatic.class);

            assertThat(seams.holdSeam(seamMock))
                .isSameAs(seamMock);
        }
    }

    @Nested
    class OpenSeam {

        @Test
        void standsInForTheClassUntilTheSeamsClose() {

            seams
                .openSeam(StoodInClass.class)
                .when(StoodInClass::readAnswer)
                .thenReturn("stood in");

            var answerWhileOpen = StoodInClass.readAnswer();

            seams.closeEverySeam();

            assertThat(answerWhileOpen)
                .isEqualTo("stood in");
            assertThat(StoodInClass.readAnswer())
                .isEqualTo("real");
        }
    }

    // A class with one static read, so a seam over it can be seen answering and then put back.
    private static final class StoodInClass {

        static String readAnswer() {
            return "real";
        }
    }
}
