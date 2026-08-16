package kmlib.starsector.ui.map.probes;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the one judgement this probe makes that is not a live tree read: which class names can
 * answer who owns a widget.
 *
 * <p>It carries the whole value of the line. Everything a map can be embedded in is built out of
 * engine classes, so a filter that let those through would bury the single mod-owned name that
 * identifies an owner in a list of wrappers - and a filter that dropped too much would report an
 * empty set for a host that was found, which reads as "nobody owns this".
 *
 * <p>The walk around it is not pinned here: it reads the live campaign UI through by-name hops that
 * cannot be stood up outside a running game, and what it does with what it finds is only meaningful
 * against a real tree.
 */
final class EmbeddedMapHostTraceTest {

    @Nested
    class IsModOwnedClass {

        @Test
        void isModOwnedClassAcceptsAModsOwnClass() {
            // The finding itself: one name like this in the report ends the search.
            assertThat(EmbeddedMapHostTrace.isModOwnedClass("org.example.mod.ui.MapTooltip"))
                .isTrue();
        }

        @Test
        void isModOwnedClassRejectsTheEnginesObfuscatedWidgets() {
            // The tree is almost entirely these, which is exactly why they cannot be the answer.
            assertThat(EmbeddedMapHostTrace.isModOwnedClass("com.fs.starfarer.ui.OOOo"))
                .isFalse();
            assertThat(EmbeddedMapHostTrace.isModOwnedClass("com.fs.starfarer.coreui.A$G"))
                .isFalse();
        }

        @Test
        void isModOwnedClassRejectsTheEnginesPublishedApi() {
            assertThat(EmbeddedMapHostTrace.isModOwnedClass(
                "com.fs.starfarer.api.impl.campaign.terrain.BaseTerrain"))
                .isFalse();
        }

        @Test
        void isModOwnedClassRejectsThePlatformsOwnClasses() {
            // Reached through the same walk - a children list holds whatever the tree holds - and
            // no more capable of naming an owner than the engine's are.
            assertThat(EmbeddedMapHostTrace.isModOwnedClass("java.util.ArrayList"))
                .isFalse();
            assertThat(EmbeddedMapHostTrace.isModOwnedClass("javax.swing.JPanel"))
                .isFalse();
        }

        @Test
        void isModOwnedClassRejectsANameItCouldNotRead() {
            // A component that answers no class name says nothing about ownership, and must not be
            // reported as though it did.
            assertThat(EmbeddedMapHostTrace.isModOwnedClass(null))
                .isFalse();
        }

        @Test
        void isModOwnedClassAcceptsANameMerelyContainingAnEnginePackage() {
            // Prefix rather than substring, deliberately: a mod is free to name a package after the
            // thing it extends, and dropping it would hide the owner most likely to be embedding a
            // map in the first place.
            assertThat(EmbeddedMapHostTrace.isModOwnedClass("org.example.com.fs.starfarer.Panel"))
                .isTrue();
        }
    }
}
