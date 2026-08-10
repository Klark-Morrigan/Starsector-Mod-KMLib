package kmlib.starsector.ui.widgets.tabs.style;

import kmlib.math.ranges.Ranges;

import java.awt.Color;

/**
 * Light laid over a finished tab - added to whatever is already on the screen there rather than mixed
 * into it. The third flavour of tab paint, and the one that cannot be a colour a tab settles on: a
 * {@link TabLook} is a surface, a {@link TabWash} moves that surface toward a target, and this is a pass
 * over the top of both.
 *
 * <p>It exists because a chrome can stand its tabs on something it does not know. Where a tab's surface is
 * transparent - a button whose interior is unpainted, showing whatever the panel floats over - a shade
 * mixed into that surface arrives diluted by however much of it is actually painted, so the same light
 * reads at full strength on a solid tab and at a fraction of it on an unpainted one. Added, it lands the
 * same on both, which is what the engine's own buttons do: their pointed-at shade stands a fixed step above
 * their resting one whatever the map behind them is showing.
 *
 * <p>Whatever it is drawn over is what it brightens - a tab's surface and the text on it alike - so a
 * chrome lays it down after everything else it paints. That is also why it carries no separate rule for
 * the label: one pass over the finished button is the whole of it.
 *
 * <p>Substrate-independent, like the rest of this package: a colour and a unit weight, with the additive
 * drawing left to whichever surface paints it.
 *
 * @param colour the light's own colour, added to what is already there
 * @param weight how much of it to add, 0 adding nothing and 1 adding it in full; clamped into that range,
 *               since it is composed from live interaction and an out-of-range value would drive the pass
 *               past the light the palette named
 */
public record TabLight(
    Color colour,
    float weight) {

    /** No light at all - what a chrome whose pointer rule is a shade rather than a glow lays down. */
    public static final TabLight NONE = new TabLight(Color.BLACK, 0f);

    public TabLight {
        weight = Ranges.clampToUnit(weight);
    }

    /**
     * @return whether this light would change anything, so a chrome can skip a pass that would add nothing
     */
    public boolean isLit() {
        return weight > 0f;
    }
}
