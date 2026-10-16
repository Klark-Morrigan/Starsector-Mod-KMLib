package kmlib.starsector.ui.map.probes;

import org.apache.log4j.Logger;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Pins the three things this owes the log: it speaks when a pass drew with no map on screen, it stays
 * quiet when one is showing, and it never lets a failed read reach the pass it was only reporting on.
 *
 * <p>Asserted through the logger rather than through a flag, because the line is the whole product: a
 * warning that is recorded as having been said but never written is exactly the failure that would make
 * this useless in the one situation it exists for. The throwable it carries is asserted as present for
 * the same reason - the stack is the only part of the line that names the caller, and the message alone
 * would leave a reader no better off than the symptom did.
 *
 * <p>The presence read is a plain supplier here, which is what makes any of this answerable: a live one
 * walks the campaign UI's widget tree and cannot be stood up outside a running game.
 */
final class ForeignMapPassWarningTests {

    // What the tree walk answers when it found no embedded map, which is what most of these cases
    // want: they are about when the line is written, not what it carries.
    private static final String NO_HOSTS_FOUND = "mapHosts=[]";

    // A walk that found one, naming a class no engine package could account for - the finding the
    // whole line exists to deliver.
    private static final String MOD_OWNED_HOST_FOUND =
        "mapHosts=[[map=com.fs.starfarer.coreui.A$G host=com.fs.starfarer.ui.impl.StandardTooltipV2"
            + " modOwned=[org.example.mod.NavTooltipPlugin]]]";

    // The caller's own words for the two lines.
    private static final String NO_MAP_LEAD = "Overlay drawn with no map on screen";
    private static final String UNREADABLE_PRESENCE = "Overlay could not read whether a map is on screen.";

    @Nested
    class WarnOnceIfNoVanillaMapIsShowing {

        @Test
        void reportsThePassWithItsStackWhenNoMapIsOnScreen() {

            var loggerMock = mock(Logger.class);

            buildWarning(() -> false, () -> NO_HOSTS_FOUND, loggerMock)
                .warnOnceIfNoVanillaMapIsShowing();

            // The throwable is the caller's stack, which is the only thing in the line that answers
            // the question the line is raised to ask.
            verify(loggerMock)
                .warn(any(), any(Throwable.class));
        }

        @Test
        void leadsWithTheCallersWordsAndFollowsWithTheHostTrace() {

            var loggerMock = mock(Logger.class);

            buildWarning(() -> false, () -> NO_HOSTS_FOUND, loggerMock)
                .warnOnceIfNoVanillaMapIsShowing();

            verify(loggerMock)
                .warn(eq("Overlay drawn with no map on screen; mapHosts=[]"), any(Throwable.class));
        }

        @Test
        void staysQuietWhileAMapIsShowing() {
            // The ordinary case, and by far the common one: this runs on every frame the sector map
            // paints, so a line here would be written sixty times a second on a working setup.
            var loggerMock = mock(Logger.class);

            buildWarning(() -> true, () -> NO_HOSTS_FOUND, loggerMock)
                .warnOnceIfNoVanillaMapIsShowing();

            verifyNoInteractions(loggerMock);
        }

        @Test
        void reportsOneForeignPassOnlyOnce() {
            // It sits in a per-frame path, and a foreign pass is foreign on every frame it runs, so
            // an unlatched report would bury the log rather than inform it.
            var loggerMock = mock(Logger.class);
            var warning = buildWarning(() -> false, () -> NO_HOSTS_FOUND, loggerMock);

            warning.warnOnceIfNoVanillaMapIsShowing();
            warning.warnOnceIfNoVanillaMapIsShowing();
            warning.warnOnceIfNoVanillaMapIsShowing();

            verify(loggerMock, times(1))
                .warn(any(), any(Throwable.class));
        }

        @Test
        void stopsAskingOnceTheMapIsShownAgain() {
            // The latch is on having spoken, not on the state that prompted it: a pass that is
            // foreign once and a map that opens afterwards must not re-arm the line, or every
            // switch back and forth would add another copy of it.
            var loggerMock = mock(Logger.class);
            var isMapShowing = new boolean[] {false};
            var warning = buildWarning(() -> isMapShowing[0], () -> NO_HOSTS_FOUND, loggerMock);

            warning.warnOnceIfNoVanillaMapIsShowing();

            isMapShowing[0] = true;
            warning.warnOnceIfNoVanillaMapIsShowing();

            isMapShowing[0] = false;
            warning.warnOnceIfNoVanillaMapIsShowing();

            verify(loggerMock, times(1))
                .warn(any(), any(Throwable.class));
        }

