package kmlib.starsector.ui.tooltip;

import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

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

    private static <T> T any(Class<T> clazz) {
        return org.mockito.ArgumentMatchers.any(clazz);
    }
}
