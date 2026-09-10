package kmlib.logging;

import org.apache.log4j.Logger;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Pins "once, and loudly" - the whole of what this is for. Every probe behind it is called from a
 * render pass and fails open, so a warning that repeated would write itself sixty times a second,
 * and one that never fired would leave a broken read silently switching off what is built on it.
 */
class SessionWarningTest {

    private static final String FIRST_MESSAGE = "the widget tree could not be walked";
    private static final String SECOND_MESSAGE = "the surface could not be identified";

    @Nested
    class WarnOnce {

        @Test
        void warnOnceWritesTheFirstMessageAtWarn() {

            var loggerMock = mock(Logger.class);

            new SessionWarning(loggerMock).warnOnce(FIRST_MESSAGE);

            verify(loggerMock)
                .warn(FIRST_MESSAGE);
        }

        @Test
        void warnOnceWritesTheFailureAlongsideTheMessageWhenOneIsGiven() {

            var loggerMock = mock(Logger.class);
            var failure = new IllegalStateException("no such method");

            new SessionWarning(loggerMock).warnOnce(FIRST_MESSAGE, failure);

            verify(loggerMock)
                .warn(FIRST_MESSAGE, failure);
        }

        @Test
        void warnOnceSaysNothingMoreAfterItHasSaidSomething() {
            // The second message is a different one on purpose: the first warning silences the
            // session, not just repeats of itself, because a probe with two ways to fail has
            // nothing new to add once it has reported that it cannot answer.
            var loggerMock = mock(Logger.class);
            var warning = new SessionWarning(loggerMock);

            warning.warnOnce(FIRST_MESSAGE);
            warning.warnOnce(SECOND_MESSAGE);

            verify(loggerMock, times(1))
                .warn(FIRST_MESSAGE);
            verify(loggerMock, never())
                .warn(SECOND_MESSAGE);
        }
    }

    @Nested
    class HasWarnedThisSession {

        @Test
        void hasWarnedThisSessionIsFalseUntilSomethingIsSaid() {

            var loggerMock = mock(Logger.class);

            assertThat(new SessionWarning(loggerMock).hasWarnedThisSession())
                .isFalse();

            verifyNoInteractions(loggerMock);
        }

        @Test
        void hasWarnedThisSessionIsTrueOnceSomethingHasBeenSaid() {

            var warning = new SessionWarning(mock(Logger.class));
            warning.warnOnce(FIRST_MESSAGE);

            assertThat(warning.hasWarnedThisSession())
                .isTrue();
        }
    }

    @Nested
    class RearmWarning {

        @Test
        void rearmWarningLetsASpentWarningBeSaidOnceMore() {
            // The case it exists for: a reach broke and said so while nobody was listening, and the
            // reader who then asks to be told would otherwise be met with silence.
            var loggerMock = mock(Logger.class);
            var warning = new SessionWarning(loggerMock);

            warning.warnOnce(FIRST_MESSAGE);
            warning.rearmWarning();
            warning.warnOnce(FIRST_MESSAGE);

            verify(loggerMock, times(2))
                .warn(FIRST_MESSAGE);
        }

        @Test
        void rearmWarningRestoresTheOnceRatherThanLiftingIt() {
            // Re-armed is not un-silenced. The reaches behind these fail identically every frame, so
            // anything that left the warning open would be writing it sixty times a second.
            var loggerMock = mock(Logger.class);
            var warning = new SessionWarning(loggerMock);

            warning.warnOnce(FIRST_MESSAGE);
            warning.rearmWarning();
            warning.warnOnce(FIRST_MESSAGE);
            warning.warnOnce(FIRST_MESSAGE);

            verify(loggerMock, times(2))
                .warn(FIRST_MESSAGE);
        }

        @Test
        void rearmWarningSaysNothingByItself() {
            // It changes what may be said, not what is: a switch flipped on a session where nothing
            // ever broke must not manufacture a warning.
            var loggerMock = mock(Logger.class);

            new SessionWarning(loggerMock).rearmWarning();

            verifyNoInteractions(loggerMock);
        }

        @Test
        void rearmWarningLeavesAnUnspentWarningUnspent() {

            var warning = new SessionWarning(mock(Logger.class));

            warning.rearmWarning();

            assertThat(warning.hasWarnedThisSession())
                .isFalse();
        }
    }
}
