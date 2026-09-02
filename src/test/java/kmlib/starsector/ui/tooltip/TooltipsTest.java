package kmlib.starsector.ui.tooltip;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pins {@link Tooltips#attach} against the {@code addTooltipTo} contract:
 *  - the parent's {@code addTooltipTo} fires exactly once with the
 *    supplied target and location,
 *  - the registered {@link TooltipMakerAPI.TooltipCreator} reports
 *    non-expandable and the configured width to the engine,
 *  - the body painter is called once per {@code createTooltip} call (so
 *    repeat hovers paint repeatedly without re-registering), and
 *  - null arguments fail fast at the call site rather than dying inside
 *    Starsector's UI loop.
 *
 * <p>The surface-making form is pinned against the same contract, plus
 * the one thing only it does: the element it builds for itself is the
 * one the engine is asked through, which a caller cannot see and so
 * could not otherwise notice going wrong.
 */
class TooltipsTest {

    private static final float WIDTH = 320f;

    @Nested
    class Attach {
        @Test
        void registersTheCreatorOnTheParentWithTheGivenTargetAndLocation() {
            var parentMock = mock(TooltipMakerAPI.class);
            var targetMock = mock(UIComponentAPI.class);

            Tooltips.attach(
                parentMock,
                targetMock,
                TooltipMakerAPI.TooltipLocation.RIGHT,
                WIDTH,
                tt -> {
                    /* unused for this assertion */ });

            verify(parentMock).addTooltipTo(
                any(TooltipMakerAPI.TooltipCreator.class),
                eq(targetMock),
                eq(TooltipMakerAPI.TooltipLocation.RIGHT));
        }

        @Test
        void registeredCreatorReportsNonExpandableAndTheConfiguredWidth() {
            var parentMock = mock(TooltipMakerAPI.class);
            var targetMock = mock(UIComponentAPI.class);

            Tooltips.attach(
                parentMock,
                targetMock,
                TooltipMakerAPI.TooltipLocation.BELOW,
                WIDTH,
                tt -> {
                    /* unused for this assertion */ });

            var creator = captureCreator(
                parentMock, targetMock, TooltipMakerAPI.TooltipLocation.BELOW);
            // Both engine-facing predicates pinned: the expandable flag stays
            // false (all current call sites are single-shot bodies) and the
            // width matches the caller's value so a future regression that
            // ignores the parameter breaks loudly here.
            assertThat(creator.isTooltipExpandable(null)).isFalse();
            assertThat(creator.getTooltipWidth(null)).isEqualTo(WIDTH);
        }

        @Test
        void createTooltipInvokesTheBodyOnceWithTheEngineSuppliedTooltip() {
            var parentMock = mock(TooltipMakerAPI.class);
            var targetMock = mock(UIComponentAPI.class);
            var engineTooltipMock = mock(TooltipMakerAPI.class);
            var invocations = new AtomicInteger();

            Tooltips.attach(
                parentMock,
                targetMock,
                TooltipMakerAPI.TooltipLocation.RIGHT,
                WIDTH,
                tt -> {
                    invocations.incrementAndGet();
                    // The body must receive the same tooltip the engine
                    // hands the creator - otherwise paint lands on the
                    // wrong surface.
                    assertThat(tt).isSameAs(engineTooltipMock);
                });

            var creator = captureCreator(
                parentMock, targetMock, TooltipMakerAPI.TooltipLocation.RIGHT);
            creator.createTooltip(engineTooltipMock, false, null);
            creator.createTooltip(engineTooltipMock, true, null);

            // Two hovers -> two paints. Validates that the helper does not
            // memoise the body's first call (the live-state pattern relies on
            // re-evaluation on every hover).
            assertThat(invocations.get()).isEqualTo(2);
        }

        @Test
        void rejectsNullsFastAtTheCallSite() {
            var parentMock = mock(TooltipMakerAPI.class);
            var targetMock = mock(UIComponentAPI.class);
            Consumer<TooltipMakerAPI> body = tt -> {
                /* unused */ };

            // Null-check coverage: failing inside the engine's UI loop later
            // is much harder to diagnose than failing at the registration
            // call.
            assertThatThrownBy(() -> Tooltips.attach(
                null, targetMock, TooltipMakerAPI.TooltipLocation.RIGHT, WIDTH, body))
                .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> Tooltips.attach(
                parentMock, null, TooltipMakerAPI.TooltipLocation.RIGHT, WIDTH, body))
                .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> Tooltips.attach(
                parentMock, targetMock, null, WIDTH, body))
                .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> Tooltips.attach(
                parentMock, targetMock, TooltipMakerAPI.TooltipLocation.RIGHT, WIDTH, null))
                .isInstanceOf(NullPointerException.class);
        }
    }

    /**
     * The same attachment for a caller with no surface of its own - the case a control appended to
     * a widget somebody else built is always in. What is pinned is that the surface it makes is
     * used for the call and nothing else: the element the engine is asked through is the one made
     * here, and the answers the engine reads off the creator are the caller's.
     */
    @Nested
    class AttachWithOwnSurface {

        @Test
        void attachWithOwnSurfaceRegistersTheCreatorOnAnElementItMakesForTheCall() {
            var elementMock = mock(TooltipMakerAPI.class);
            var targetMock = mock(UIComponentAPI.class);

            withSettingsCreating(elementMock, () -> Tooltips.attachWithOwnSurface(
                targetMock,
                TooltipMakerAPI.TooltipLocation.ABOVE,
                WIDTH,
                tt -> {
                    /* unused for this assertion */ }));

            // The caller never sees the element, so the only evidence it was made and used is the
            // call landing on it with the caller's own target and edge.
            verify(elementMock).addTooltipTo(
                any(TooltipMakerAPI.TooltipCreator.class),
                eq(targetMock),
                eq(TooltipMakerAPI.TooltipLocation.ABOVE));
        }

        @Test
        void attachWithOwnSurfaceRegistersACreatorAnsweringTheEngineAsTheParentFormDoes() {
            var elementMock = mock(TooltipMakerAPI.class);
            var targetMock = mock(UIComponentAPI.class);
            var engineTooltipMock = mock(TooltipMakerAPI.class);
            var invocations = new AtomicInteger();

            withSettingsCreating(elementMock, () -> Tooltips.attachWithOwnSurface(
                targetMock,
                TooltipMakerAPI.TooltipLocation.ABOVE,
                WIDTH,
                tt -> invocations.incrementAndGet()));

            var creator = captureCreator(
                elementMock, targetMock, TooltipMakerAPI.TooltipLocation.ABOVE);
            creator.createTooltip(engineTooltipMock, false, null);

            // The three answers the engine asks a creator for. Pinned again here rather than taken
            // on trust from the form above, because a surface made in-house is the one thing that
            // could quietly substitute a width or a flag of its own.
            assertThat(creator.isTooltipExpandable(null)).isFalse();
            assertThat(creator.getTooltipWidth(null)).isEqualTo(WIDTH);
            assertThat(invocations.get()).isEqualTo(1);
        }

        @Test
        void attachWithOwnSurfaceRejectsNullsFastAtTheCallSite() {
            var targetMock = mock(UIComponentAPI.class);
            Consumer<TooltipMakerAPI> body = tt -> {
                /* unused */ };

            // Checked before the surface is made, so a bad call site never reaches the engine.
            assertThatThrownBy(() -> Tooltips.attachWithOwnSurface(
                null, TooltipMakerAPI.TooltipLocation.ABOVE, WIDTH, body))
                .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> Tooltips.attachWithOwnSurface(
                targetMock, null, WIDTH, body))
                .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> Tooltips.attachWithOwnSurface(
                targetMock, TooltipMakerAPI.TooltipLocation.ABOVE, WIDTH, null))
                .isInstanceOf(NullPointerException.class);
        }
    }

    // Runs the subject against a settings stand-in whose panels hand back one known element, which
    // is what lets a test see the surface the subject makes for itself. Global.setSettings is the
    // engine's own seam, so no static mocking is needed.
    private static void withSettingsCreating(TooltipMakerAPI element, Runnable subject) {
        var panelMock = mock(CustomPanelAPI.class);
        when(panelMock.createUIElement(anyFloat(), anyFloat(), anyBoolean()))
            .thenReturn(element);

        var settingsMock = mock(SettingsAPI.class);
        when(settingsMock.createCustom(anyFloat(), anyFloat(), any()))
            .thenReturn(panelMock);

        Global.setSettings(settingsMock);
        try {
            subject.run();
        } finally {
            Global.setSettings(null);
        }
    }

    private static TooltipMakerAPI.TooltipCreator captureCreator(
            TooltipMakerAPI parent,
            UIComponentAPI target,
            TooltipMakerAPI.TooltipLocation location) {
        var captor =
            ArgumentCaptor.forClass(TooltipMakerAPI.TooltipCreator.class);
        verify(parent).addTooltipTo(
            captor.capture(),
            eq(target),
            eq(location));
        return captor.getValue();
    }
}
