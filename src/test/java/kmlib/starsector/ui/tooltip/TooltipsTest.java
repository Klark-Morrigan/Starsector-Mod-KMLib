package kmlib.starsector.ui.tooltip;

import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;

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

    @Test
    void registersTheCreatorOnTheParentWithTheGivenTargetAndLocation() {
        var parent = mock(TooltipMakerAPI.class);
        var target = mock(UIComponentAPI.class);

        Tooltips.attach(
                parent,
                target,
                TooltipMakerAPI.TooltipLocation.RIGHT,
                WIDTH,
                tt -> { /* unused for this assertion */ });

        verify(parent).addTooltipTo(
                any(TooltipMakerAPI.TooltipCreator.class),
                eq(target),
                eq(TooltipMakerAPI.TooltipLocation.RIGHT));
    }

    @Test
    void registeredCreatorReportsNonExpandableAndTheConfiguredWidth() {
        var parent = mock(TooltipMakerAPI.class);
        var target = mock(UIComponentAPI.class);

        Tooltips.attach(
                parent,
                target,
                TooltipMakerAPI.TooltipLocation.BELOW,
                WIDTH,
                tt -> { /* unused for this assertion */ });

        var creator = captureCreator(
                parent, target, TooltipMakerAPI.TooltipLocation.BELOW);
        // Both engine-facing predicates pinned: the expandable flag stays
        // false (all current call sites are single-shot bodies) and the
        // width matches the caller's value so a future regression that
        // ignores the parameter breaks loudly here.
        assertThat(creator.isTooltipExpandable(null)).isFalse();
        assertThat(creator.getTooltipWidth(null)).isEqualTo(WIDTH);
    }

    @Test
    void createTooltipInvokesTheBodyOnceWithTheEngineSuppliedTooltip() {
        var parent = mock(TooltipMakerAPI.class);
        var target = mock(UIComponentAPI.class);
        var engineTooltip = mock(TooltipMakerAPI.class);
        var invocations = new AtomicInteger();

        Tooltips.attach(
                parent,
                target,
                TooltipMakerAPI.TooltipLocation.RIGHT,
                WIDTH,
                tt -> {
                    invocations.incrementAndGet();
                    // The body must receive the same tooltip the engine
                    // hands the creator - otherwise paint lands on the
                    // wrong surface.
                    assertThat(tt).isSameAs(engineTooltip);
                });

        var creator = captureCreator(
                parent, target, TooltipMakerAPI.TooltipLocation.RIGHT);
        creator.createTooltip(engineTooltip, false, null);
        creator.createTooltip(engineTooltip, true, null);

        // Two hovers -> two paints. Validates that the helper does not
        // memoise the body's first call (the live-state pattern relies on
        // re-evaluation on every hover).
        assertThat(invocations.get()).isEqualTo(2);
    }

    @Test
    void rejectsNullsFastAtTheCallSite() {
        var parent = mock(TooltipMakerAPI.class);
        var target = mock(UIComponentAPI.class);
        Consumer<TooltipMakerAPI> body = tt -> { /* unused */ };

        // Null-check coverage: failing inside the engine's UI loop later
        // is much harder to diagnose than failing at the registration
        // call.
        assertThatThrownBy(() -> Tooltips.attach(
                null, target, TooltipMakerAPI.TooltipLocation.RIGHT, WIDTH, body))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Tooltips.attach(
                parent, null, TooltipMakerAPI.TooltipLocation.RIGHT, WIDTH, body))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Tooltips.attach(
                parent, target, null, WIDTH, body))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Tooltips.attach(
                parent, target, TooltipMakerAPI.TooltipLocation.RIGHT, WIDTH, null))
                .isInstanceOf(NullPointerException.class);
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
