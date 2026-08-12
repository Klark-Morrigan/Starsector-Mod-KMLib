package kmlib.starsector.ui.input;

import kmlib.animation.TraverseDurations;

import java.util.HashMap;
import java.util.Map;

/**
 * The {@link HoverFade}s of a row of like elements, keyed so a consumer can ask any one of them how far onto
 * its hovered look it is. Advanced with whichever key the pointer is on (or none), it steps that key's fade
 * up and every other one down in the same pass - which is what makes a row read as one hover travelling
 * across it rather than as several fades a caller has to remember to wind back down.
 *
 * <p>A lone element holds a {@link HoverFade} directly; a row holds one of these. The difference is only
 * whether there is more than one element to tell apart, so nothing about what a fraction means lives here
 * either.
 *
 * <p>A key that has settled fully off its hovered look is dropped, since a missing fade and a rested one
 * answer the same fraction. That keeps the set bounded for a row whose keys churn, and costs nothing for one
 * whose keys are stable.
 *
 * @param <K> what tells one element of the row from another - an index for a row that keeps its order, and
 *            for a set rebuilt under the pointer the place an element stands in rather than an identity of
 *            its own (see {@link BodyCellSlot}): a fade belongs to what the pointer is over, so keying it to
 *            the thing occupying that place would dip the lit element dark whenever the set changed
 */
public final class HoverFades<K> {

    // What a key with no fade of its own reads as: an element nothing has hovered yet stands fully off its
    // hovered look, which is the same answer a rested fade gives - so a dropped fade changes no consumer's
    // reading.
    private static final float NOT_HOVERED = 0f;

    // One fade per element the pointer has touched since the last reset, keyed by the consumer's own identity
    // for a row element. Grown on hover and pruned once a fade settles back off, so the set holds only the
    // elements currently showing something.
    private final Map<K, HoverFade> fadesByKey = new HashMap<>();

    /**
     * Steps every fade by a frame's worth of time: the hovered key's toward its hovered look, every other
     * key's back off theirs. A key hovered for the first time gains a fade here, and a key whose fade has
     * settled fully off loses it, so the set tracks what is actually in motion.
     *
     * @param hoveredKey     the element the pointer is on this frame, or null when it is on none of them
     * @param elapsedSeconds real time since the last frame the consumer drew
     * @param durations      how long travelling onto a hovered look and back off it each take; a
     *                       non-positive one snaps that way
     */
    public void advanceTowardHoveredKey(
            K hoveredKey,
            float elapsedSeconds,
            TraverseDurations durations) {

        // Minted before the walk rather than beside it, so a newly hovered key is stepped by this same frame
        // and starts rising immediately instead of standing still for one frame at zero.
        if (hoveredKey != null) {
            fadesByKey.computeIfAbsent(hoveredKey, key -> new HoverFade());
        }

        var entries = fadesByKey.entrySet().iterator();
        while (entries.hasNext()) {

            var entry = entries.next();
            var isHovered = entry.getKey().equals(hoveredKey);

            entry.getValue().advanceTowardHover(isHovered, elapsedSeconds, durations);

            if (!isHovered && entry.getValue().hasSettledOffHover()) {
                entries.remove();
            }
        }
    }

    /**
     * Drops every fade, for a consumer whose elements stop showing - so a fade left part-way up cannot
     * reappear on whatever is drawn next, which the player never saw rise and would see fall for no reason.
     */
    public void resetFades() {
        fadesByKey.clear();
    }

    /**
     * @param key the element being asked about
     * @return how far onto its hovered look that element currently is, 0 for one with no fade running
     */
    public float resolveHoverFractionAt(K key) {
        var fade = fadesByKey.get(key);
        return fade == null
            ? NOT_HOVERED
            : fade.getHoverFraction();
    }
}
