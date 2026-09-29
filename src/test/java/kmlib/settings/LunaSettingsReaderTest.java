package kmlib.settings;

import kmlib.testfixtures.logging.LogAppenderFake;
import kmlib.testfixtures.starsector.settings.ModStateScopes;

import lunalib.lunaSettings.LunaSettings;
import lunalib.lunaSettings.LunaSettingsListener;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

/**
 * Pins the fallback the reader's own null check cannot give: a read taken where LunaLib cannot
 * answer at all hands back the caller's value rather than dying inside LunaLib's loader.
 *
 * <p>Three states read as "cannot answer" and each is posed once - no game settings, settings
 * carrying no mod manager, and a mod set without LunaLib in it. They are posed against one getter
 * rather than all four, the guard being one reading shared by them; what each of the other three
 * pins is that it asks that reading at all, which is the way any of them regresses.
 *
 * <p>Where LunaLib can answer, its store is stood in by a static mock, since a running game is what
 * stands the real one up: a read hands through what the store holds, or the caller's value where it
 * holds nothing, and a change subscription is the listener LunaLib is handed - which of its notices
 * reach the callback, and what becomes of a callback that throws.
 */
class LunaSettingsReaderTest {

    // IDs of no consequence: a case either is refused before it looks at them or stands in the store
    // that answers for them.
    private static final String MOD_ID = "some_mod";
    private static final String FIELD_ID = "some_field";

    private static final String LUNALIB_MOD_ID = "lunalib";

    @Nested
    class GetDouble {

        @Test
        void answersTheFallbackOutsideARunningGame() {
            // The state that made this necessary: a value read on a path a test drives - a per-frame
            // budget, a cadence - reaching LunaLib's loader before the game has stood its settings
            // up, where the loader reads the mod set and dies.
            ModStateScopes.runWithoutGameSettings(() ->
                assertThat(LunaSettingsReader.getDouble(MOD_ID, FIELD_ID, 4.5))
                    .isEqualTo(4.5));
        }

        @Test
        void answersTheFallbackWithNoModManagerStoodUp() {
            // The half-built state between a game that is up and one that is not, which is a
            // separate hop and so a separate way to throw.
            ModStateScopes.runWithoutModManager(() ->
                assertThat(LunaSettingsReader.getDouble(MOD_ID, FIELD_ID, 4.5))
                    .isEqualTo(4.5));
        }

        @Test
        void answersTheFallbackOnAnInstallWithoutLunaLib() {
            // LunaLib is a declared KMLib dependency, so this is not a state a shipped install
            // reaches - it is what the reading actually asks, and pinning it is what keeps the guard
            // from being quietly narrowed to one of the two states above.
            ModStateScopes.runWithModEnabled(LUNALIB_MOD_ID, false, () ->
                assertThat(LunaSettingsReader.getDouble(MOD_ID, FIELD_ID, 4.5))
                    .isEqualTo(4.5));
        }
    }

    @Nested
    class GetBoolean {

        @Test
        void answersTheFallbackOutsideARunningGame() {
            // Posed against true, so a getter that had lost its guard and answered a primitive
            // default would fail rather than pass by coincidence.
            ModStateScopes.runWithoutGameSettings(() ->
                assertThat(LunaSettingsReader.getBoolean(MOD_ID, FIELD_ID, true))
                    .isTrue());
        }
    }

    @Nested
    class GetInt {

        @Test
        void answersTheFallbackOutsideARunningGame() {

            ModStateScopes.runWithoutGameSettings(() ->
                assertThat(LunaSettingsReader.getInt(MOD_ID, FIELD_ID, 7))
                    .isEqualTo(7));
        }
    }

    @Nested
    class GetString {

        @Test
        void answersTheFallbackOutsideARunningGame() {

            ModStateScopes.runWithoutGameSettings(() ->
                assertThat(LunaSettingsReader.getString(MOD_ID, FIELD_ID, "unset"))
                    .isEqualTo("unset"));
        }

