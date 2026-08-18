package kmlib.starsector.ui.map.probes;

/**
 * Shared bounds for the reads this package takes into the live widget tree: how deep a walk goes
 * before it gives up, and how many of the things it meets are carried forward.
 *
 * <p>Both are judgements about the same tree rather than about any one probe, so they live once here
 * instead of as a per-probe literal. Walks stopping at different depths would disagree about what
 * "not in the tree" means while looking at one tab, and a walk that collected more than a line can
 * name would gather findings nobody ever sees - so one number governs both ends, and a walk stops
 * where the report it feeds would have truncated anyway.
 */
final class ProbeLimits {

    // The chrome, the tooltip host and the map widget all sit a few panels down inside a tab, so
    // the bound has to clear that; what it is really for is keeping a pathological tree - or one
    // whose parent and child answer as each other's children - from a runaway walk.
    static final int MAX_SEARCH_DEPTH = 12;

    // Cap on the items one walk collects and one diagnostic line names, so a deeply nested hit, a
    // busy tab or a nebula-heavy sector yields a readable sample rather than a wall of text - and so
    // a walk feeding such a line stops gathering where that line would have stopped naming.
    static final int MAX_REPORTED_ITEMS = 24;

    private ProbeLimits() {
    }
}
