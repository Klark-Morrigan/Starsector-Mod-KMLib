package kmlib.mods.consolecommands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModManagerAPI;
import com.fs.starfarer.api.SettingsAPI;

import kmlib.testfixtures.mods.consolecommands.ConsoleOverlayPresenceFake;
import kmlib.testfixtures.starsector.StubbedGlobalLogger;
import kmlib.testfixtures.starsector.settings.ModStateScopes;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static kmlib.testfixtures.starsector.settings.StubbedModIds.CONSOLE_COMMANDS;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pins the console gate's fail-open contract, which is the whole reason it exists: an install
 * without Console Commands, a release that moved the accessor, and a read that throws must all
 * report "no console open" rather than propagate, because every caller of this stands down when
 * it answers true and none of them may be taken away by a broken read.
 *
 * <p>The mod-absent case additionally pins that the console is never asked at all, since reaching
 * the read is what resolves the class naming a Console Commands type - the thing an install
 * without the mod would take a missing-class error on.
 */
final class ConsoleCommandsOverlayTest {

    @Nested
    class IsOpen {

        @Test
        void reportsClosedWithoutAskingTheConsoleWhenTheModIsAbsent() {

            var presenceFake = new ConsoleOverlayPresenceFake();
            presenceFake.openConsole();

            ModStateScopes.runWithModEnabled(CONSOLE_COMMANDS, false, () -> {

                assertThat(new ConsoleCommandsOverlay(presenceFake).isOpen())
                    .isFalse();
                assertThat(presenceFake.readAskCount())
                    .isZero();
            });
        }

        @Test
        void reportsClosedThroughTheLiveReadWhenTheModIsAbsent() {

            // The live read rather than a stood-in one: an install without Console Commands has
            // to answer without ever resolving the class that names the mod's overlay panel,
            // which would otherwise be a missing-class error on a per-frame path. Nothing else
            // here exercises that constructor, so nothing else would notice it eagerly built.
            ModStateScopes.runWithModEnabled(CONSOLE_COMMANDS, false, () ->
                assertThat(new ConsoleCommandsOverlay().isOpen())
                    .isFalse());
        }

        @Test
        void reportsOpenWhileTheConsoleOverlayIsUp() {

            var presenceFake = new ConsoleOverlayPresenceFake();
            presenceFake.openConsole();

            ModStateScopes.runWithModEnabled(CONSOLE_COMMANDS, true, () ->
                assertThat(new ConsoleCommandsOverlay(presenceFake).isOpen())
                    .isTrue());
        }

        @Test
        void reportsClosedWhileTheConsoleOverlayIsDown() {

            var presenceFake = new ConsoleOverlayPresenceFake();

            ModStateScopes.runWithModEnabled(CONSOLE_COMMANDS, true, () ->
                assertThat(new ConsoleCommandsOverlay(presenceFake).isOpen())
                    .isFalse());
        }

        @Test
        void reportsClosedWhenTheConsoleOverlayCannotBeReached() {

            var presenceFake = new UnreachableConsolePresenceFake();

            ModStateScopes.runWithModEnabled(CONSOLE_COMMANDS, true, () ->
                assertThat(new ConsoleCommandsOverlay(presenceFake).isOpen())
                    .isFalse());
        }

        @Test
        void reportsClosedWhenTheConsoleReadThrows() {

            var presenceFake = new FailingConsolePresenceFake();

            ModStateScopes.runWithModEnabled(CONSOLE_COMMANDS, true, () ->
                assertThat(new ConsoleCommandsOverlay(presenceFake).isOpen())
                    .isFalse());
        }

        @Test
        void reportsClosedWhenTheModStateCannotBeRead() {

            var presenceFake = new ConsoleOverlayPresenceFake();
            presenceFake.openConsole();

            try (var globalMock = mockStatic(Global.class)) {

                stubLogger(globalMock);

                globalMock
                    .when(Global::getSettings)
                    .thenThrow(new IllegalStateException("settings not up"));

                assertThat(new ConsoleCommandsOverlay(presenceFake).isOpen())
                    .isFalse();
            }
        }

        @Test
        void stopsAskingTheConsoleOnceAReadHasFailed() {
            // The gate is asked every frame, so a failure that kept being retried would throw and
            // be swallowed sixty times a second for the rest of the session.
            var presenceFake = new UnreachableConsolePresenceFake();

            ModStateScopes.runWithModEnabled(CONSOLE_COMMANDS, true, () -> {

                var consoleOverlay = new ConsoleCommandsOverlay(presenceFake);
                consoleOverlay.isOpen();

                assertThat(consoleOverlay.isOpen())
                    .isFalse();
                assertThat(presenceFake.askCount)
                    .isEqualTo(1);
            });
        }

        @Test
        void readsTheModStateOnceAcrossFrames() {

            var presenceFake = new ConsoleOverlayPresenceFake();

            try (var globalMock = mockStatic(Global.class)) {

                var modManagerMock = stubGlobalWithTheModEnabled(globalMock);
                var consoleOverlay = new ConsoleCommandsOverlay(presenceFake);

                consoleOverlay.isOpen();
                consoleOverlay.isOpen();

                // The mod set cannot change within a run, so the enablement is settled once and
                // held rather than walked on every frame.
                verify(modManagerMock, times(1))
                    .isModEnabled(CONSOLE_COMMANDS);
            }
        }
    }

    // A mod manager the case can count the asks on, which is the one thing ModStateScopes' proxy
    // cannot answer - every other case here says "the mod is enabled" through the shipped scope.
    private static ModManagerAPI stubGlobalWithTheModEnabled(MockedStatic<Global> globalMock) {

        stubLogger(globalMock);

        var settingsMock = mock(SettingsAPI.class);
        var modManagerMock = mock(ModManagerAPI.class);

        globalMock
            .when(Global::getSettings)
            .thenReturn(settingsMock);

        when(settingsMock.getModManager())
            .thenReturn(modManagerMock);

        when(modManagerMock.isModEnabled(CONSOLE_COMMANDS))
            .thenReturn(true);

        return modManagerMock;
    }

    // The gate holds a static logger, resolved when its class is first loaded - which happens
    // inside one of these mocked scopes, so Global must be able to hand one back.
    private static void stubLogger(MockedStatic<Global> globalMock) {
        StubbedGlobalLogger.answerLoggersOn(globalMock);
    }

    // A console whose panel class or accessor is gone: what an install running a Console Commands
    // release that moved or renamed either would take on the read.
    private static final class UnreachableConsolePresenceFake implements ConsoleOverlayPresence {

        private int askCount;

        @Override
        public boolean isOverlayUp() {
            askCount++;
            throw new NoClassDefFoundError(
                "org/lazywizard/console/overlay/v2/panels/ConsoleOverlayPanel");
        }
    }

    // A console whose read fails as something other than a linkage error, which the gate must
    // swallow just the same rather than let escape into a render or input pass.
    private static final class FailingConsolePresenceFake implements ConsoleOverlayPresence {

        @Override
        public boolean isOverlayUp() {
            throw new IllegalStateException("overlay state unavailable");
        }
    }
}
