package kmlib.starsector.ui.tooltip;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Hanging a vanilla hover tooltip on a component.
 *
 * <p>The engine takes only a fully realised
 * {@link TooltipMakerAPI.TooltipCreator} - four overrides, three of which
 * are the same answers every time: never expandable, one fixed width, and
 * the body delegated onwards. Those three are filled in here so a caller
 * writes the fourth alone, as a lambda over the tooltip it is handed.
 *
 * <p>Two forms, because the engine offers the attachment only on a
 * tooltip surface. A caller building its own panel already holds one and
 * passes it in. A caller decorating a widget somebody else built holds
 * none, and nothing about the target yields one, so
 * {@link #attachWithOwnSurface} makes a surface for the call and drops it.
 *
 * <p>Non-expandable and fixed-width is what a hover tooltip wants; the
 * expandable variant is rare enough that a caller needing one implements
 * {@link TooltipMakerAPI.TooltipCreator} directly rather than being served
 * by a second pair of entry points here.
 */
public final class Tooltips {

    private Tooltips() {
        // Utility class - never instantiated.
    }

    /**
     * Attaches a fixed-width, non-expandable hover tooltip on {@code target},
     * rendering {@code body} into the tooltip's
     * {@link TooltipMakerAPI} every time the player opens it.
     *
     * <p>The body lambda is invoked on every hover - capturing live state
     * inside the lambda lets the tooltip reflect updates without
     * re-registering. Reusing the same {@link Consumer} across cells is
     * safe; the creator wraps it without keeping per-invocation state.
     *
     * @param parent   the tooltip surface the target lives on
     * @param target   the UI component the hover triggers on
     * @param location which edge of {@code target} the tooltip sits against
     * @param width    fixed tooltip width in pixels
     * @param body     painter invoked on every hover with the tooltip
     *                 element to fill
     */
    public static void attach(
            TooltipMakerAPI parent,
            UIComponentAPI target,
            TooltipMakerAPI.TooltipLocation location,
            float width,
            Consumer<TooltipMakerAPI> body) {

        Objects.requireNonNull(parent, "parent");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(body, "body");

        parent.addTooltipTo(
            new TooltipMakerAPI.TooltipCreator() {
                @Override
                public boolean isTooltipExpandable(Object tooltipParam) {
                    return false;
                }

                @Override
                public float getTooltipWidth(Object tooltipParam) {
                    return width;
                }

                @Override
                public void createTooltip(
                        TooltipMakerAPI tooltip,
                        boolean expanded,
                        Object tooltipParam) {
                    body.accept(tooltip);
                }
            },
            target,
            location);
    }

    /**
     * Attaches the same fixed-width, non-expandable hover tooltip on {@code target} for a caller
     * that has no tooltip surface of its own to hang it from.
     *
     * <p>The engine only offers {@code addTooltipTo} on a {@link TooltipMakerAPI}, which every
     * caller building its own panel already holds. A caller decorating a widget somebody else built
     * holds none, and nothing about the target yields one - so one is made here: a custom panel and
     * a single element off it, used for the call and then dropped.
     *
     * <p>Neither is added to anything and neither draws. What the call leaves behind is on the
     * target component alone - the element is the vehicle for making it, not a surface with a life
     * of its own - so letting both go the moment it returns is the whole of the cleanup rather than
     * a leak.
     *
     * @param target   the UI component the hover triggers on
     * @param location which edge of {@code target} the tooltip sits against
     * @param width    fixed tooltip width in pixels
     * @param body     painter invoked on every hover with the tooltip element to fill
     */
    public static void attachWithOwnSurface(
            UIComponentAPI target,
            TooltipMakerAPI.TooltipLocation location,
            float width,
            Consumer<TooltipMakerAPI> body) {

        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(body, "body");

        // Sized at the tooltip's own width rather than at nothing, so a panel that did somehow get
        // drawn would be the shape of the thing it carries. No plugin: a panel with no lifetime has
        // no per-frame work to hand one.
        var surface = Global.getSettings().createCustom(width, width, null);

        attach(surface.createUIElement(width, width, false), target, location, width, body);
    }
}
