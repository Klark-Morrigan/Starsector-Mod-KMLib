package kmlib.starsector.ui.controls.specs;

import java.util.List;

/**
 * The one run of a strip that scrolls: its controls keep their natural height and slide within
 * whatever room is left once everything outside the section is pinned, so a long list stays reachable
 * inside a bounded box while the controls around it never move. A strip with no section is never
 * capped - it stands at its natural height.
 *
 * <p>A group rather than a flag on whichever control happens to be long, because what a body wants to
 * scroll is a run: a heading, the list under it and the row beside that travel together, and a flag
 * on one member could only ever scroll that member. At most one section per strip - two would each
 * need the leftover height the other is claiming, so the capped layout takes the first and a body
 * stating two has asked for a layout nothing can satisfy.
 *
 * <p>It draws no chrome of its own and is flattened to its children's laid-out controls exactly as
 * {@link SideBySideSpec} is, so the renderer and the input listener only ever see ordinary controls -
 * each marked as scrolled, which is what the clip and the viewport-limited hit-test read. Moving a
 * run into a section therefore changes where it may go and nothing about how it reads.
 *
 * @param controls the controls inside the scrolling run, top to bottom
 */
public record ScrollingSectionSpec(
    List<ControlSpec> controls) implements ControlSpec {

    /** Copies the run defensively, so a later edit to a caller's list cannot mutate the spec. */
    public ScrollingSectionSpec {
        controls = List.copyOf(controls);
    }

    @Override
    public List<String> labels() {
        return List.of();
    }
}
