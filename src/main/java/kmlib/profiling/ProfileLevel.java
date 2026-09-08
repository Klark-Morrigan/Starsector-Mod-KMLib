package kmlib.profiling;

/**
 * How much detail a capture keeps, and how much detail one section is worth
 * keeping at.
 *
 * <p>Two questions with one answer, because they are asked against each other:
 * a section states the level below which it is not worth timing, a profiler
 * states the finest level it keeps, and a section the bound profiler does not
 * reach opens silently - a comparison rather than a clock read, so a section on
 * a per-item path costs nothing at all while a reader is looking at whole
 * frames.
 *
 * <p>Ordered coarsest first, and the order is the contract: each level admits
 * itself and everything above it. {@link #OFF} admits nothing, which is what
 * makes it the state a reader who wants no capture asks for rather than a level
 * anything is registered at.
 */
public enum ProfileLevel {

    /** No capture at all - the state the library sits in until a mod asks otherwise. */
    OFF,

    /**
     * The beats and passes a frame is made of: sections opened a handful of
     * times per frame, which is what a reader looking for where a frame went
     * reads first.
     */
    COARSE,

    /**
     * What one item of a loop costs - the turns of a loop and the sections
     * inside one. Affordable per cell and not per vertex, so it is asked for
     * deliberately rather than left on.
     */
    FINE;

    /**
     * Whether a profiler keeping detail to this level records a section
     * registered at {@code sectionLevel}.
     *
     * @param sectionLevel the level the section stated it is worth timing at
     * @return whether the section's spans are kept
     */
    public boolean canAdmitLevel(ProfileLevel sectionLevel) {
        // OFF is checked apart from the ordering rather than falling out of it:
        // it means no capture, so it must not admit a section that happens to
        // sit at the same end of the scale.
        return this != OFF && compareTo(sectionLevel) >= 0;
    }
}
