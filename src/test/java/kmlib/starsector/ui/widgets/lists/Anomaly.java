package kmlib.starsector.ui.widgets.lists;

/**
 * Test fixture: one row of a picker list, standing in for whatever a consuming mod ranks in its own
 * sidebar. It implements nothing this package declares, which is the point - the sort model is
 * exercised over a type it has no way to open, so a coupling back to a caller's item shape would
 * fail to compile here rather than pass unnoticed. Carries two numerics beside the name so
 * {@link AnomalySortMode} has distinct keys to rank and flip on.
 *
 * @param displayName the label a row draws, also standing in as the item's identity
 * @param severity    one numeric a fixture mode ranks on
 * @param radius      the other, so a mode switch visibly reorders rather than relabels
 */
record Anomaly(
    String displayName,
    int severity,
    int radius) {
}
