package kmlib.logging;

import kmlib.logging.KmLogging.LunaLogBinding;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the contract of {@link KmLogging}.
 *
 * Two seams are exercised directly:
 *  - {@link KmLogging#applyLevel} - the log4j core: a named level reaches the
 *    whole subtree through inheritance, whitespace is tolerated, null and
 *    unrecognised names fall back, and loggers outside the subtree are left
 *    alone.
 *  - {@link LunaLogBinding#settingsChanged} - the change filter: a notification
 *    for another mod's settings is ignored.
 *
 * The matching-id read path is not unit-tested here: it calls
 * {@code LunaSettings.getString}, which loads game-backed settings, so it
 * belongs to in-game/integration coverage rather than a static mock. Uses the
 * real log4j {@link Logger} hierarchy (no mock), since that inheritance is
 * exactly what the helper relies on; each test uses a distinct logger-root
 * name because log4j loggers are process-global.
 */
final class KmLoggingTest {
    @Test
    void named_level_is_inherited_by_descendant_loggers() {
        var descendant = Logger.getLogger("kmlibtest_named.child.grandchild");

        KmLogging.applyLevel("kmlibtest_named", "DEBUG", Level.INFO);

        assertThat(descendant.getEffectiveLevel()).isEqualTo(Level.DEBUG);
    }

    @Test
    void surrounding_whitespace_on_the_name_is_tolerated() {
        var descendant = Logger.getLogger("kmlibtest_pad.child");

        KmLogging.applyLevel("kmlibtest_pad", "  WARN  ", Level.INFO);

        assertThat(descendant.getEffectiveLevel()).isEqualTo(Level.WARN);
    }

    @Test
    void null_name_falls_back_to_the_default() {
        var descendant = Logger.getLogger("kmlibtest_null.child");

        KmLogging.applyLevel("kmlibtest_null", null, Level.ERROR);

        assertThat(descendant.getEffectiveLevel()).isEqualTo(Level.ERROR);
    }

    @Test
    void unrecognised_name_falls_back_to_the_default() {
        var descendant = Logger.getLogger("kmlibtest_bad.child");

        KmLogging.applyLevel("kmlibtest_bad", "nonsense", Level.ERROR);

        assertThat(descendant.getEffectiveLevel()).isEqualTo(Level.ERROR);
    }

    @Test
    void loggers_outside_the_subtree_are_not_affected() {
        var sibling = Logger.getLogger("kmlibtest_sibling_outside");
        var siblingBefore = sibling.getEffectiveLevel();

        KmLogging.applyLevel("kmlibtest_subtree", "OFF", Level.INFO);

        assertThat(sibling.getEffectiveLevel()).isEqualTo(siblingBefore);
    }

    @Test
    void library_default_level_is_warn() {
        // Pins the shared fallback used by the no-default bindToLunaSetting
        // overload, so mods do not restate a default of their own.
        assertThat(KmLogging.DEFAULT_LEVEL).isEqualTo(Level.WARN);
    }

    @Test
    void binding_ignores_changes_to_other_mods_settings() {
        // A change notification carrying a different mod id must not retune
        // this binding's logger subtree, so its explicit level stays unset.
        var root = Logger.getLogger("kmlibtest_filter_root");
        var binding =
                new LunaLogBinding("kmlibtest_filter", "kmlibtest_filter_root", "f", Level.DEBUG);

        binding.settingsChanged("some_other_mod");

        assertThat(root.getLevel()).isNull();
    }
}
