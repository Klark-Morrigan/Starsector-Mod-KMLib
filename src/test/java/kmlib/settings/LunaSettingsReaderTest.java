package kmlib.settings;

import kmlib.settings.LunaSettingsReader.ChangeRelay;
import kmlib.testfixtures.logging.LogAppenderFake;
import kmlib.testfixtures.starsector.settings.ModStateScopes;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the fallback the reader's own null check cannot give: a read taken where LunaLib cannot
 * answer at all hands back the caller's value rather than dying inside LunaLib's loader.
 *
 * <p>Three states read as "cannot answer" and each is posed once - no game settings, settings
 * carrying no mod manager, and a mod set without LunaLib in it. They are posed against one getter
 * rather than all four, the guard being one reading shared by them; what each of the other three
 * pins is that it asks that reading at all, which is the way any of them regresses.
 *
 * <p>What the reader does once LunaLib can answer is not pinned here and cannot be: the value comes
 * out of LunaLib's own store, which is stood up by a running game rather than by a fixture.
 *
 * <p>The change relay is posed directly, without LunaLib's listener list: which of its notices reach
 * the callback, and what becomes of a callback that throws.
 */
class LunaSettingsReaderTest {

    // IDs of no consequence: every case here is refused before either is looked at.
    private static final String MOD_ID = "some_mod";
    private static final String FIELD_ID = "some_field";

    private static final String LUNALIB_MOD_ID = "lunalib";

    @Nested
    class GetDouble {

        @Test
        void getDoubleAnswersTheFallbackOutsideARunningGame() {
            // The state that made this necessary: a value read on a path a test drives - a per-frame
            // budget, a cadence - reaching LunaLib's loader before the game has stood its settings
            // up, where the loader reads the mod set and dies.
            ModStateScopes.runWithoutGameSettings(() ->
                assertThat(LunaSettingsReader.getDouble(MOD_ID, FIELD_ID, 4.5))
                    .isEqualTo(4.5));
        }

        @Test
        void getDoubleAnswersTheFallbackWithNoModManagerStoodUp() {
            // The half-built state between a game that is up and one that is not, which is a
            // separate hop and so a separate way to throw.
            ModStateScopes.runWithoutModManager(() ->
                assertThat(LunaSettingsReader.getDouble(MOD_ID, FIELD_ID, 4.5))
                    .isEqualTo(4.5));
        }

        @Test
        void getDoubleAnswersTheFallbackOnAnInstallWithoutLunaLib() {
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
        void getBooleanAnswersTheFallbackOutsideARunningGame() {
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
        void getIntAnswersTheFallbackOutsideARunningGame() {

            ModStateScopes.runWithoutGameSettings(() ->
                assertThat(LunaSettingsReader.getInt(MOD_ID, FIELD_ID, 7))
                    .isEqualTo(7));
        }
    }

    @Nested
    class GetString {

        @Test
        void getStringAnswersTheFallbackOutsideARunningGame() {

            ModStateScopes.runWithoutGameSettings(() ->
                assertThat(LunaSettingsReader.getString(MOD_ID, FIELD_ID, "unset"))
                    .isEqualTo("unset"));
        }
    }

    @Nested
    class SettingsChanged {

        @Test
        void logsAChangeThatFailedToApplyAtErrorWithItsTrace() {
            // LunaLib's own catch says a failed listener only at debug and drops the trace, which is
            // below what a player's log keeps.
            var failure = new IllegalStateException("switched feature half torn down");
            var relay = new ChangeRelay(MOD_ID, () -> {
                throw failure;
            });

            var capture = LogAppenderFake.captureLogOf(
                LunaSettingsReader.class,
                () -> relay.settingsChanged(MOD_ID));

            assertThat(capture.getMessages())
                .containsExactly("Applying the changed settings of mod 'some_mod' failed.");
            assertThat(capture.getEvents().get(0).getThrowableInformation().getThrowable())
                .isSameAs(failure);
        }

        @Test
        void containsACallbackThatFailsToLink() {
            // A callback reaching a class that no longer links throws an error rather than an
            // exception, and is as much the change failing to apply.
            var relay = new ChangeRelay(MOD_ID, () -> {
                throw new NoClassDefFoundError("a class the callback names");
            });

            var capture = LogAppenderFake.captureLogOf(
                LunaSettingsReader.class,
                () -> relay.settingsChanged(MOD_ID));

            assertThat(capture.getMessages())
                .hasSize(1);
        }

        @Test
        void appliesTheNextChangeAfterOneFailed() {
            // Not latched: what failed may have been that change's own state.
            var applyCount = new AtomicInteger();
            var relay = new ChangeRelay(MOD_ID, () -> {
                if (applyCount.incrementAndGet() == 1) {
                    throw new IllegalStateException("first change fails");
                }
            });

            LogAppenderFake.captureLogOf(
                LunaSettingsReader.class,
                () -> relay.settingsChanged(MOD_ID));
            relay.settingsChanged(MOD_ID);

            assertThat(applyCount)
                .hasValue(2);
        }

        @Test
        void ignoresChangesToOtherModsSettings() {

            var applyCount = new AtomicInteger();
            var relay = new ChangeRelay(MOD_ID, applyCount::incrementAndGet);

            relay.settingsChanged("some_other_mod");

            assertThat(applyCount)
                .hasValue(0);
        }
    }
}
