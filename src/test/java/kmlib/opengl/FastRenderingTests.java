package kmlib.opengl;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the Fast Rendering facts that are checkable without it: that a stock classpath reports the
 * renderer as absent, that a relocated bridge is still recognised, and that the installed release is
 * read off genir's version class or reported as unknown.
 */
final class FastRenderingTests {

    @Nested
    class IsFastRenderingActive {

        @Test
        void reportsFalseWhenGlIsStockLwjgl() {
            // Nothing rewrote this test's class references, so GL11 is the real one. This pins the
            // stock half of the detection - the patched half needs a patched game to observe.
            assertThat(FastRendering.isFastRenderingActive()).isFalse();
        }
    }

    @Nested
    class IsBridgeClassName {

        @Test
        void reportsTrueWhenTheNameIsTheRelocatedBridge() {
            // The layout from v0.7.4 onwards. Fast Rendering relocated its bridge within its own
            // package without saying so, and a check keyed to one release's full class name
            // reported "stock" afterwards - which sent callers into GL reads it cannot serve.
            assertThat(FastRendering.isBridgeClassName("com.genir.renderer.bridge.commands.GL11"))
                .isTrue();
        }

        @Test
        void reportsTrueWhenTheNameIsTheFacadeBridge() {
            // The layout from v0.8.9 onwards, which is the second unannounced move. The commands
            // package still holds the implementations, but GL references now rewrite to a facade
            // beside it, so this is the name a current install actually reports.
            assertThat(FastRendering.isBridgeClassName("com.genir.renderer.bridge.opengl.GL11"))
                .isTrue();
        }

        @Test
        void reportsTrueWhenTheNameIsTheEarlierBridgeLayout() {
            // The layout up to v0.7.3, still in the field on installs that have not updated.
            assertThat(FastRendering.isBridgeClassName("com.genir.renderer.bridge.GL11")).isTrue();
        }

        @Test
        void reportsFalseWhenTheNameIsStockLwjgl() {
            assertThat(FastRendering.isBridgeClassName("org.lwjgl.opengl.GL11")).isFalse();
        }
    }

    @Nested
    class ReadInstalledVersion {

        @Test
        void reportsTheReleaseTheVersionClassAnswers() {

            assertThat(FastRendering.readInstalledVersion(className -> InstalledVersionClass.class))
                .isEqualTo("v0.9.1rc1");
        }

        @Test
        void asksForGenirsVersionClassByName() {

            var askedClassName = new AtomicReference<String>();

            FastRendering.readInstalledVersion(className -> {
                askedClassName.set(className);
                return InstalledVersionClass.class;
            });

            assertThat(askedClassName.get())
                .isEqualTo("com.genir.renderer.Version");
        }

        @Test
        void reportsNoVersionWhereNoVersionClassIsInstalled() {

            // A stock install, or a jar older than v0.8.2, which carries no version class at all.
            assertThat(FastRendering.readInstalledVersion(className -> {
                throw new ClassNotFoundException(className);
            }))
                .isNull();
        }

        @Test
        void reportsNoVersionWhereTheVersionMethodIsGone() {

            assertThat(FastRendering.readInstalledVersion(className -> ClassWithoutVersion.class))
                .isNull();
        }
    }

    // Stand-ins for genir's version class: public, with the static method the read looks up, and one
    // without it.
    public static final class InstalledVersionClass {

        public static String getVersion() {
            return "v0.9.1rc1";
        }
    }

    public static final class ClassWithoutVersion {
    }
}
