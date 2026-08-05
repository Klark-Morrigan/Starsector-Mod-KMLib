package kmlib.starsector.ui.map.probes;

/**
 * Shared bounds for the reads this package takes into the live widget tree: how deep a walk goes
 * before it gives up, and how many of the things it found one diagnostic line names.
 *
 * <p>Both are judgements about the same tree and the same log rather than about any one probe, so
 * they live once here instead of as a per-probe literal. Walks stopping at different depths would
 * disagree about what "not in the tree" means while looking at one tab, and lines capped
 * differently would truncate at different points in one log for no reason its reader could see.
 */
final class ProbeLimits {

    // The chrome, the tooltip host and the map widget all sit a few panels down inside a tab, so
    // the bound has to clear that; what it is really for is keeping a pathological tree - or one
    // whose parent and child answer as each other's children - from a runaway walk.
    static final int MAX_SEARCH_DEPTH = 12;

    // Cap on the items named in one diagnostic line, so a deeply nested hit, a busy tab or a
    // nebula-heavy sector logs a readable sample rather than a wall of text.
    static final int MAX_DESCRIBED_ITEMS = 24;

    private ProbeLimits() {
    }
}
