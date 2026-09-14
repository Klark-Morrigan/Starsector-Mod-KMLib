package kmlib.starsector.ui.debug;

/**
 * One pushed debug output: a short key that reads as a subtitle over its value, and the value
 * itself. Kept as a pair so the layout can draw the key small above the body rather than a caller
 * having to format the two into one line.
 *
 * @param key  the label, drawn small above the body
 * @param body the value, drawn in the larger body face beneath the key
 */
public record DebugHudEntry(
    String key,
    String body) {
}
