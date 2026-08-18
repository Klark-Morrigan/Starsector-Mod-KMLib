package kmlib.starsector.ui.map.probes;

import kmlib.testfixtures.starsector.ui.coreui.CoreUiComponentFake;
import kmlib.testfixtures.starsector.ui.map.probes.SectorMapWidgetFake;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

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
 * <p>The walk that feeds it is pinned beside {@link EmbeddedMapFinder} instead, that being where it
 * lives. What is left here is the wording, which reads the live campaign UI through by-name hops
 * that cannot be stood up outside a running game.
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

    @Nested
    class DescribeMapHost {

        @Test
        void describeMapHostNamesTheTooltipTheMapSitsInAsItsHost() {
            // The host is what the search for an owner is bounded to, and a tooltip is the whole of
            // what a mod built - so picking the immediate parent instead would bound the search to
            // one panel inside somebody's widget and miss the class that names them.
            var mapFake = new SectorMapWidgetFake();
            var panelFake = new CoreUiComponentFake(mapFake);
            var tooltipHostFake = new TooltipHostFake(panelFake);

            var describedHost = EmbeddedMapHostTrace.describeMapHost(new EmbeddedMap(
                mapFake, List.of(new CoreUiComponentFake(tooltipHostFake), tooltipHostFake,
                    panelFake)));

            assertThat(describedHost)
                .contains("host=" + TooltipHostFake.class.getName());
        }

        @Test
        void describeMapHostNamesTheImmediateParentWhenNoTooltipIsAboveTheMap() {
            // The fallback, and it is a narrowing rather than a guess: it bounds the search to the
            // map's own siblings instead of the whole screen, which is the most that can be said
            // when nothing in the chain announces itself as a host.
            var mapFake = new SectorMapWidgetFake();
            var panelFake = new CoreUiComponentFake(mapFake);

            var describedHost = EmbeddedMapHostTrace.describeMapHost(new EmbeddedMap(
                mapFake, List.of(new CoreUiComponentFake(panelFake), panelFake)));

            assertThat(describedHost)
                .contains("host=" + CoreUiComponentFake.class.getName());
        }

        @Test
        void describeMapHostNamesTheMapItselfWhenItHangsUnderNothing() {
            // A map found at the root of the walk. There is no host to search around, and naming
            // the map is what keeps the line's shape the same rather than leaving a field empty.
            var mapFake = new SectorMapWidgetFake();

            assertThat(EmbeddedMapHostTrace.describeMapHost(new EmbeddedMap(mapFake, List.of())))
                .contains("host=" + SectorMapWidgetFake.class.getName());
        }

        @Test
        void describeMapHostNamesAModOwnedPluginCarriedInsideTheHost() {
            // The finding this line exists for. A custom panel is a vanilla component holding a
            // mod-supplied plugin, so a walk reading component classes alone crosses the mod's own
            // object and reports the engine's wrapper around it.
            var mapFake = new SectorMapWidgetFake();
            var tooltipHostFake = new TooltipHostFake(new PluginCarryingPanelFake(), mapFake);

            var describedHost = EmbeddedMapHostTrace.describeMapHost(new EmbeddedMap(
                mapFake, List.of(tooltipHostFake)));

            assertThat(describedHost)
                .contains(MapPanelPluginFake.class.getName());
        }

        @Test
        void describeMapHostNamesAModOwnedClassStandingAboveTheHost() {
            // Searched over the ancestry as well as below the host, because a mod that builds no
            // tooltip of its own leaves its own class further up the chain than any host bound.
            var mapFake = new SectorMapWidgetFake();

            var describedHost = EmbeddedMapHostTrace.describeMapHost(new EmbeddedMap(
                mapFake, List.of(new PluginCarryingPanelFake(), new CoreUiComponentFake(mapFake))));

            assertThat(describedHost)
                .contains(PluginCarryingPanelFake.class.getName());
        }
    }

    // A host whose class name announces it as a tooltip, which is how one is recognised - the class
    // is unpublished, so the marker is the name. Public because a by-name invoke resolves a public
    // method and then calls it, which the nested type's own visibility affects.
    public static final class TooltipHostFake {
        private final List<Object> children;

        public TooltipHostFake(Object... children) {
            this.children = List.of(children);
        }

        public List<Object> getChildrenCopy() {
            return children;
        }
    }

    // A component of the engine's kind holding a mod's own object, which is the shape a custom panel
    // has and the one case a walk over component classes alone cannot report.
    public static final class PluginCarryingPanelFake {

        public Object getPlugin() {
            return new MapPanelPluginFake();
        }
    }

    // Stands for whatever a mod hangs off its panel. Carries nothing: being reachable and having a
    // name of its own is the whole of what the line reports about it.
    public static final class MapPanelPluginFake {
    }
}
