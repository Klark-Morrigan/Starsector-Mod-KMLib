package kmlib.starsector.ui.map.probes;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the three judgements the probe makes about each component it reaches. The walk itself starts
 * at the live core UI and answers nothing outside a running game, so it goes uncovered the way this
 * package's other walks do; what a walk does with a component it has already reached needs only an
 * object answering the core UI's names, which is what the fakes here are.
 *
 * <p>Each of the three decides whether an overlay steps aside: a host misread as a leaf hides a
 * tooltip that is up, a tooltip idling on a widget mistaken for a shown one blanks the overlay over
 * every star, and a name match that missed the game's own subclass would report vanilla's tooltip as
 * some other mod's. None of the three is observable in the log until it is already wrong on screen.
 */
class VanillaMapTooltipProbeTest {

    @Nested
    class FindTooltipShownBy {

        @Test
        void findTooltipShownByAnswersWhatAHostIsShowing() {

            var tooltipFake = new Object();

            assertThat(VanillaMapTooltipProbe.findTooltipShownBy(new TooltipHostFake(tooltipFake)))
                .isSameAs(tooltipFake);
        }

        @Test
        void findTooltipShownByIsNullForAHostShowingNothing() {
            // How a host says its tooltip has hidden: it clears the field rather than dropping the
            // accessor, so null here is the ordinary resting state and not a failed read.
            assertThat(VanillaMapTooltipProbe.findTooltipShownBy(new TooltipHostFake(null)))
                .isNull();
        }

        @Test
        void findTooltipShownByIsNullForAComponentThatHostsNoTooltip() {
            // The common leaf. Most of the tree exposes no such accessor at all, and reading that as
            // a failure would abort the walk at its first ordinary component.
            assertThat(VanillaMapTooltipProbe.findTooltipShownBy(new Object()))
                .isNull();
        }

        @Test
        void findTooltipShownByStillReadsAHostWhoseAccessorThrewOnce() {
            // Nothing about a widget is remembered from a *failed call*, only from whether its shape
            // carries the name, and this is the case that distinguishes the two: a host that threw
            // from inside its own accessor is still a host. Were a failure enough to condemn it,
            // that widget's tooltips would be invisible to the probe for the rest of the run - and
            // the one bad frame that caused it is long gone by the time anyone notices.
            var hostFake = new ThrowingOnceTooltipHostFake();

            assertThat(VanillaMapTooltipProbe.findTooltipShownBy(hostFake))
                .isNull();
            assertThat(VanillaMapTooltipProbe.findTooltipShownBy(hostFake))
                .isSameAs(ThrowingOnceTooltipHostFake.TOOLTIP);
        }
    }

    @Nested
    class IsTooltipVisible {

        @Test
        void isTooltipVisibleIsTrueForATooltipFadedIn() {

            assertThat(VanillaMapTooltipProbe.isTooltipVisible(new TooltipFake(new FaderFake(false))))
                .isTrue();
        }

        @Test
        void isTooltipVisibleIsFalseForATooltipFadedOut() {
            // A widget can hold a tooltip it has never shown, its fader idle at nothing. Reading that
            // as shown would stand the overlay aside for a box nobody can see.
            assertThat(VanillaMapTooltipProbe.isTooltipVisible(new TooltipFake(new FaderFake(true))))
                .isFalse();
        }

        @Test
        void isTooltipVisibleIsFalseWhenThereIsNoFader() {

            assertThat(VanillaMapTooltipProbe.isTooltipVisible(new TooltipFake(null)))
                .isFalse();
        }

        @Test
        void isTooltipVisibleIsFalseWhenTheFaderCannotBeRead() {
            // Fail-open, and the direction matters: an unreadable fader leaves the overlay drawing,
            // where the opposite would hide it on every frame of a game build this cannot read.
            assertThat(VanillaMapTooltipProbe.isTooltipVisible(new Object()))
                .isFalse();
        }
    }

    @Nested
    class IsNamedInHierarchy {

        @Test
        void isNamedInHierarchyIsTrueForTheClassItself() {

            assertThat(VanillaMapTooltipProbe.isNamedInHierarchy(FaderFake.class, FaderFake.class.getName()))
                .isTrue();
        }

        @Test
        void isNamedInHierarchyIsTrueForASubclassOfTheNamedClass() {
            // The load-bearing case: what the map shows is an expandable subclass of the tooltip type,
            // so an exact-class test would never match the thing actually on screen.
            assertThat(VanillaMapTooltipProbe
                .isNamedInHierarchy(SubclassFake.class, BaseFake.class.getName()))
                .isTrue();
        }

        @Test
        void isNamedInHierarchyIsFalseForAnUnrelatedClass() {

            assertThat(VanillaMapTooltipProbe.isNamedInHierarchy(FaderFake.class, BaseFake.class.getName()))
                .isFalse();
        }

        @Test
        void isNamedInHierarchyIsFalseForTheNameEveryClassWouldMatch() {
            // Object terminates the walk instead of being compared, since every class reaches it and
            // matching there would name every component in the tree as the type being looked for.
            assertThat(VanillaMapTooltipProbe.isNamedInHierarchy(SubclassFake.class, "java.lang.Object"))
                .isFalse();
        }
    }

    // Stands for a core-UI component that holds a tooltip: it answers the accessor the probe reads by
    // name, holding null the way a real host does once its tooltip has hidden.
    //
    // Local rather than a shipped fixture because the tooltip contract is this probe's alone - the
    // fixtures KMLib publishes are the tree shapes every walk shares. Public because a by-name invoke
    // resolves a public method, which the enclosing class's visibility does not confer.
    public static final class TooltipHostFake {

        private final Object tooltip;

        public TooltipHostFake(Object tooltip) {
            this.tooltip = tooltip;
        }

        public Object getTooltip() {
            return tooltip;
        }
    }

    // A host whose accessor fails once and then answers, which is the shape that separates "this class
    // has no such method" from "this call went wrong" - the distinction the read turns on, and the only
    // way to observe from outside that it drew the line in the right place.
    public static final class ThrowingOnceTooltipHostFake {

        public static final Object TOOLTIP = new Object();

        private boolean hasThrown;

        public Object getTooltip() {
            if (hasThrown) {
                return TOOLTIP;
            }
            hasThrown = true;
            throw new IllegalStateException("A host that fails from inside its own accessor.");
        }
    }

    // Stands for a tooltip: it answers the fader accessor, holding null for the tooltip shapes that
    // expose the name but no fader.
    public static final class TooltipFake {

        private final Object fader;

        public TooltipFake(Object fader) {
            this.fader = fader;
        }

        public Object getFader() {
            return fader;
        }
    }

    // Stands for the fade state a tooltip is read through. Primitive boolean because that is what the
    // core UI declares, and what the probe's instanceof against the boxed value has to survive.
    public static final class FaderFake {

        private final boolean fadedOut;

        public FaderFake(boolean fadedOut) {
            this.fadedOut = fadedOut;
        }

        public boolean isFadedOut() {
            return fadedOut;
        }
    }

    // A two-class hierarchy standing in for the game's tooltip type and the subclass it actually shows.
    // Named locally because the real pair cannot be built here: the game's classes are obfuscated and
    // do not load in this JVM, and the rule under test is about names in a hierarchy rather than about
    // those particular two.
    public static class BaseFake {
    }

    public static final class SubclassFake extends BaseFake {
    }
}