        @Test
        void carriesTheModOwnedClassesIntoTheLine() {
            // The point of the whole line. The stack cannot name an owner - the mod builds its
            // widget once and the engine renders it every frame after - so if the tree read does
            // not reach the message, the warning costs a walk and answers nothing.
            var loggerMock = mock(Logger.class);

            buildWarning(() -> false, () -> MOD_OWNED_HOST_FOUND, loggerMock)
                .warnOnceIfNoVanillaMapIsShowing();

            verify(loggerMock)
                .warn(contains("org.example.mod.NavTooltipPlugin"), any(Throwable.class));
        }

        @Test
        void saysSoWhenTheTreeCouldNotBeWalked() {
            // A null read must not read as "walked the tree and found nobody", which would send a
            // reader looking for a mod that the walk never had a chance to see.
            var loggerMock = mock(Logger.class);

            buildWarning(() -> false, () -> null, loggerMock)
                .warnOnceIfNoVanillaMapIsShowing();

            verify(loggerMock)
                .warn(
                    eq("Overlay drawn with no map on screen; the tree could not be walked"),
                    any(Throwable.class));
        }

        @Test
        void doesNotWalkTheTreeWhileAMapIsShowing() {
            // The walk is a full sweep of the core UI, and this runs on every frame the map paints.
            // Asking for it before knowing a line is owed would pay for it sixty times a second on
            // a working setup.
            var loggerMock = mock(Logger.class);
            var treeWalkOnShowingMap = buildWarning(
                () -> true,
                () -> {
                    throw new AssertionError("walked the tree with a map on screen");
                },
                loggerMock);

            assertThatCode(treeWalkOnShowingMap::warnOnceIfNoVanillaMapIsShowing)
                .doesNotThrowAnyException();
        }

        @Test
        void swallowsAPresenceReadThatThrows() {
            // The read reaches into the live widget tree and into core classes. This is a report
            // about the frame and not part of drawing one, so a read that cannot be made must cost
            // the report alone - letting it out would take the overlay away over a log line.
            var loggerMock = mock(Logger.class);
            var warningOnBrokenRead = buildWarning(
                () -> {
                    throw new IllegalStateException("no widget tree");
                },
                () -> NO_HOSTS_FOUND,
                loggerMock);

            assertThatCode(warningOnBrokenRead::warnOnceIfNoVanillaMapIsShowing)
                .doesNotThrowAnyException();
        }

        @Test
        void stopsRetryingAReadThatThrows() {
            // A read that broke once breaks every frame, so the failure spends the warning too. The
            // reported throwable is the failure itself, which is what says which of the two lines
            // this was.
            var loggerMock = mock(Logger.class);
            var readFailure = new IllegalStateException("no widget tree");
            var warningOnBrokenRead = buildWarning(
                () -> {
                    throw readFailure;
                },
                () -> NO_HOSTS_FOUND,
                loggerMock);

            warningOnBrokenRead.warnOnceIfNoVanillaMapIsShowing();
            warningOnBrokenRead.warnOnceIfNoVanillaMapIsShowing();

            verify(loggerMock, times(1))
                .warn(anyString(), any(Throwable.class));
            verify(loggerMock, never())
                .warn(any());
        }

        @Test
        void saysTheCallersWordsForAReadThatThrows() {

            var loggerMock = mock(Logger.class);
            var readFailure = new IllegalStateException("no widget tree");
            var warningOnBrokenRead = buildWarning(
                () -> {
                    throw readFailure;
                },
                () -> NO_HOSTS_FOUND,
                loggerMock);

            warningOnBrokenRead.warnOnceIfNoVanillaMapIsShowing();

            verify(loggerMock)
                .warn(UNREADABLE_PRESENCE, readFailure);
        }
    }

    private static ForeignMapPassWarning buildWarning(
            BooleanSupplier isAnyVanillaMapShowing,
            Supplier<String> describeEmbeddedMapHosts,
            Logger logger) {

        return new ForeignMapPassWarning(
            isAnyVanillaMapShowing,
            describeEmbeddedMapHosts,
            logger,
            NO_MAP_LEAD,
            UNREADABLE_PRESENCE);
    }
}