        @Test
        void answersTheStoredValueWhereLunaLibAnswers() {

            ModStateScopes.runWithModEnabled(LUNALIB_MOD_ID, true, () -> {
                try (var lunaSettingsMock = mockStatic(LunaSettings.class)) {
                    lunaSettingsMock
                        .when(() -> LunaSettings.getString(MOD_ID, FIELD_ID))
                        .thenReturn("stored");

                    assertThat(LunaSettingsReader.getString(MOD_ID, FIELD_ID, "unset"))
                        .isEqualTo("stored");
                }
            });
        }

        @Test
        void answersTheFallbackWhereLunaLibHoldsNoValue() {
            // LunaLib's null for a field it has no value for, the ordinary case of the two.
            ModStateScopes.runWithModEnabled(LUNALIB_MOD_ID, true, () -> {
                try (var lunaSettingsMock = mockStatic(LunaSettings.class)) {
                    assertThat(LunaSettingsReader.getString(MOD_ID, FIELD_ID, "unset"))
                        .isEqualTo("unset");
                }
            });
        }
    }

    @Nested
    class RunOnSettingsChange {

        @Test
        void runsTheCallbackOnAChangeToItsModsSettings() {

            var applyCount = new AtomicInteger();

            registerListenerFor(applyCount::incrementAndGet)
                .settingsChanged(MOD_ID);

            assertThat(applyCount)
                .hasValue(1);
        }

        @Test
        void ignoresChangesToOtherModsSettings() {
            // LunaLib tells every listener about every mod's change.
            var applyCount = new AtomicInteger();

            registerListenerFor(applyCount::incrementAndGet)
                .settingsChanged("some_other_mod");

            assertThat(applyCount)
                .hasValue(0);
        }

        @Test
        void logsACallbackThatThrowsAtErrorWithItsTrace() {
            // LunaLib's own catch says a failed listener only at debug and drops the trace, which is
            // below what a player's log keeps.
            var failure = new IllegalStateException("switched feature half torn down");
            var listener = registerListenerFor(() -> {
                throw failure;
            });

            var capture = LogAppenderFake.captureLogOf(
                LunaSettingsReader.class,
                () -> listener.settingsChanged(MOD_ID));

            assertThat(capture.getMessages())
                .containsExactly("Applying the changed settings of mod 'some_mod' failed.");
            assertThat(capture.getThrowables())
                .containsExactly(failure);
        }

        @Test
        void containsACallbackThatFailsToLink() {
            // A callback reaching a class that no longer links throws an error rather than an
            // exception, and is as much the change failing to apply.
            var failure = new NoClassDefFoundError("a class the callback names");
            var listener = registerListenerFor(() -> {
                throw failure;
            });

            var capture = LogAppenderFake.captureLogOf(
                LunaSettingsReader.class,
                () -> listener.settingsChanged(MOD_ID));

            assertThat(capture.getThrowables())
                .containsExactly(failure);
        }

        @Test
        void runsTheCallbackAgainOnTheNextChangeAfterOneFailed() {
            // Not latched: what failed may have been that change's own state.
            var applyCount = new AtomicInteger();
            var listener = registerListenerFor(() -> {
                if (applyCount.incrementAndGet() == 1) {
                    throw new IllegalStateException("first change fails");
                }
            });

            LogAppenderFake.captureLogOf(
                LunaSettingsReader.class,
                () -> listener.settingsChanged(MOD_ID));
            listener.settingsChanged(MOD_ID);

            assertThat(applyCount)
                .hasValue(2);
        }
    }

    // Subscribes the callback the way a caller does and answers the listener LunaLib was handed,
    // which is what LunaLib calls on a change.
    private static LunaSettingsListener registerListenerFor(Runnable onChange) {

        try (var lunaSettingsMock = mockStatic(LunaSettings.class)) {
            LunaSettingsReader.runOnSettingsChange(MOD_ID, onChange);

            var listenerCaptor = ArgumentCaptor.forClass(LunaSettingsListener.class);

            lunaSettingsMock.verify(() -> LunaSettings.addSettingsListener(listenerCaptor.capture()));
            return listenerCaptor.getValue();
        }
    }
}
